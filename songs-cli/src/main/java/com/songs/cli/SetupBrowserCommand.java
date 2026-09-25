package com.songs.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

@Command(
    name = "setup-browser",
    description = "Install the Playwright Chromium browser.",
    mixinStandardHelpOptions = true)
final class SetupBrowserCommand implements Callable<Integer> {
  @Override
  public Integer call() {
    try {
      String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
      Process process =
          new ProcessBuilder(
                  java,
                  "-cp",
                  System.getProperty("java.class.path"),
                  "com.microsoft.playwright.CLI",
                  "install",
                  "chromium")
              .inheritIO()
              .start();
      return process.waitFor();
    } catch (IOException e) {
      System.err.println("Could not install Playwright Chromium: " + e.getMessage());
      return 1;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      System.err.println("Playwright browser installation was interrupted.");
      return 1;
    }
  }
}
