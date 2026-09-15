import os
import sys

# Make the project root (parent of this notebooks/ folder) importable so `main` resolves.
_PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), os.pardir))
if _PROJECT_ROOT not in sys.path:
    sys.path.insert(0, _PROJECT_ROOT)
os.chdir(_PROJECT_ROOT)

from legacy.main import check_ffmpeg, extract_apple_playlist, search_youtube, download_youtube_audio

import requests
import argparse
import pyfiglet
from bs4 import BeautifulSoup
from concurrent.futures import ThreadPoolExecutor, as_completed
import json
import sys
import re
import yt_dlp
from tqdm import tqdm
import logging
import os
import shutil


url = "https://music.apple.com/pl/playlist/favourite-107950/pl.u-KVXBDGJFd29j5g"
max_threads = 8
output_dir = "/usr/local/Caskroom/miniconda/base/envs/srd/lib/python3.12/site-packages/songs/data"

logging.info(f"Extracting Music Using {max_threads} threads")
logging.info(f"Output Directory: {output_dir}\n")

check_ffmpeg()
logging.info("Extracting playlist songs and artists...")
songs, artists = extract_apple_playlist(url)