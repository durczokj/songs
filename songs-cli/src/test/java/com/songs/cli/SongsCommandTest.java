package com.songs.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SongsCommandTest {

    @Test
    void helpListsCommandsAndGlobalFlags() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CommandLine command = new CommandLine(new SongsCommand())
            .setOut(new PrintWriter(output, true));

        int exitCode = command.execute("--help");

        String text = output.toString();
        assertEquals(0, exitCode);
        assertTrue(text.contains("sync"));
        assertTrue(text.contains("show"));
        assertTrue(text.contains("doctor"));
        assertTrue(text.contains("--quiet"));
        assertTrue(text.contains("--verbose"));
    }

    @Test
    void versionPrintsApplicationVersion() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CommandLine command = new CommandLine(new SongsCommand())
            .setOut(new PrintWriter(output, true));

        int exitCode = command.execute("--version");

        assertEquals(0, exitCode);
        assertEquals("songs 0.1.2", output.toString().trim());
    }

    @Test
    void missingArgumentsReturnUsageError() {
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        CommandLine command = new CommandLine(new SongsCommand())
            .setErr(new PrintWriter(error, true));

        int exitCode = command.execute("sync");

        assertEquals(2, exitCode);
        assertTrue(error.toString().contains("Missing required parameters"));
    }

    @Test
    void invalidConcurrencyReturnsUsageError(@TempDir Path tempDir) {
        String uri = tempDir.toUri().toString();

        int exitCode = new CommandLine(new SongsCommand()).execute(
            "sync", "--concurrency", "0", uri, uri
        );

        assertEquals(2, exitCode);
    }

    @Test
    void dryRunPrintsPlanWithoutChangingEmptyLocalTarget(@TempDir Path tempDir) throws IOException {
        Path source = tempDir.resolve("source");
        Path target = tempDir.resolve("target");
        Files.createDirectories(source);
        Files.createDirectories(target);
        String sourceUri = source.toUri().toString();
        String targetUri = target.toUri().toString();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(output));
            int exitCode = new CommandLine(new SongsCommand()).execute(
                "sync", "--dry-run", sourceUri, targetUri
            );

            assertEquals(0, exitCode);
            assertTrue(output.toString().contains("unchanged 0  add 0  remove 0"));
            assertTrue(Files.isDirectory(target));
        } finally {
            System.setOut(originalOut);
        }
    }
}
