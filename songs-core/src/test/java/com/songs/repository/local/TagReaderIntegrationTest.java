package com.songs.repository.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.songs.model.Track;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

// Generates a temporary MP3 with ffmpeg; excluded from the default `mvn test` run.
@Tag("integration")
class TagReaderIntegrationTest {

  @Test
  void writesAndReadsId3Tags(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
    assumeTrue(isFfmpegAvailable(), "ffmpeg is not installed on this machine");
    Path audioPath = tempDir.resolve("tag-reader-test.mp3");
    createSilentMp3(audioPath);

    TagReader tagReader = new TagReader();
    Track expected = new Track("Test title", "Test artist", "Test album", null, "TEST12345678");

    tagReader.writeTags(audioPath, expected);
    Track actual = tagReader.readTags(audioPath);

    assertEquals(expected.title(), actual.title());
    assertEquals(expected.artist(), actual.artist());
    assertEquals(expected.album(), actual.album());
    assertEquals(expected.isrc(), actual.isrc());
    assertEquals(1, actual.durationSec());
  }

  private static void createSilentMp3(Path outputPath) throws IOException, InterruptedException {
    Process process =
        new ProcessBuilder(
                "ffmpeg",
                "-y",
                "-f",
                "lavfi",
                "-i",
                "anullsrc=r=44100:cl=mono",
                "-t",
                "1",
                "-q:a",
                "9",
                outputPath.toString())
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start();
    if (!process.waitFor(30, TimeUnit.SECONDS) || process.exitValue() != 0) {
      process.destroyForcibly();
      throw new IOException("ffmpeg could not create the test MP3");
    }
  }

  private static boolean isFfmpegAvailable() {
    try {
      Process process =
          new ProcessBuilder("ffmpeg", "-version")
              .redirectOutput(ProcessBuilder.Redirect.DISCARD)
              .redirectError(ProcessBuilder.Redirect.DISCARD)
              .start();
      return process.waitFor() == 0;
    } catch (IOException e) {
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }
}
