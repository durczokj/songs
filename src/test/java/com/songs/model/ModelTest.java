package com.songs.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TrackTest {

    @Test
    void tracksWithSameFieldsAreEqual() {
        Track a = new Track("Title", "Artist", "Album", 200, "ISRC1");
        Track b = new Track("Title", "Artist", "Album", 200, "ISRC1");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void tracksWithDifferentFieldsAreNotEqual() {
        Track a = new Track("Title", "Artist", "Album", 200, "ISRC1");
        Track b = new Track("Title", "Artist", "Album", 200, "ISRC2");

        assertNotEquals(a, b);
    }

    @Test
    void nullableFieldsCanBeNull() {
        Track a = new Track("Title", "Artist", null, null, null);
        Track b = new Track("Title", "Artist", null, null, null);

        assertEquals(a, b);
    }
}

class PlaylistTest {

    @Test
    void mutatingInputListDoesNotAffectPlaylist() {
        List<Track> tracks = new ArrayList<>();
        tracks.add(new Track("Title", "Artist", null, null, null));

        Playlist playlist = new Playlist("file:///music", tracks, "My Playlist");
        tracks.add(new Track("Other", "Artist2", null, null, null));

        assertEquals(1, playlist.tracks().size());
    }

    @Test
    void playlistTracksListIsImmutable() {
        Playlist playlist = new Playlist("file:///music", List.of(), "My Playlist");

        assertEquals(0, playlist.tracks().size());
        try {
            playlist.tracks().add(new Track("Title", "Artist", null, null, null));
            throw new AssertionError("expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // immutable list rejects mutation, as expected
        }
    }

    @Test
    void playlistsWithSameFieldsAreEqual() {
        List<Track> tracks = List.of(new Track("Title", "Artist", null, null, null));

        Playlist a = new Playlist("file:///music", tracks, "My Playlist");
        Playlist b = new Playlist("file:///music", tracks, "My Playlist");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}

class AudioRefTest {

    @Test
    void audioRefsWithSameFieldsAreEqual() {
        AudioRef a = new AudioRef("youtube", "https://youtube.com/watch?v=abc");
        AudioRef b = new AudioRef("youtube", "https://youtube.com/watch?v=abc");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
