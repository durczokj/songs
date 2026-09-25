package com.songs.repository.local;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TagReaderTest {

  @Test
  void readTagsRejectsMissingFile() {
    TagReader tagReader = new TagReader();

    assertThrows(Exception.class, () -> tagReader.readTags(Path.of("does-not-exist.mp3")));
  }
}
