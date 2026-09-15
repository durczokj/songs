package com.songs.repository.local;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TagReaderTest {

    @Test
    void readTagsRejectsMissingFile() {
        TagReader tagReader = new TagReader();

        assertThrows(Exception.class, () -> tagReader.readTags(Path.of("does-not-exist.mp3")));
    }
}
