package com.songs.env;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnvTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void playwrightBrowserCheckReturnsFalseForEmptyCache() {
        assertFalse(Env.isPlaywrightBrowserAvailable(temporaryDirectory));
    }

    @Test
    void playwrightBrowserCheckReturnsTrueWhenChromiumMarkerExists() throws IOException {
        Path chromiumDir = Files.createDirectory(temporaryDirectory.resolve("chromium-1243"));
        Files.createFile(chromiumDir.resolve("INSTALLATION_COMPLETE"));
        assertTrue(Env.isPlaywrightBrowserAvailable(temporaryDirectory));
    }

    @Test
    void playwrightBrowserCheckIgnoresUnrelatedBrowsers() throws IOException {
        Path firefox = Files.createDirectory(temporaryDirectory.resolve("firefox-1543"));
        Files.createFile(firefox.resolve("INSTALLATION_COMPLETE"));
        Path headlessShell = Files.createDirectory(temporaryDirectory.resolve("chromium_headless_shell-1243"));
        Files.createFile(headlessShell.resolve("INSTALLATION_COMPLETE"));
        assertFalse(Env.isPlaywrightBrowserAvailable(temporaryDirectory));
    }

    @Test
    void isCommandAvailableReturnsFalseForUnknownCommand() {
        assertFalse(Env.isCommandAvailable("definitely-not-a-real-binary-xyz", "--version"));
    }

    @Test
    void isCommandAvailableReturnsTrueForAKnownCommand() {
        // The JVM running this test guarantees "java" is on PATH.
        assertTrue(Env.isCommandAvailable("java", "-version"));
    }

    @Test
    void ffmpegAndJsRuntimeChecksReflectThisMachineWithoutThrowing() {
        // These reflect whatever is actually installed; we only assert they run cleanly.
        assertDoesNotThrow(Env::isFfmpegAvailable);
        assertDoesNotThrow(Env::isJsRuntimeAvailable);
    }

    @Test
    void requireFfmpegThrowsOnlyWhenFfmpegIsMissing() {
        if (Env.isFfmpegAvailable()) {
            assertDoesNotThrow(Env::requireFfmpeg);
        } else {
            assertThrows(IllegalStateException.class, Env::requireFfmpeg);
        }
    }
}
