package com.songs.cli;

import com.microsoft.playwright.CLI;
import picocli.CommandLine.Command;

import java.io.IOException;
import java.util.concurrent.Callable;

@Command(name = "setup-browser", description = "Install the Playwright Chromium browser.")
final class SetupBrowserCommand implements Callable<Integer> {
    @Override
    public Integer call() {
        try {
            CLI.main(new String[]{"install", "chromium"});
            return 0;
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