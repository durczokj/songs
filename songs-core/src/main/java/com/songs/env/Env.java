package com.songs.env;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.microsoft.playwright.Playwright;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Env {

    private static final Logger logger = LoggerFactory.getLogger(Env.class);
    private Env() {
    }

    public static boolean isFfmpegAvailable() {
        return isCommandAvailable("ffmpeg", "-version");
    }

    /** yt-dlp needs a JS runtime on PATH (deno or node) to extract current YouTube streams. */
    public static boolean isJsRuntimeAvailable() {
        return isCommandAvailable("deno", "--version") || isCommandAvailable("node", "--version");
    }

    public static boolean isPlaywrightBrowserAvailable() {
        try (Playwright playwright = Playwright.create()) {
            Path executable = Path.of(playwright.chromium().executablePath());
            boolean available = Files.isExecutable(executable);
            logger.debug("Playwright Chromium available at {}: {}", executable, available);
            return available;
        } catch (RuntimeException e) {
            logger.debug("Playwright Chromium is unavailable: {}", e.getMessage());
            return false;
        }
    }

    public static void requireFfmpeg() {
        if (!isFfmpegAvailable()) {
            logger.warn("FFmpeg is not available on PATH");
            throw new IllegalStateException(
                "FFmpeg is required but not found. Install FFmpeg and ensure it's in your system PATH."
            );
        }
    }

    // Package-private so tests can probe the underlying check with a known-missing command.
    static boolean isCommandAvailable(String command, String versionFlag) {
        try {
            Process process = new ProcessBuilder(command, versionFlag)
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

