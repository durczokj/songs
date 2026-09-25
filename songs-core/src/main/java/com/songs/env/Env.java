package com.songs.env;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Env {

  private static final Logger logger = LoggerFactory.getLogger(Env.class);

  private Env() {}

  public static boolean isFfmpegAvailable() {
    return isCommandAvailable("ffmpeg", "-version");
  }

  /** yt-dlp needs a JS runtime on PATH (deno or node) to extract current YouTube streams. */
  public static boolean isJsRuntimeAvailable() {
    return isCommandAvailable("deno", "--version") || isCommandAvailable("node", "--version");
  }

  public static boolean isPlaywrightBrowserAvailable() {
    return isPlaywrightBrowserAvailable(playwrightBrowserCache());
  }

  static boolean isPlaywrightBrowserAvailable(Path browserCache) {
    // Playwright writes an INSTALLATION_COMPLETE marker into each browser directory
    // once its install finishes; stable across Chromium/Chrome-for-Testing renames.
    try (Stream<Path> entries = Files.list(browserCache)) {
      boolean available =
          entries
              .filter(Files::isDirectory)
              .filter(dir -> dir.getFileName().toString().startsWith("chromium-"))
              .anyMatch(dir -> Files.isRegularFile(dir.resolve("INSTALLATION_COMPLETE")));
      logger.debug("Playwright Chromium available in {}: {}", browserCache, available);
      return available;
    } catch (IOException e) {
      logger.debug("Playwright Chromium is unavailable in {}: {}", browserCache, e.getMessage());
      return false;
    }
  }

  private static Path playwrightBrowserCache() {
    String configuredPath = System.getenv("PLAYWRIGHT_BROWSERS_PATH");
    if (configuredPath != null && !configuredPath.isBlank()) {
      return Path.of(configuredPath);
    }

    String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
    if (os.contains("mac")) {
      return Path.of(System.getProperty("user.home"), "Library", "Caches", "ms-playwright");
    }
    if (os.contains("win")) {
      String localAppData = System.getenv("LOCALAPPDATA");
      return Path.of(
          localAppData == null ? System.getProperty("user.home") : localAppData, "ms-playwright");
    }
    return Path.of(System.getProperty("user.home"), ".cache", "ms-playwright");
  }

  public static void requireFfmpeg() {
    if (!isFfmpegAvailable()) {
      logger.warn("FFmpeg is not available on PATH");
      throw new IllegalStateException(
          "FFmpeg is required but not found. Install FFmpeg and ensure it's in your system PATH.");
    }
  }

  // Package-private so tests can probe the underlying check with a known-missing command.
  static boolean isCommandAvailable(String command, String versionFlag) {
    try {
      Process process =
          new ProcessBuilder(command, versionFlag)
              .redirectOutput(ProcessBuilder.Redirect.DISCARD)
              .redirectError(ProcessBuilder.Redirect.DISCARD)
              .start();
      boolean available = process.waitFor() == 0;
      logger.debug("Command {} available: {}", command, available);
      return available;
    } catch (IOException e) {
      logger.debug("Command {} is unavailable: {}", command, e.getMessage());
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.warn("Interrupted while checking command {}", command);
      return false;
    }
  }
}
