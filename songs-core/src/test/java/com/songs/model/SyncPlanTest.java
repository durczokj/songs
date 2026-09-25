package com.songs.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SyncPlanTest {

  @Test
  void mutatingInputListsDoesNotAffectSyncPlan() {
    Track track = new Track("Title", "Artist", null, null, null);
    List<Track> toAdd = new ArrayList<>(List.of(track));

    SyncPlan plan = new SyncPlan("apple://a", "file://b", toAdd, List.of(), List.of());
    toAdd.add(new Track("Other", "Artist2", null, null, null));

    assertEquals(1, plan.toAdd().size());
  }

  @Test
  void syncPlanListsAreImmutable() {
    SyncPlan plan = new SyncPlan("apple://a", "file://b", List.of(), List.of(), List.of());

    try {
      plan.toAdd().add(new Track("Title", "Artist", null, null, null));
      throw new AssertionError("expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
      // immutable list rejects mutation, as expected
    }
  }
}
