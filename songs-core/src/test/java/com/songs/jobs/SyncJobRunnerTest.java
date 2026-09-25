package com.songs.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.songs.model.AddResult;
import com.songs.model.Playlist;
import com.songs.model.RemoveResult;
import com.songs.model.Track;
import com.songs.repository.PlaylistReader;
import com.songs.repository.PlaylistRepositoryRegistry;
import com.songs.repository.PlaylistWriter;
import com.songs.sync.PlaylistSynchronizer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SyncJobRunnerTest {
  @Test
  void resolvesBothUrisAndRunsTheTargetWriter() throws Exception {
    Track existing = new Track("Existing", "Artist", null, null, null);
    Track added = new Track("Added", "Artist", null, null, null);
    RecordingRepository source =
        new RecordingRepository(new Playlist("source", List.of(existing, added), "source"));
    RecordingRepository target =
        new RecordingRepository(new Playlist("target", List.of(existing), "target"));
    SyncJobRunner runner =
        new SyncJobRunner(
            new PlaylistRepositoryRegistry(source, target), new PlaylistSynchronizer());

    var result = runner.run(new SyncJob("job", "source", "target"));

    assertEquals(List.of("source"), source.uris);
    assertEquals(List.of("target"), target.uris);
    assertEquals(List.of(added), target.added);
    assertEquals(1, result.added().size());
  }

  private static final class RecordingRepository implements PlaylistReader, PlaylistWriter {
    private final Playlist playlist;
    private final List<String> uris = new ArrayList<>();
    private final List<Track> added = new ArrayList<>();

    private RecordingRepository(Playlist playlist) {
      this.playlist = playlist;
    }

    @Override
    public String name() {
      return "recording";
    }

    @Override
    public boolean canHandle(String uri) {
      return uri.equals(playlist.uri());
    }

    @Override
    public Playlist extract(String uri) {
      uris.add(uri);
      return playlist;
    }

    @Override
    public List<AddResult> addTracks(String uri, List<Track> tracks) {
      added.addAll(tracks);
      return tracks.stream()
          .map(track -> (AddResult) new AddResult.Added(track, "recording://" + track.title()))
          .toList();
    }

    @Override
    public List<RemoveResult> removeTracks(String uri, List<Track> tracks) {
      return tracks.stream()
          .map(RemoveResult.Removed::new)
          .map(result -> (RemoveResult) result)
          .toList();
    }
  }
}
