package com.songs.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DownloadResultTest {

  @Test
  void switchHandlesDownloadedAndDownloadFailed() {
    Track track = new Track("Title", "Artist", null, null, null);
    TrackResolution resolution =
        new TrackResolution.Resolved(track, new AudioRef("youtube", "https://youtube.com/x"));

    DownloadResult downloaded = new DownloadResult.Downloaded(resolution, Path.of("/tmp/song.mp3"));
    DownloadResult failed = new DownloadResult.DownloadFailed(resolution, "ffmpeg missing");

    assertEquals("saved: /tmp/song.mp3", describe(downloaded));
    assertEquals("failed: ffmpeg missing", describe(failed));
  }

  private String describe(DownloadResult result) {
    return switch (result) {
      case DownloadResult.Downloaded d -> "saved: " + d.outputPath();
      case DownloadResult.DownloadFailed f -> "failed: " + f.error();
    };
  }
}
