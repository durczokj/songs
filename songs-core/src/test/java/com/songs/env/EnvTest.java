package com.songs.env;

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
    void playwrightBrowserCheckInspectsCacheWithoutStartingPlaywright() {
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
