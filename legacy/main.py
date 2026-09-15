import requests
import argparse
import pyfiglet
from bs4 import BeautifulSoup
from concurrent.futures import ThreadPoolExecutor, as_completed
from urllib.parse import urlparse, parse_qs
import json
import sys
import re
import yt_dlp
from tqdm import tqdm
import logging
import os
import shutil


# Configure logging
class ColoredFormatter(logging.Formatter):
    COLORS = {
        "WARNING": "\033[93m",  # Yellow
        "ERROR": "\033[91m",  # Red
        "INFO": "\033[94m",  # Blue
        "DEBUG": "\033[92m",  # Green
        "RESET": "\033[0m",  # Reset
    }

    def format(self, record):
        log_color = self.COLORS.get(record.levelname, self.COLORS["RESET"])
        reset_color = self.COLORS["RESET"]
        record.msg = f"{log_color}{record.msg}{reset_color}"
        return super().format(record)


# Configure logging
handler = logging.StreamHandler()
handler.setFormatter(ColoredFormatter("%(message)s"))
logging.basicConfig(level=logging.INFO, handlers=[handler])


def check_ffmpeg():
    """
    Checks if FFmpeg is installed and accessible.

    Raises:
        EnvironmentError: If FFmpeg is not found in the system path.
    """
    if not shutil.which("ffmpeg"):
        logging.error(
            "FFmpeg is not installed or not in your system PATH. Please install FFmpeg to continue."
        )
        raise EnvironmentError(
            "FFmpeg is required but not found. Install FFmpeg and ensure it's in your system PATH."
        )


def _get_apple_music_bearer_token(playlist_url):
    """
    Captures Apple Music's anonymous web-player API token by loading the playlist
    in a headless browser and sniffing the token off an outgoing request.

    Apple Music's server-rendered playlist page only embeds the first ~300 tracks;
    fetching the rest requires an authenticated call to the catalog API, and the
    bearer token is generated client-side by JavaScript (it is not present anywhere
    in the plain HTML/JS source), so a real browser is needed to obtain it.

    Returns:
        str or None: The "Bearer ..." token, or None if it could not be captured
        (e.g. Playwright/its browsers are not installed).
    """
    try:
        from playwright.sync_api import sync_playwright
    except ImportError:
        logging.warning(
            "playwright is not installed; only the first ~300 tracks of large "
            "playlists can be fetched. Install it with 'pip install playwright' "
            "and 'playwright install chromium' to fetch the full playlist."
        )
        return None

    def capture_token():
        token = None

        def handle_request(request):
            nonlocal token
            if token is not None:
                return
            auth = request.headers.get("authorization")
            if auth and "amp-api.music.apple.com" in request.url:
                token = auth
            elif "devToken=" in request.url:
                match = re.search(r"devToken=([^&]+)", request.url)
                if match:
                    token = f"Bearer {match.group(1)}"

        with sync_playwright() as p:
            browser = p.chromium.launch()
            page = browser.new_page()
            page.on("request", handle_request)
            page.goto(playlist_url, wait_until="domcontentloaded")
            page.wait_for_timeout(4000)
            browser.close()

        return token

    # Playwright's sync API cannot run inside an already-running asyncio event
    # loop (e.g. Jupyter notebooks), so run it in a separate thread which has
    # no event loop of its own.
    with ThreadPoolExecutor(max_workers=1) as executor:
        return executor.submit(capture_token).result()


def _fetch_remaining_playlist_tracks(next_intent, bearer_token):
    """
    Fetches the playlist tracks beyond Apple Music's server-rendered page limit
    (~300 items) using the catalog API endpoint referenced by `nextIntent`.

    Args:
        next_intent (dict): The "nextIntent" object from the playlist page data,
            containing the catalog API pagination URL.
        bearer_token (str): The "Bearer ..." token to authorize the request.

    Returns:
        tuple: A tuple of (songs, artists) lists for the remaining tracks.
    """
    parsed = urlparse(next_intent["url"])
    endpoint = f"https://amp-api.music.apple.com{parsed.path}"
    params = {k: v[0] for k, v in parse_qs(parsed.query).items()}
    headers = {
        "Authorization": bearer_token,
        "Origin": "https://music.apple.com",
    }

    songs = []
    artists = []
    offset = int(params.get("offset", 0))

    while True:
        params["offset"] = offset
        response = requests.get(endpoint, params=params, headers=headers, timeout=15)
        if response.status_code == 404:
            # Offset is past the end of the playlist.
            break
        response.raise_for_status()
        items = response.json().get("data", [])
        if not items:
            break

        for item in items:
            attributes = item.get("attributes", {})
            songs.append(attributes.get("name"))
            artists.append(attributes.get("artistName"))

        offset += len(items)

    return songs, artists


def extract_apple_playlist(playlist_url):
    """
    Extracts the songs and artists from an Apple Music playlist.

    This function assumes the playlist is publicly accessible and follows a consistent structure.

    Note:
        Apple Music only embeds the first ~300 tracks directly in the page HTML.
        For larger playlists, the remaining tracks are fetched via the catalog API,
        which requires a bearer token captured using a headless browser (see
        `_get_apple_music_bearer_token`). If Playwright is not installed, only the
        first ~300 tracks are returned.

    Args:
        playlist_url (str): The URL of the Apple Music playlist.

    Returns:
        tuple: A tuple containing two lists. The first list contains the names of the songs in the playlist, and the second list contains the names of the artists.

    Raises:
        ValueError: If no script tag is found in the HTML or no JSON data is found in the script tag. This can occur if the URL is not valid or the playlist is not public.
    """
    logging.info("Fetching playlist data...")
    response = requests.get(playlist_url)
    soup = BeautifulSoup(response.text, "html.parser")

    script_tag = soup.find("script", id="serialized-server-data")

    if script_tag:
        # get the JSON string
        json_str = script_tag.get_text()
    else:
        logging.error(
            "No script tag found in the HTML. Ensure Valid/Public Playlist URL"
        )
        raise ValueError(
            "No script tag found in the HTML. Ensure Valid/Public Playlist URL"
        )

    if json_str:
        # convert the JSON string into a Python dictionary
        data = json.loads(json_str)
    else:
        logging.error("No JSON data found. Ensure Valid/Public Playlist URL")
        raise ValueError("No JSON data found. Ensure Valid/Public Playlist URL")

    page_data = data["data"][0]["data"]
    playlistJson = page_data["sections"][1]["items"]

    songs = []
    artists = []

    for song in playlistJson:
        songs.append(song["title"])
        artists.append(song["artistName"])

    # The page only server-renders the first ~300 tracks; fetch the rest if present.
    next_intent = page_data.get("nextIntent")
    if next_intent and next_intent.get("$kind") == "PlaylistPaginationIntent":
        bearer_token = _get_apple_music_bearer_token(playlist_url)
        if bearer_token:
            more_songs, more_artists = _fetch_remaining_playlist_tracks(
                next_intent, bearer_token
            )
            songs.extend(more_songs)
            artists.extend(more_artists)

    logging.info(f"Found {len(songs)} songs in the playlist.")
    return songs, artists



def search_youtube(songs, artists, max_threads=5):
    """
    Searches YouTube for official audio videos of songs by given artists using parallel threads.

    Parameters:
    - songs (list): A list of song titles.
    - artists (list): A list of artist names.
    - max_threads (int): Maximum number of threads to use for parallel searches. Default is 5.

    Returns:
    - youtube_urls (list): A list of YouTube URLs for official audio videos of the songs.
    """
    youtube_urls = []
    base_youtube_url = "https://www.youtube.com"

    def search_single_song(song, artist):
        """Helper function to search for a single song."""
        query = f"{song} {artist} official audio"
        search_url = (
            f"https://www.youtube.com/results?search_query={query.replace(' ', '+')}"
        )
        response = requests.get(search_url)
        if response.status_code == 200:
            match = re.search(r"\"url\":\"(/watch\?v=[^\"]+)", response.text)
            if match:
                video_url = base_youtube_url + match.group(1).split("\\")[0]
                return video_url
        logging.warning(f"No YouTube video found for '{song}' by '{artist}'")
        return None

    with ThreadPoolExecutor(max_threads) as executor:
        futures = {
            executor.submit(search_single_song, song, artist): (song, artist)
            for song, artist in zip(songs, artists)
        }

        for future in tqdm(as_completed(futures), total=len(futures), desc="Searching"):
            result = future.result()
            if result:
                youtube_urls.append(result)

    return youtube_urls


def download_single_song(url, output_path, ydl_opts):
    """Helper function to download a single song."""
    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            ydl.download([url])
    except Exception as e:
        logging.warning(f"Failed to download: {url} ({e})")
    return url


def download_youtube_audio(youtube_urls, output_dir="output", max_threads=5):
    """
    Downloads the audio from the given YouTube URLs and saves them as MP3 files in the specified output path.

    Note:
        This function requires FFmpeg to be installed for audio extraction when using yt-dlp.

    Parameters:
        youtube_urls (list): A list of YouTube URLs from which the audio will be downloaded.
        output_path (str): The directory path where the downloaded audio files will be saved.
        max_threads (int): The maximum number of threads to use for parallel downloading. Default is 5.

    Returns:
        None
    """
    ydl_opts = {
        "format": "bestaudio/best",
        "outtmpl": output_dir + "/%(title)s.%(ext)s",
        "quiet": True,
        "no-warnings": True,
        "progress_hooks": [lambda _: None],
        "postprocessors": [
            {
                "key": "FFmpegExtractAudio",
                "preferredcodec": "mp3",
                "preferredquality": "192",
            }
        ],
        "logger": logging.getLogger("null"),
        "no-progress": True,
        "postprocessor-args": ["-loglevel", "quiet"],
    }

    with ThreadPoolExecutor(max_threads) as executor:
        futures = [
            executor.submit(download_single_song, url, output_dir, ydl_opts)
            for url in youtube_urls
        ]
        for future in tqdm(
            as_completed(futures), total=len(futures), desc="Downloading"
        ):
            try:
                future.result()
            except Exception as e:
                logging.error(f"Error downloading file: {e}")


def main():
    print(pyfiglet.figlet_format("songs"))
    logging.info("Welcome to Apple Playlist YouTube Downloader CLI")
    print("================================================\n")

    # Argument parser setup
    parser = argparse.ArgumentParser(
        description="Download Apple Music playlists as audio files from YouTube."
    )
    parser.add_argument(
        "playlist_url",
        metavar="URL",
        type=str,
        help="Apple Music Playlist URL to process",
    )
    parser.add_argument(
        "-t",
        "--threads",
        type=int,
        default=5,
        help="Number of threads to use for parallel downloads (default: 5)",
    )
    parser.add_argument(
        "-o",
        "--output",
        type=str,
        default="output",
        help="Output directory for downloaded audio files (default: output)",
    )

    args = parser.parse_args()
    url = args.playlist_url
    max_threads = args.threads
    output_dir = args.output

    logging.info(f"Extracting Music Using {max_threads} threads")
    logging.info(f"Output Directory: {output_dir}\n")

    try:
        check_ffmpeg()
        logging.info("Extracting playlist songs and artists...")
        songs, artists = extract_apple_playlist(url)

        # Parallel YouTube search
        logging.info("Searching YouTube for song URLs...")
        yt_urls = search_youtube(songs, artists, max_threads=max_threads)

        # Parallel audio download
        os.makedirs(output_dir, exist_ok=True)
        logging.info("Starting downloads...")
        download_youtube_audio(yt_urls, output_dir=output_dir, max_threads=max_threads)

        logging.info("All downloads completed successfully!")
    except Exception as e:
        logging.error(f"Error: {e}")


if __name__ == "__main__":
    main()
