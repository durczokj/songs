package com.songs.cli;

import com.songs.env.Env;
import java.io.IOException;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

@Command(
    name = "doctor",
    description = "Check required external tools.",
    mixinStandardHelpOptions = true)
final class DoctorCommand implements Callable<Integer> {
  @Override
  public Integer call() {
    boolean ffmpeg = Env.isFfmpegAvailable();
    boolean js = Env.isJsRuntimeAvailable();
    boolean ytdlp = commandAvailable("yt-dlp");
    boolean browser = Env.isPlaywrightBrowserAvailable();
    System.out.printf("ffmpeg: %s%n", available(ffmpeg));
    System.out.printf("yt-dlp: %s%n", available(ytdlp));
    System.out.printf("JavaScript runtime: %s%n", available(js));
    System.out.printf("Playwright Chromium: %s%n", available(browser));
    return ffmpeg && ytdlp && js && browser ? 0 : 1;
  }

  private static boolean commandAvailable(String command) {
    try {
      Process process =
          new ProcessBuilder(command, "--version")
              .redirectError(ProcessBuilder.Redirect.DISCARD)
              .redirectOutput(ProcessBuilder.Redirect.DISCARD)
              .start();
      return process.waitFor() == 0;
    } catch (IOException e) {
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private static String available(boolean value) {
    return value ? "ok" : "missing";
  }
}
