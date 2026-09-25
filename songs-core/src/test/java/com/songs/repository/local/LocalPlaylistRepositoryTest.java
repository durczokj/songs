package com.songs.repository.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.songs.model.AddResult;
import com.songs.model.AudioRef;
import com.songs.model.DownloadResult;
import com.songs.model.Playlist;
import com.songs.model.RemoveResult;
import com.songs.model.Track;
import com.songs.model.TrackResolution;
import com.songs.provider.AudioProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalPlaylistRepositoryTest {

  private static Track track(String title, String artist) {
    return new Track(title, artist, null, null, null);
  }

  private static Track trackIsrc(String title, String artist, String isrc) {
    return new Track(title, artist, null, null, isrc);
  }

  private static String fileUri(Path directory) {
    return directory.toUri().toString();
  }

  // --- canHandle ---

  @Test
  void canHandleAcceptsFileUri() {
    LocalPlaylistRepository repo =
        new LocalPlaylistRepository(new FakeAudioProvider(), new TagReader());

    assertTrue(repo.canHandle("file:///tmp/music"));
    assertFalse(repo.canHandle("https://example.com/list"));
    assertFalse(repo.canHandle(null));
  }

  // --- extract ---

  @Test
  void extractReadsTracksViaTagReader(@TempDir Path tempDir) throws IOException {
    Path a = tempDir.resolve("a.mp3");
    Path b = tempDir.resolve("b.mp3");
    Files.createFile(a);
    Files.createFile(b);
    Map<Path, Track> canned = new HashMap<>();
    canned.put(a, track("Song A", "Artist A"));
    canned.put(b, track("Song B", "Artist B"));
    RecordingTagReader tagReader = new RecordingTagReader();
    tagReader.readCanned = canned;

    LocalPlaylistRepository repo = new LocalPlaylistRepository(new FakeAudioProvider(), tagReader);
    Playlist playlist = repo.extract(fileUri(tempDir));

    assertEquals(2, playlist.tracks().size());
    assertEquals("Song A", playlist.tracks().get(0).title());
    assertEquals("Song B", playlist.tracks().get(1).title());
  }

  @Test
  void extractFallsBackToFilenameWhenTagsUnreadable(@TempDir Path tempDir) throws IOException {
    // Real TagReader on an empty file throws; the repository must fall back to parsing the
    // filename.
    Path file = tempDir.resolve("Queen - Bohemian Rhapsody.mp3");
    Files.createFile(file);

    LocalPlaylistRepository repo =
        new LocalPlaylistRepository(new FakeAudioProvider(), new TagReader());
    Playlist playlist = repo.extract(fileUri(tempDir));

    assertEquals(1, playlist.tracks().size());
    Track parsed = playlist.tracks().get(0);
    assertEquals("Bohemian Rhapsody", parsed.title());
    assertEquals("Queen", parsed.artist());
  }

  @Test
  void extractSkipsNonMp3Files(@TempDir Path tempDir) throws IOException {
    Files.createFile(tempDir.resolve("cover.jpg"));
    Files.createFile(tempDir.resolve("notes.txt"));
    Path mp3 = tempDir.resolve("only.mp3");
    Files.createFile(mp3);
    RecordingTagReader tagReader = new RecordingTagReader();
    tagReader.readCanned = Map.of(mp3, track("Only", "Someone"));

    LocalPlaylistRepository repo = new LocalPlaylistRepository(new FakeAudioProvider(), tagReader);
    Playlist playlist = repo.extract(fileUri(tempDir));

    assertEquals(1, playlist.tracks().size());
    assertEquals("Only", playlist.tracks().get(0).title());
  }

  // --- addTracks ---

  @Test
  void addTracksReturnsAlignedAddedAndWritesTags(@TempDir Path tempDir) {
    RecordingTagReader tagReader = new RecordingTagReader();
    FakeAudioProvider provider = new FakeAudioProvider();

    LocalPlaylistRepository repo = new LocalPlaylistRepository(provider, tagReader);
    Track a = track("Song A", "Artist A");
    Track b = track("Song B", "Artist B");
    List<AddResult> results = repo.addTracks(fileUri(tempDir), List.of(a, b));

    assertEquals(2, results.size());
    AddResult.Added first = assertInstanceOf(AddResult.Added.class, results.get(0));
    AddResult.Added second = assertInstanceOf(AddResult.Added.class, results.get(1));
    assertEquals(a, first.track());
    assertEquals(b, second.track());
    // Same tracks were passed to writeTags, one call per download.
    assertEquals(2, tagReader.written.size());
    assertTrue(tagReader.written.containsValue(a));
    assertTrue(tagReader.written.containsValue(b));
  }

  @Test
  void addTracksSanitizesUnsafeCharactersInCanonicalFilename(@TempDir Path tempDir) {
    RecordingTagReader tagReader = new RecordingTagReader();
    LocalPlaylistRepository repo = new LocalPlaylistRepository(new FakeAudioProvider(), tagReader);
    Track track = track("Stereo Mix / 2022", "Artist");

    List<AddResult> results = repo.addTracks(fileUri(tempDir), List.of(track));

    AddResult.Added added = assertInstanceOf(AddResult.Added.class, results.get(0));
    assertFalse(added.addedRef().contains("/Stereo Mix / 2022/"));
    assertTrue(Path.of(added.addedRef()).getFileName().toString().contains("Stereo Mix _ 2022"));
  }

  @Test
  void addTracksMapsDownloadFailureToAddFailed(@TempDir Path tempDir) {
    FakeAudioProvider provider = new FakeAudioProvider();
    Track ok = track("Good", "Artist");
    Track bad = track("Bad", "Artist");
    provider.downloadFailures.put("Bad", "network exploded");
    RecordingTagReader tagReader = new RecordingTagReader();

    LocalPlaylistRepository repo = new LocalPlaylistRepository(provider, tagReader);
    List<AddResult> results = repo.addTracks(fileUri(tempDir), List.of(ok, bad));

    assertInstanceOf(AddResult.Added.class, results.get(0));
    AddResult.AddFailed failed = assertInstanceOf(AddResult.AddFailed.class, results.get(1));
    assertEquals(bad, failed.track());
    assertTrue(failed.error().contains("network exploded"));
    // Only the successful download had tags written.
    assertEquals(1, tagReader.written.size());
  }

  @Test
  void addTracksMapsTagWriteFailureToAddFailed(@TempDir Path tempDir) {
    RecordingTagReader tagReader = new RecordingTagReader();
    tagReader.writesShouldFail = true;

    LocalPlaylistRepository repo = new LocalPlaylistRepository(new FakeAudioProvider(), tagReader);
    Track t = track("Song", "Artist");
    List<AddResult> results = repo.addTracks(fileUri(tempDir), List.of(t));

    AddResult.AddFailed failed = assertInstanceOf(AddResult.AddFailed.class, results.get(0));
    assertTrue(failed.error().contains("Tag write failed"));
  }

  @Test
  void addTracksCreatesTargetDirectoryWhenMissing(@TempDir Path tempDir) {
    Path missing = tempDir.resolve("does/not/exist/yet");

    LocalPlaylistRepository repo =
        new LocalPlaylistRepository(new FakeAudioProvider(), new RecordingTagReader());
    List<AddResult> results = repo.addTracks(fileUri(missing), List.of(track("X", "Y")));

    assertInstanceOf(AddResult.Added.class, results.get(0));
    assertTrue(Files.isDirectory(missing));
  }

  // --- removeTracks ---

  @Test
  void removeTracksDeletesMatchingFilesAndReturnsRemoved(@TempDir Path tempDir) throws IOException {
    Path a = tempDir.resolve("a.mp3");
    Path b = tempDir.resolve("b.mp3");
    Files.createFile(a);
    Files.createFile(b);
    RecordingTagReader tagReader = new RecordingTagReader();
    tagReader.readCanned =
        Map.of(
            a, track("Song A", "Artist A"),
            b, track("Song B", "Artist B"));

    LocalPlaylistRepository repo = new LocalPlaylistRepository(new FakeAudioProvider(), tagReader);
    List<RemoveResult> results =
        repo.removeTracks(fileUri(tempDir), List.of(track("Song A", "Artist A")));

    assertInstanceOf(RemoveResult.Removed.class, results.get(0));
    assertFalse(Files.exists(a));
    assertTrue(Files.exists(b));
  }

  @Test
  void removeTracksMatchesByIsrcEvenWhenTitleDiffers(@TempDir Path tempDir) throws IOException {
    Path file = tempDir.resolve("mystery.mp3");
    Files.createFile(file);
    RecordingTagReader tagReader = new RecordingTagReader();
    tagReader.readCanned =
        Map.of(file, trackIsrc("On-Disk Title", "On-Disk Artist", "USRC17607839"));

    LocalPlaylistRepository repo = new LocalPlaylistRepository(new FakeAudioProvider(), tagReader);
    List<RemoveResult> results =
        repo.removeTracks(
            fileUri(tempDir), List.of(trackIsrc("Wanted Title", "Wanted Artist", "USRC17607839")));

    assertInstanceOf(RemoveResult.Removed.class, results.get(0));
    assertFalse(Files.exists(file));
  }

  @Test
  void removeTracksReturnsRemoveFailedWhenNoMatch(@TempDir Path tempDir) throws IOException {
    Path file = tempDir.resolve("song.mp3");
    Files.createFile(file);
    RecordingTagReader tagReader = new RecordingTagReader();
    tagReader.readCanned = Map.of(file, track("Something Else", "Nobody"));

    LocalPlaylistRepository repo = new LocalPlaylistRepository(new FakeAudioProvider(), tagReader);
    List<RemoveResult> results =
        repo.removeTracks(fileUri(tempDir), List.of(track("Missing", "Ghost")));

    RemoveResult.RemoveFailed failed =
        assertInstanceOf(RemoveResult.RemoveFailed.class, results.get(0));
    assertTrue(failed.error().contains("No matching file"));
    assertTrue(Files.exists(file));
  }

  @Test
  void removeTracksDoesNotDoubleClaimSingleFileForDuplicateTargets(@TempDir Path tempDir)
      throws IOException {
    Path file = tempDir.resolve("only.mp3");
    Files.createFile(file);
    RecordingTagReader tagReader = new RecordingTagReader();
    tagReader.readCanned = Map.of(file, track("Song", "Artist"));

    LocalPlaylistRepository repo = new LocalPlaylistRepository(new FakeAudioProvider(), tagReader);
    List<RemoveResult> results =
        repo.removeTracks(
            fileUri(tempDir), List.of(track("Song", "Artist"), track("Song", "Artist")));

    assertInstanceOf(RemoveResult.Removed.class, results.get(0));
    assertInstanceOf(RemoveResult.RemoveFailed.class, results.get(1));
    assertFalse(Files.exists(file));
  }

  // --- Test doubles ---

  private static final class FakeAudioProvider implements AudioProvider {
    final Map<String, String> downloadFailures = new HashMap<>();
    final List<Track> resolveCalls = new ArrayList<>();
    Function<Track, TrackResolution> resolveOverride;

    @Override
    public String name() {
      return "fake";
    }

    @Override
    public TrackResolution resolveOne(Track track) {
      resolveCalls.add(track);
      if (resolveOverride != null) {
        return resolveOverride.apply(track);
      }
      return new TrackResolution.Resolved(track, new AudioRef("fake", "fake://" + track.title()));
    }

    @Override
    public DownloadResult downloadOne(TrackResolution resolution, Path outputDir) {
      if (resolution instanceof TrackResolution.Failed failed) {
        return new DownloadResult.DownloadFailed(resolution, failed.error());
      }
      Track track = resolution.track();
      String failure = downloadFailures.get(track.title());
      if (failure != null) {
        return new DownloadResult.DownloadFailed(resolution, failure);
      }
      Path outputPath = outputDir.resolve(sanitize(track.title()) + ".mp3");
      try {
        Files.createDirectories(outputDir);
        if (!Files.exists(outputPath)) {
          Files.createFile(outputPath);
        }
      } catch (IOException e) {
        return new DownloadResult.DownloadFailed(resolution, e.getMessage());
      }
      return new DownloadResult.Downloaded(resolution, outputPath);
    }

    private static String sanitize(String s) {
      return s.replaceAll("[^a-zA-Z0-9]+", "_");
    }
  }

  private static final class RecordingTagReader extends TagReader {
    final Map<Path, Track> written = new LinkedHashMap<>();
    Map<Path, Track> readCanned;
    boolean writesShouldFail;

    @Override
    public Track readTags(Path audioPath) throws IOException {
      Track canned = readCanned == null ? null : readCanned.get(audioPath);
      if (canned == null) {
        throw new IOException("no canned track for " + audioPath);
      }
      return canned;
    }

    @Override
    public void writeTags(Path audioPath, Track track) throws IOException {
      if (writesShouldFail) {
        throw new IOException("boom");
      }
      written.put(audioPath, track);
    }
  }
}
