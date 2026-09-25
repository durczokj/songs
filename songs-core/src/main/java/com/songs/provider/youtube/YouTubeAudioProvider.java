package com.songs.provider.youtube;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.songs.http.HttpClient;
import com.songs.model.AudioRef;
import com.songs.model.DownloadResult;
import com.songs.model.Track;
import com.songs.model.TrackResolution;
import com.songs.provider.AudioProvider;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YouTubeAudioProvider implements AudioProvider {

  private static final String BASE_URL = "https://www.youtube.com";
  private static final String INNERTUBE_SEARCH_URL =
      "https://www.youtube.com/youtubei/v1/search?prettyPrint=false";
  private static final Duration SEARCH_TIMEOUT = Duration.ofSeconds(15);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Logger logger = LoggerFactory.getLogger(YouTubeAudioProvider.class);

  private static final Pattern VIDEO_ID =
      Pattern.compile("\"videoId\"\\s*:\\s*\"([a-zA-Z0-9_-]{11})\"");

  private final HttpClient httpClient;
  private final YtDlpConfig config;

  public YouTubeAudioProvider(HttpClient httpClient, YtDlpConfig config) {
    this.httpClient = httpClient;
    this.config = config;
  }

  @Override
  public String name() {
    return "youtube";
  }

  @Override
  public TrackResolution resolveOne(Track track) {
    String query = buildSearchQuery(track);
    logger.debug("Searching YouTube for {}", query);
    try {
      String jsonPayload =
          MAPPER.writeValueAsString(
              Map.of(
                  "context",
                  Map.of(
                      "client",
                      Map.of(
                          "clientName", "WEB",
                          "clientVersion", "2.20240101.00.00",
                          "hl", "en",
                          "gl", "US")),
                  "query",
                  query));
      String response = httpClient.post(INNERTUBE_SEARCH_URL, jsonPayload, SEARCH_TIMEOUT);
      String videoUrl = extractFirstWatchUrl(response);
      if (videoUrl == null) {
        logger.warn("No YouTube video found for {}", track);
        return new TrackResolution.Failed(track, "No YouTube video found");
      }
      logger.debug("Resolved {} to {}", track, videoUrl);
      return new TrackResolution.Resolved(track, new AudioRef(name(), videoUrl));
    } catch (IOException e) {
      logger.warn("YouTube search failed for {}: {}", track, e.getMessage());
      return new TrackResolution.Failed(track, "Search failed: " + e.getMessage());
    }
  }

  @Override
  public DownloadResult downloadOne(TrackResolution resolution, Path outputDir) {
    if (!(resolution instanceof TrackResolution.Resolved resolved)) {
      TrackResolution.Failed failed = (TrackResolution.Failed) resolution;
      logger.warn("Skipping download for unresolved track {}: {}", failed.track(), failed.error());
      return new DownloadResult.DownloadFailed(
          resolution, "Cannot download an unresolved track: " + failed.error());
    }
    logger.info("Downloading {} to {}", resolved.track(), outputDir);

    List<String> command =
        List.of(
            config.binaryPath(),
            "--quiet",
            "--no-warnings",
            "-x",
            "--audio-format",
            "mp3",
            "-o",
            outputDir.resolve(config.outputTemplate()).toString(),
            "--print",
            "after_move:filepath",
            resolved.ref().url());

    try {
      Process process = new ProcessBuilder(command).start();

      boolean finished = process.waitFor(config.timeout().toSeconds(), TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
        logger.warn("yt-dlp timed out for {}", resolved.track());
        return new DownloadResult.DownloadFailed(
            resolution, "yt-dlp timed out after " + config.timeout());
      }
      if (process.exitValue() != 0) {
        String stderr = readAll(process.getErrorStream());
        logger.warn(
            "yt-dlp failed for {} with exit code {}", resolved.track(), process.exitValue());
        return new DownloadResult.DownloadFailed(
            resolution, "yt-dlp exited " + process.exitValue() + ": " + stderr.trim());
      }

      String filePath = lastNonBlankLine(readAll(process.getInputStream()));
      if (filePath == null) {
        logger.warn("yt-dlp did not report an output file for {}", resolved.track());
        return new DownloadResult.DownloadFailed(
            resolution, "yt-dlp did not report an output file path");
      }
      Path outputPath = Path.of(filePath);
      logger.info("Downloaded {} to {}", resolved.track(), outputPath);
      return new DownloadResult.Downloaded(resolution, outputPath);
    } catch (IOException e) {
      logger.warn("Could not start yt-dlp for {}: {}", resolved.track(), e.getMessage());
      return new DownloadResult.DownloadFailed(
          resolution, "Failed to start yt-dlp: " + e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.warn("Download interrupted for {}", resolved.track());
      return new DownloadResult.DownloadFailed(resolution, "Interrupted while downloading");
    }
  }

  // Package-private for testing without network.
  static String buildSearchQuery(Track track) {
    return track.title() + " " + track.artist() + " official audio";
  }

  // Package-private so the fixture-based test can exercise it without network.
  static String extractFirstWatchUrl(String content) {
    if (content == null || content.isBlank()) {
      return null;
    }
    Matcher videoIdMatcher = VIDEO_ID.matcher(content);
    if (videoIdMatcher.find()) {
      return BASE_URL + "/watch?v=" + videoIdMatcher.group(1);
    }
    return null;
  }

  private static String readAll(InputStream in) throws IOException {
    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
  }

  private static String lastNonBlankLine(String text) {
    String result = null;
    for (String line : text.lines().toList()) {
      if (!line.isBlank()) {
        result = line.trim();
      }
    }
    return result;
  }
}
