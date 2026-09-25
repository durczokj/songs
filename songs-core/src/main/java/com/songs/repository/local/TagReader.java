package com.songs.repository.local;

import com.songs.model.Track;
import java.io.IOException;
import java.nio.file.Path;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TagReader {

  private static final Logger logger = LoggerFactory.getLogger(TagReader.class);

  public Track readTags(Path audioPath) throws IOException {
    logger.debug("Reading tags from {}", audioPath);
    try {
      AudioFile audioFile = AudioFileIO.read(audioPath.toFile());
      Tag tag = audioFile.getTag();
      if (tag == null) {
        throw new IOException("No tags found in " + audioPath);
      }
      return new Track(
          valueOrNull(tag.getFirst(FieldKey.TITLE)),
          valueOrNull(tag.getFirst(FieldKey.ARTIST)),
          valueOrNull(tag.getFirst(FieldKey.ALBUM)),
          durationSeconds(audioFile),
          valueOrNull(tag.getFirst(FieldKey.ISRC)));
    } catch (Exception e) {
      logger.warn("Could not read tags from {}: {}", audioPath, e.getMessage());
      throw asIOException("Could not read tags from " + audioPath, e);
    }
  }

  public void writeTags(Path audioPath, Track track) throws IOException {
    logger.debug("Writing tags to {}", audioPath);
    try {
      AudioFile audioFile = AudioFileIO.read(audioPath.toFile());
      Tag tag = audioFile.getTagOrCreateAndSetDefault();
      setOrDelete(tag, FieldKey.TITLE, track.title());
      setOrDelete(tag, FieldKey.ARTIST, track.artist());
      setOrDelete(tag, FieldKey.ALBUM, track.album());
      setOrDelete(tag, FieldKey.ISRC, track.isrc());
      audioFile.commit();
    } catch (Exception e) {
      logger.warn("Could not write tags to {}: {}", audioPath, e.getMessage());
      throw asIOException("Could not write tags to " + audioPath, e);
    }
  }

  private static Integer durationSeconds(AudioFile audioFile) {
    int milliseconds = audioFile.getAudioHeader().getTrackLength() * 1000;
    return milliseconds == 0 ? null : milliseconds / 1000;
  }

  private static String valueOrNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private static void setOrDelete(Tag tag, FieldKey key, String value) throws Exception {
    tag.deleteField(key);
    if (value != null && !value.isBlank()) {
      tag.setField(key, value);
    }
  }

  private static IOException asIOException(String message, Exception cause) {
    if (cause instanceof IOException ioException) {
      return ioException;
    }
    return new IOException(message, cause);
  }
}
