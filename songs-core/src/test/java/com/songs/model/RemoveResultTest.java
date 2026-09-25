package com.songs.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RemoveResultTest {

  @Test
  void switchHandlesRemovedAndRemoveFailed() {
    Track track = new Track("Title", "Artist", null, null, null);

    RemoveResult removed = new RemoveResult.Removed(track);
    RemoveResult failed = new RemoveResult.RemoveFailed(track, "file locked");

    assertEquals("removed: Title", describe(removed));
    assertEquals("failed: file locked", describe(failed));
  }

  private String describe(RemoveResult result) {
    return switch (result) {
      case RemoveResult.Removed r -> "removed: " + r.track().title();
      case RemoveResult.RemoveFailed f -> "failed: " + f.error();
    };
  }
}
