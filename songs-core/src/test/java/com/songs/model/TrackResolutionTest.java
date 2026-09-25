package com.songs.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TrackResolutionTest {

  @Test
  void switchHandlesResolvedAndFailed() {
    Track track = new Track("Title", "Artist", null, null, null);

    TrackResolution resolved =
        new TrackResolution.Resolved(track, new AudioRef("youtube", "https://youtube.com/x"));
    TrackResolution failed = new TrackResolution.Failed(track, "not found");

    assertEquals("found: https://youtube.com/x", describe(resolved));
    assertEquals("failed: not found", describe(failed));
  }

  private String describe(TrackResolution resolution) {
    return switch (resolution) {
      case TrackResolution.Resolved r -> "found: " + r.ref().url();
      case TrackResolution.Failed f -> "failed: " + f.error();
    };
  }
}
