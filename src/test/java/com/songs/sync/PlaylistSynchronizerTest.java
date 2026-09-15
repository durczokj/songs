package com.songs.sync;

import com.songs.matching.ExactTrackMatcher;
import com.songs.model.AddResult;
import com.songs.model.Playlist;
import com.songs.model.RemoveResult;
import com.songs.model.SyncPlan;
import com.songs.model.SyncResult;
import com.songs.model.Track;
import com.songs.repository.PlaylistWriter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaylistSynchronizerTest {

    private final PlaylistSynchronizer sync = new PlaylistSynchronizer(new ExactTrackMatcher());

    private static Track t(String title, String artist) {
        return new Track(title, artist, null, null, null);
    }

    private static Track tIsrc(String title, String artist, String isrc) {
        return new Track(title, artist, null, null, isrc);
    }

    private static Playlist source(List<Track> tracks) {
        return new Playlist("apple://src", tracks, "src");
    }

    private static Playlist target(List<Track> tracks) {
        return new Playlist("file:///tgt", tracks, "tgt");
    }

    @Test
    void bothEmptyProducesEmptyPlan() {
        SyncPlan plan = sync.plan(source(List.of()), target(List.of()));

        assertEquals("apple://src", plan.sourceUri());
        assertEquals("file:///tgt", plan.targetUri());
        assertTrue(plan.toAdd().isEmpty());
        assertTrue(plan.toRemove().isEmpty());
        assertTrue(plan.unchanged().isEmpty());
    }

    @Test
    void emptyTargetPutsEverySourceTrackInToAdd() {
        Track a = t("Song A", "Artist A");
        Track b = t("Song B", "Artist B");

        SyncPlan plan = sync.plan(source(List.of(a, b)), target(List.of()));

        assertEquals(List.of(a, b), plan.toAdd());
        assertTrue(plan.toRemove().isEmpty());
        assertTrue(plan.unchanged().isEmpty());
    }

    @Test
    void emptySourcePutsEveryTargetTrackInToRemove() {
        Track a = t("Song A", "Artist A");
        Track b = t("Song B", "Artist B");

        SyncPlan plan = sync.plan(source(List.of()), target(List.of(a, b)));

        assertTrue(plan.toAdd().isEmpty());
        assertEquals(List.of(a, b), plan.toRemove());
        assertTrue(plan.unchanged().isEmpty());
    }

    @Test
    void partialOverlapPartitionsCorrectly() {
        Track shared = t("Shared", "Artist");
        Track onlyInSource = t("Only Source", "Artist");
        Track onlyInTarget = t("Only Target", "Artist");

        SyncPlan plan = sync.plan(
            source(List.of(shared, onlyInSource)),
            target(List.of(shared, onlyInTarget))
        );

        assertEquals(List.of(onlyInSource), plan.toAdd());
        assertEquals(List.of(onlyInTarget), plan.toRemove());
        assertEquals(List.of(shared), plan.unchanged());
    }

    @Test
    void isrcMatchIsUnchangedEvenWhenTitleAndArtistDiffer() {
        Track sourceTrack = tIsrc("Original Title", "Original Artist", "USRC17607839");
        Track targetTrack = tIsrc("Completely Different", "Someone Else", "USRC17607839");

        SyncPlan plan = sync.plan(source(List.of(sourceTrack)), target(List.of(targetTrack)));

        assertEquals(List.of(sourceTrack), plan.unchanged());
        assertTrue(plan.toAdd().isEmpty());
        assertTrue(plan.toRemove().isEmpty());
    }

    @Test
    void unchangedPreservesSourceOrderAndSourceInstance() {
        Track s1 = t("First", "Artist");
        Track s2 = t("Second", "Artist");
        Track s3 = t("Third", "Artist");
        // Target scrambled and using different casing to force normalization matching.
        Track t3 = t("THIRD", "artist");
        Track t1 = t("first", "ARTIST");
        Track t2 = t("SECOND", "Artist");

        SyncPlan plan = sync.plan(source(List.of(s1, s2, s3)), target(List.of(t3, t1, t2)));

        // Unchanged reports source-side tracks in source order.
        assertEquals(List.of(s1, s2, s3), plan.unchanged());
        assertTrue(plan.toAdd().isEmpty());
        assertTrue(plan.toRemove().isEmpty());
    }

    @Test
    void duplicateSourceTracksAreDeduplicated() {
        Track dupA = t("Duplicated", "Artist");
        Track dupB = t("Duplicated", "Artist");
        Track once = t("Duplicated", "Artist");

        SyncPlan plan = sync.plan(source(List.of(dupA, dupB)), target(List.of(once)));

        // Set semantics keep the first source occurrence and ignore the duplicate.
        assertEquals(List.of(dupA), plan.unchanged());
        assertTrue(plan.toAdd().isEmpty());
        assertTrue(plan.toRemove().isEmpty());
    }

    @Test
    void duplicateSourceTracksAreAddedOnlyOnceToEmptyTarget() {
        Track first = t("Duplicated", "Artist");
        Track duplicate = t("DUPLICATED", "artist");

        SyncPlan plan = sync.plan(source(List.of(first, duplicate)), target(List.of()));

        assertEquals(List.of(first), plan.toAdd());
        assertTrue(plan.unchanged().isEmpty());
        assertTrue(plan.toRemove().isEmpty());
    }

    @Test
    void planIsPureAndDoesNotMutateInputs() {
        Track a = t("Song A", "Artist");
        Track b = t("Song B", "Artist");
        Playlist src = source(List.of(a));
        Playlist tgt = target(List.of(b));

        sync.plan(src, tgt);

        assertEquals(List.of(a), src.tracks());
        assertEquals(List.of(b), tgt.tracks());
    }

    // --- apply ---

    @Test
    void applyDelegatesAddAndRemoveToTargetAndReturnsAlignedResult() {
        Track toAdd = t("New", "Artist");
        Track toRemove = t("Old", "Artist");
        Track keep = t("Same", "Artist");
        SyncPlan plan = sync.plan(
            source(List.of(keep, toAdd)),
            target(List.of(keep, toRemove))
        );
        RecordingPlaylistWriter writer = new RecordingPlaylistWriter();

        SyncResult result = sync.apply(plan, writer);

        assertSame(plan, result.plan());
        assertEquals(List.of("file:///tgt"), writer.addUris);
        assertEquals(List.of(List.of(toAdd)), writer.addBatches);
        assertEquals(List.of("file:///tgt"), writer.removeUris);
        assertEquals(List.of(List.of(toRemove)), writer.removeBatches);
        assertEquals(1, result.added().size());
        assertInstanceOf(AddResult.Added.class, result.added().get(0));
        assertEquals(1, result.removed().size());
        assertInstanceOf(RemoveResult.Removed.class, result.removed().get(0));
    }

    @Test
    void applySkipsWriterCallsWhenNothingToAddOrRemove() {
        Track same = t("Same", "Artist");
        SyncPlan plan = sync.plan(source(List.of(same)), target(List.of(same)));
        RecordingPlaylistWriter writer = new RecordingPlaylistWriter();

        SyncResult result = sync.apply(plan, writer);

        assertTrue(writer.addBatches.isEmpty(), "addTracks should not be called");
        assertTrue(writer.removeBatches.isEmpty(), "removeTracks should not be called");
        assertTrue(result.added().isEmpty());
        assertTrue(result.removed().isEmpty());
    }

    @Test
    void applyPropagatesWriterFailures() {
        Track add = t("New", "Artist");
        Track remove = t("Old", "Artist");
        SyncPlan plan = sync.plan(source(List.of(add)), target(List.of(remove)));
        RecordingPlaylistWriter writer = new RecordingPlaylistWriter();
        writer.addResultOverride = List.of(new AddResult.AddFailed(add, "network down"));
        writer.removeResultOverride = List.of(new RemoveResult.RemoveFailed(remove, "permission denied"));

        SyncResult result = sync.apply(plan, writer);

        AddResult.AddFailed addedFail = assertInstanceOf(AddResult.AddFailed.class, result.added().get(0));
        assertEquals("network down", addedFail.error());
        RemoveResult.RemoveFailed removedFail = assertInstanceOf(RemoveResult.RemoveFailed.class, result.removed().get(0));
        assertEquals("permission denied", removedFail.error());
    }

    @Test
    void applyUsesPlanTargetUriNotTheOriginalPlaylist() {
        Track add = t("New", "Artist");
        SyncPlan plan = new SyncPlan("apple://src", "file:///custom/uri", List.of(add), List.of(), List.of());
        RecordingPlaylistWriter writer = new RecordingPlaylistWriter();

        sync.apply(plan, writer);

        assertEquals(List.of("file:///custom/uri"), writer.addUris);
    }

    // --- Test doubles ---

    private static final class RecordingPlaylistWriter implements PlaylistWriter {
        final List<String> addUris = new ArrayList<>();
        final List<List<Track>> addBatches = new ArrayList<>();
        final List<String> removeUris = new ArrayList<>();
        final List<List<Track>> removeBatches = new ArrayList<>();
        List<AddResult> addResultOverride;
        List<RemoveResult> removeResultOverride;

        @Override
        public List<AddResult> addTracks(String uri, List<Track> tracks) {
            addUris.add(uri);
            addBatches.add(List.copyOf(tracks));
            if (addResultOverride != null) {
                return addResultOverride;
            }
            return tracks.stream()
                .map(track -> (AddResult) new AddResult.Added(track, "fake://" + track.title()))
                .toList();
        }

        @Override
        public List<RemoveResult> removeTracks(String uri, List<Track> tracks) {
            removeUris.add(uri);
            removeBatches.add(List.copyOf(tracks));
            if (removeResultOverride != null) {
                return removeResultOverride;
            }
            return tracks.stream()
                .map(track -> (RemoveResult) new RemoveResult.Removed(track))
                .toList();
        }
    }
}
