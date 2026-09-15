package com.songs.repository.apple;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.songs.model.Playlist;
import com.songs.model.Track;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplePlaylistRepositoryTest {

    private static final String PLAYLIST_URI =
            "https://music.apple.com/pl/playlist/favourite-107950/pl.u-xlyNqGYue3GNz0";

    private final ApplePlaylistRepository repository = new ApplePlaylistRepository();

    @Test
    void canHandleMatchesAppleMusicPlaylistUrls() {
        assertTrue(repository.canHandle(PLAYLIST_URI));
        assertTrue(repository.canHandle("https://music.apple.com/us/playlist/road-trip/pl.abc123"));
        assertTrue(repository.canHandle("https://music.apple.com/gb/playlist/x/pl.y"));
        assertTrue(repository.canHandle("https://music.apple.com/us/playlist/pl.123"));
    }

    @Test
    void canHandleRejectsNonPlaylistUrls() {
        assertFalse(repository.canHandle("https://music.apple.com/pl/album/99-luftballons/1446014467"));
        assertFalse(repository.canHandle("https://open.spotify.com/playlist/abc"));
    }

    @Test
    void toTrackParsesAllMetadataFieldsCorrectly() throws IOException {
        String json = """
                {
                    "id": "1446014714",
                    "type": "songs",
                    "attributes": {
                        "name": "99 Luftballons",
                        "artistName": "Nena",
                        "albumName": "Nena",
                        "durationInMillis": 231000,
                        "isrc": "DEE868300011"
                    }
                }
                """;

        ApplePlaylistPaginator paginator = new ApplePlaylistPaginator();
        Track track = paginator.toTrack(new ObjectMapper().readTree(json));

        assertEquals("99 Luftballons", track.title());
        assertEquals("Nena", track.artist());
        assertEquals("Nena", track.album());
        assertEquals(231, track.durationSec());
        assertEquals("DEE868300011", track.isrc());
    }

    @Test
    void extractUsesTokenProviderAndPaginator() throws IOException {
        AppleMusicTokenProvider fakeTokenProvider = new AppleMusicTokenProvider() {
            @Override
            public String captureToken(String playlistUri) {
                return "Bearer fake-token";
            }
        };

        ApplePlaylistPaginator fakePaginator = new ApplePlaylistPaginator() {
            @Override
            public Playlist fetchPlaylist(String storefront, String playlistId, String bearerToken, String originalUri) {
                assertEquals("pl", storefront);
                assertEquals("pl.u-xlyNqGYue3GNz0", playlistId);
                assertEquals("Bearer fake-token", bearerToken);
                return new Playlist(originalUri, List.of(new Track("Test", "Artist", "Album", 120, "ISRC123")), "Test Playlist");
            }
        };

        ApplePlaylistRepository customRepo = new ApplePlaylistRepository(fakeTokenProvider, fakePaginator);
        Playlist result = customRepo.extract(PLAYLIST_URI);

        assertEquals("Test Playlist", result.name());
        assertEquals(1, result.tracks().size());
        assertEquals("Test", result.tracks().get(0).title());
        assertEquals("ISRC123", result.tracks().get(0).isrc());
    }
}
