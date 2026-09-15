package com.songs.sync;

import com.songs.model.AddResult;
import com.songs.model.Playlist;
import com.songs.model.RemoveResult;
import com.songs.model.Track;
import com.songs.repository.PlaylistReader;
import com.songs.repository.PlaylistWriter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyncPlaylistUseCaseTest {
    private static final Track SAME = new Track("Same", "Artist", null, null, null);
    private static final Track NEW = new Track("New", "Artist", null, null, null);

    @Test
    void previewLoadsBothPlaylistsAndPlansWithoutWriting() throws IOException {
        RecordingReader source = new RecordingReader(new Playlist("source", List.of(SAME, NEW), "source"));
        RecordingReader target = new RecordingReader(new Playlist("target", List.of(SAME), "target"));
        RecordingWriter writer = new RecordingWriter();
        SyncPlaylistUseCase useCase = new SyncPlaylistUseCase(new PlaylistSynchronizer());

        var plan = useCase.preview(source, "source", target, "target");

        assertEquals(List.of("source"), source.uris);
        assertEquals(List.of("target"), target.uris);
        assertEquals(List.of(NEW), plan.toAdd());
        assertEquals(0, writer.addCalls);
    }

    @Test
    void executeSharesPlanningAndAppliesToWriter() throws IOException {
        RecordingReader source = new RecordingReader(new Playlist("source", List.of(SAME, NEW), "source"));
        RecordingReader target = new RecordingReader(new Playlist("target", List.of(SAME), "target"));
        RecordingWriter writer = new RecordingWriter();
        SyncPlaylistUseCase useCase = new SyncPlaylistUseCase(new PlaylistSynchronizer());

        var result = useCase.execute(source, "source", target, "target", writer);

        assertEquals(List.of(NEW), writer.added);
        assertEquals(1, result.added().size());
    }

    private static final class RecordingReader implements PlaylistReader {
        private final Playlist playlist;
        private final List<String> uris = new java.util.ArrayList<>();

        private RecordingReader(Playlist playlist) {
            this.playlist = playlist;
        }

        @Override
        public String name() {
            return "recording";
        }

        @Override
        public boolean canHandle(String uri) {
            return true;
        }

        @Override
        public Playlist extract(String uri) {
            uris.add(uri);
            return playlist;
        }
    }

    private static final class RecordingWriter implements PlaylistWriter {
        private final List<Track> added = new java.util.ArrayList<>();
        private int addCalls;

        @Override
        public List<AddResult> addTracks(String uri, List<Track> tracks) {
            addCalls++;
            added.addAll(tracks);
            return tracks.stream().map(track -> (AddResult) new AddResult.Added(track, "fake://" + track.title())).toList();
        }

        @Override
        public List<RemoveResult> removeTracks(String uri, List<Track> tracks) {
            return tracks.stream()
                .map(track -> (RemoveResult) new RemoveResult.Removed(track))
                .toList();
        }
    }
}
