package com.songs.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AddResultTest {

  @Test
  void switchHandlesAddedAndAddFailed() {
    Track track = new Track("Title", "Artist", null, null, null);

    AddResult added = new AddResult.Added(track, "/music/song.mp3");
    AddResult failed = new AddResult.AddFailed(track, "download failed");

    assertEquals("added: /music/song.mp3", describe(added));
    assertEquals("failed: download failed", describe(failed));
  }

  private String describe(AddResult result) {
    return switch (result) {
      case AddResult.Added a -> "added: " + a.addedRef();
      case AddResult.AddFailed f -> "failed: " + f.error();
    };
  }
}
