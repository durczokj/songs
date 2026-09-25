package com.songs.repository.local;

import com.songs.matching.ExactTrackMatcher;
import com.songs.matching.TrackMatcher;
import com.songs.model.AddResult;
import com.songs.model.DownloadResult;
import com.songs.model.Playlist;
import com.songs.model.RemoveResult;
import com.songs.model.Track;
import com.songs.model.TrackResolution;
import com.songs.provider.AudioProvider;
import com.songs.repository.PlaylistReader;
import com.songs.repository.PlaylistWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LocalPlaylistRepository implements PlaylistReader, PlaylistWriter {

  private static final int DEFAULT_CONCURRENCY = 4;
  private static final Logger logger = LoggerFactory.getLogger(LocalPlaylistRepository.class);

  private final AudioProvider audioProvider;
  private final TagReader tagReader;
  private final TrackMatcher matcher;
  private final int concurrency;

  public LocalPlaylistRepository(AudioProvider audioProvider, TagReader tagReader) {
    this(audioProvider, tagReader, new ExactTrackMatcher(), DEFAULT_CONCURRENCY);
  }

  public LocalPlaylistRepository(
      AudioProvider audioProvider, TagReader tagReader, TrackMatcher matcher, int concurrency) {
    if (concurrency < 1) {
      throw new IllegalArgumentException("concurrency must be >= 1");
    }
    this.audioProvider = audioProvider;
    this.tagReader = tagReader;
    this.matcher = matcher;
    this.concurrency = concurrency;
  }

  @Override
  public String name() {
    return "local";
  }

  @Override
  public boolean canHandle(String uri) {
    return uri != null && uri.startsWith("file://");
  }

  @Override
  public Playlist extract(String uri) throws IOException {
    logger.info("Extracting local playlist from {}", uri);
    Path directory = toDirectory(uri);
    List<Path> mp3s = listMp3Files(directory);
    List<Track> tracks = new ArrayList<>(mp3s.size());
    for (Path mp3 : mp3s) {
      Track track = readTrackWithFallback(mp3);
      tracks.add(track);
      logger.debug("Extracted local track: {}", track);
    }
    Playlist playlist =
        new Playlist(
            uri,
            tracks,
            directory.getFileName() == null ? null : directory.getFileName().toString());
    logger.info("Extracted {} tracks from {}", tracks.size(), directory);
    return playlist;
  }

  @Override
  public List<AddResult> addTracks(String uri, List<Track> tracks) {
    logger.info("Adding {} tracks to local playlist {}", tracks.size(), uri);
    Path directory;
    try {
      directory = toDirectory(uri, true);
    } catch (IOException e) {
      logger.warn("Could not prepare local target {}: {}", uri, e.getMessage());
      return tracks.stream()
          .map(
              t ->
                  (AddResult)
                      new AddResult.AddFailed(t, "Invalid target directory: " + e.getMessage()))
          .toList();
    }

    List<TrackResolution> resolutions = audioProvider.resolveMany(tracks, concurrency);
    List<DownloadResult> downloads =
        audioProvider.downloadMany(resolutions, directory, concurrency);

    List<AddResult> results = new ArrayList<>(tracks.size());
    for (int i = 0; i < tracks.size(); i++) {
      Track track = tracks.get(i);
      DownloadResult download = downloads.get(i);
      results.add(toAddResult(track, download));
    }
    long failures = results.stream().filter(item -> item instanceof AddResult.AddFailed).count();
    logger.info(
        "Finished adding tracks to {}: {} successes, {} failures",
        directory,
        tracks.size() - failures,
        failures);
    return List.copyOf(results);
  }

  @Override
  public List<RemoveResult> removeTracks(String uri, List<Track> tracks) {
    logger.info("Removing {} tracks from local playlist {}", tracks.size(), uri);
    Path directory;
    try {
      directory = toDirectory(uri);
    } catch (IOException e) {
      logger.warn("Could not access local target {}: {}", uri, e.getMessage());
      return tracks.stream()
          .map(
              t ->
                  (RemoveResult)
                      new RemoveResult.RemoveFailed(
                          t, "Invalid target directory: " + e.getMessage()))
          .toList();
    }

    List<FileEntry> entries;
    try {
      entries = loadFileEntries(directory);
    } catch (IOException e) {
      logger.warn("Could not list local target {}: {}", directory, e.getMessage());
      return tracks.stream()
          .map(
              t ->
                  (RemoveResult)
                      new RemoveResult.RemoveFailed(
                          t, "Could not list directory: " + e.getMessage()))
          .toList();
    }

    List<RemoveResult> results = new ArrayList<>(tracks.size());
    for (Track track : tracks) {
      results.add(removeOne(track, entries));
    }
    long failures =
        results.stream().filter(item -> item instanceof RemoveResult.RemoveFailed).count();
    logger.info(
        "Finished removing tracks from {}: {} successes, {} failures",
        directory,
        tracks.size() - failures,
        failures);
    return List.copyOf(results);
  }

  private AddResult toAddResult(Track track, DownloadResult download) {
    return switch (download) {
      case DownloadResult.DownloadFailed failed -> new AddResult.AddFailed(track, failed.error());
      case DownloadResult.Downloaded downloaded -> {
        try {
          Path outputPath = canonicalPath(downloaded.outputPath(), track);
          tagReader.writeTags(downloaded.outputPath(), track);
          if (!outputPath.equals(downloaded.outputPath())) {
            Files.move(downloaded.outputPath(), outputPath, StandardCopyOption.REPLACE_EXISTING);
          }
          yield new AddResult.Added(track, outputPath.toString());
        } catch (IOException e) {
          logger.warn("Could not write tags for {}: {}", downloaded.outputPath(), e.getMessage());
          yield new AddResult.AddFailed(track, "Tag write failed: " + e.getMessage());
        }
      }
    };
  }

  private static Path canonicalPath(Path downloadedPath, Track track) {
    String fileName =
        track
            .toString()
            .replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_")
            .replaceAll("[ .]+$", "")
            .trim();
    if (fileName.isEmpty()) {
      fileName = "untitled";
    }
    return downloadedPath.resolveSibling(fileName + ".mp3");
  }

  private RemoveResult removeOne(Track track, List<FileEntry> entries) {
    FileEntry match = null;
    for (FileEntry entry : entries) {
      if (entry.claimed) continue;
      if (matcher.matches(track, entry.track)) {
        match = entry;
        break;
      }
    }
    if (match == null) {
      logger.warn("No local file matched track {}", track);
      return new RemoveResult.RemoveFailed(track, "No matching file found");
    }
    try {
      Files.delete(match.path);
      match.claimed = true;
      return new RemoveResult.Removed(track);
    } catch (IOException e) {
      logger.warn("Could not delete {}: {}", match.path, e.getMessage());
      return new RemoveResult.RemoveFailed(track, "Delete failed: " + e.getMessage());
    }
  }

  private List<FileEntry> loadFileEntries(Path directory) throws IOException {
    List<Path> mp3s = listMp3Files(directory);
    List<FileEntry> entries = new ArrayList<>(mp3s.size());
    for (Path mp3 : mp3s) {
      entries.add(new FileEntry(mp3, readTrackWithFallback(mp3)));
    }
    return entries;
  }

  private Track readTrackWithFallback(Path mp3) {
    try {
      return tagReader.readTags(mp3);
    } catch (IOException e) {
      logger.debug("Could not read tags from {}; using filename fallback", mp3);
      return trackFromFilename(mp3);
    }
  }

  // Parses "Artist - Title.mp3" if a " - " separator is present; otherwise treats the stem as the
  // title.
  private static Track trackFromFilename(Path mp3) {
    String fileName = mp3.getFileName().toString();
    String stem =
        fileName.toLowerCase().endsWith(".mp3")
            ? fileName.substring(0, fileName.length() - 4)
            : fileName;
    int sep = stem.indexOf(" - ");
    if (sep > 0) {
      String artist = stem.substring(0, sep).trim();
      String title = stem.substring(sep + 3).trim();
      return new Track(title, artist, null, null, null);
    }
    return new Track(stem.trim(), "", null, null, null);
  }

  private static List<Path> listMp3Files(Path directory) throws IOException {
    List<Path> mp3s = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.mp3")) {
      for (Path path : stream) {
        if (Files.isRegularFile(path)) {
          mp3s.add(path);
        }
      }
    }
    mp3s.sort(Comparator.comparing(p -> p.getFileName().toString()));
    return mp3s;
  }

  private static Path toDirectory(String uri) throws IOException {
    return toDirectory(uri, false);
  }

  private static Path toDirectory(String uri, boolean createIfMissing) throws IOException {
    if (uri == null || !uri.startsWith("file://")) {
      throw new IOException("Not a file:// URI: " + uri);
    }
    Path path;
    try {
      path = Path.of(URI.create(uri));
    } catch (IllegalArgumentException e) {
      throw new IOException("Invalid file:// URI: " + uri, e);
    }
    if (createIfMissing) {
      Files.createDirectories(path);
    } else if (!Files.isDirectory(path)) {
      throw new IOException("Not a directory: " + path);
    }
    return path;
  }

  // Mutable local record; only lives inside removeTracks.
  private static final class FileEntry {
    final Path path;
    final Track track;
    boolean claimed;

    FileEntry(Path path, Track track) {
      this.path = path;
      this.track = track;
    }
  }
}
