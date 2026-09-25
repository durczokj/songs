package com.songs.repository.apple;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.songs.model.Playlist;
import com.songs.model.Track;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

// Hits the real network + launches a real headless browser; excluded from the default
// `mvn test` run (see pom.xml surefire config). Run with: mvn test -Dgroups=integration
@Tag("integration")
class ApplePlaylistRepositoryIntegrationTest {

  private static final String LARGE_PLAYLIST_URI =
      "https://music.apple.com/pl/playlist/favourite-107950/pl.u-xlyNqGYue3GNz0";

  @Test
  void extractsMoreThan300TracksFromARealLargePlaylist() throws Exception {
    ApplePlaylistRepository repository = new ApplePlaylistRepository();

    Playlist playlist = repository.extract(LARGE_PLAYLIST_URI);

    System.out.println(
        "Fetched " + playlist.tracks().size() + " tracks for \"" + playlist.name() + "\"");
    assertTrue(
        playlist.tracks().size() > 300, "expected pagination to fetch beyond the first 300 tracks");

    Track firstTrack = playlist.tracks().get(0);
    assertTrue(
        firstTrack.isrc() != null && !firstTrack.isrc().isBlank(),
        "first track should have isrc populated");
    assertTrue(
        firstTrack.album() != null && !firstTrack.album().isBlank(),
        "first track should have album populated");
  }
}
