package com.songs.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyncResultTest {

    @Test
    void mutatingInputListsDoesNotAffectSyncResult() {
        Track track = new Track("Title", "Artist", null, null, null);
        SyncPlan plan = new SyncPlan("apple://a", "file://b", List.of(track), List.of(), List.of());
        List<AddResult> added = new ArrayList<>(List.of(new AddResult.Added(track, "/music/song.mp3")));

        SyncResult result = new SyncResult(plan, added, List.of());
        added.add(new AddResult.AddFailed(track, "boom"));

        assertEquals(1, result.added().size());
    }

    @Test
    void syncResultListsAreImmutable() {
        SyncPlan plan = new SyncPlan("apple://a", "file://b", List.of(), List.of(), List.of());
        SyncResult result = new SyncResult(plan, List.of(), List.of());

        try {
            result.removed().add(new RemoveResult.Removed(new Track("Title", "Artist", null, null, null)));
            throw new AssertionError("expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // immutable list rejects mutation, as expected
        }
    }
}
