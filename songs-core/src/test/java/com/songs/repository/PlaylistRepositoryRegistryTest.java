package com.songs.repository;

import com.songs.model.AddResult;
import com.songs.model.Playlist;
import com.songs.model.RemoveResult;
import com.songs.model.Track;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlaylistRepositoryRegistryTest {

    @Test
    void readerForReturnsFirstRegisteredReaderThatHandlesUri() {
        FakeReader first = new FakeReader("first", "test://");
        FakeReader second = new FakeReader("second", "test://");
        PlaylistRepositoryRegistry registry = new PlaylistRepositoryRegistry();

        registry.register(first);
        registry.register(second);

        assertSame(first, registry.readerFor("test://playlist"));
    }

    @Test
    void writerForReturnsWriterFromRepositoryThatHandlesUri() {
        FakeRepository repository = new FakeRepository("file://");
        PlaylistRepositoryRegistry registry = new PlaylistRepositoryRegistry(repository);

        assertSame(repository, registry.writerFor("file:///music"));
    }

    @Test
    void writerForRejectsReadOnlyRepository() {
        PlaylistRepositoryRegistry registry = new PlaylistRepositoryRegistry();
        registry.register(new FakeReader("apple", "https://music.apple.com/"));

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> registry.writerFor("https://music.apple.com/pl/playlist/test")
        );

        assertEquals(
            "Playlist repository is read-only for URI: https://music.apple.com/pl/playlist/test",
            error.getMessage()
        );
    }

    @Test
    void registerRejectsUnsupportedObject() {
        PlaylistRepositoryRegistry registry = new PlaylistRepositoryRegistry();

        assertThrows(IllegalArgumentException.class, () -> registry.register(new Object()));
    }

    private static class FakeReader implements PlaylistReader {
        private final String name;
        private final String prefix;

        FakeReader(String name, String prefix) {
            this.name = name;
            this.prefix = prefix;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public boolean canHandle(String uri) {
            return uri.startsWith(prefix);
        }

        @Override
        public Playlist extract(String uri) throws IOException {
            return new Playlist(uri, List.of(), name);
        }
    }

    private static final class FakeRepository extends FakeReader implements PlaylistWriter {
        FakeRepository(String prefix) {
            super("writer", prefix);
        }

        @Override
        public List<AddResult> addTracks(String uri, List<Track> tracks) {
            return List.of();
        }

        @Override
        public List<RemoveResult> removeTracks(String uri, List<Track> tracks) {
            return List.of();
        }
    }
}