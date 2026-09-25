package com.songs.provider.youtube;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.songs.model.AudioRef;
import com.songs.model.DownloadResult;
import com.songs.model.Track;
import com.songs.model.TrackResolution;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

// Shells out to the real yt-dlp binary and downloads a real file; excluded from the
// default `mvn test` run. Run with: mvn test -DexcludedGroups=
@Tag("integration")
class YouTubeAudioProviderIntegrationTest {

  @Test
  void downloadsARealTrackWithYtDlp(@org.junit.jupiter.api.io.TempDir Path tempDir)
      throws Exception {
    assumeTrue(isYtDlpAvailable(), "yt-dlp is not installed on this machine");

    YouTubeAudioProvider provider = new YouTubeAudioProvider(null, new YtDlpConfig());
    Track track = new Track("99 Luftballons", "Nena", null, null, null);
    TrackResolution resolution =
        new TrackResolution.Resolved(
            track, new AudioRef("youtube", "https://www.youtube.com/watch?v=Fpu5a0Bl8eY"));

    DownloadResult result = provider.downloadOne(resolution, tempDir);

    DownloadResult.Downloaded downloaded =
        assertInstanceOf(DownloadResult.Downloaded.class, result);
    assertTrue(
        java.nio.file.Files.isRegularFile(downloaded.outputPath()),
        "downloaded file should exist on disk");
    assertTrue(
        downloaded.outputPath().toString().endsWith(".mp3"), "downloaded file should be an mp3");
  }

  private static boolean isYtDlpAvailable() {
    try {
      Process process =
          new ProcessBuilder("yt-dlp", "--version")
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
