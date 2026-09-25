package com.songs.repository;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * URI-based lookup for registered playlist repositories.
 *
 * <p>A writer must also be a reader because {@link PlaylistWriter} deliberately has no URI
 * capability method. This keeps the read/write ports separate while still allowing repositories
 * such as the local repository to support both.
 */
public final class PlaylistRepositoryRegistry {

  private static final Logger logger = LoggerFactory.getLogger(PlaylistRepositoryRegistry.class);
  private final List<PlaylistReader> readers = new ArrayList<>();
  private final List<PlaylistWriter> writers = new ArrayList<>();

  public PlaylistRepositoryRegistry(Object... repositories) {
    for (Object repository : repositories) {
      register(repository);
    }
  }

  public void register(Object repository) {
    if (repository instanceof PlaylistReader reader) {
      readers.add(reader);
      logger.debug("Registered playlist reader {}", reader.name());
    }
    if (repository instanceof PlaylistWriter writer) {
      if (!(repository instanceof PlaylistReader)) {
        throw new IllegalArgumentException(
            "A URI-dispatched writer must also implement PlaylistReader: "
                + repository.getClass().getName());
      }
      writers.add(writer);
      logger.debug("Registered playlist writer {}", repository.getClass().getSimpleName());
    }
    if (!(repository instanceof PlaylistReader) && !(repository instanceof PlaylistWriter)) {
      throw new IllegalArgumentException(
          "Repository must implement PlaylistReader or PlaylistWriter");
    }
  }

  public PlaylistReader readerFor(String uri) {
    PlaylistReader selectedReader =
        readers.stream()
            .filter(candidate -> candidate.canHandle(uri))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException("No playlist reader handles URI: " + uri));
    logger.info("Selected playlist reader {} for {}", selectedReader.name(), uri);
    return selectedReader;
  }

  public PlaylistWriter writerFor(String uri) {
    for (PlaylistWriter writer : writers) {
      PlaylistReader reader = (PlaylistReader) writer;
      if (reader.canHandle(uri)) {
        logger.info("Selected playlist writer {} for {}", reader.name(), uri);
        return writer;
      }
    }
    if (readers.stream().anyMatch(reader -> reader.canHandle(uri))) {
      logger.warn("Playlist URI {} is handled by a read-only repository", uri);
      throw new IllegalArgumentException("Playlist repository is read-only for URI: " + uri);
    }
    throw new IllegalArgumentException("No playlist writer handles URI: " + uri);
  }
}
