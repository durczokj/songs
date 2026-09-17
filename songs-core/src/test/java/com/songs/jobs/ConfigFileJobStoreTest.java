package com.songs.jobs;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.songs.config.SongsConfigFile;

class ConfigFileJobStoreTest {
    @Test
    void managesJobsInOneConfigFile(@TempDir Path tempDir) throws Exception {
        Path path = tempDir.resolve("config.json");
        ObjectMapper mapper = new ObjectMapper();
        var root = mapper.createObjectNode();
        root.putObject("settings").put("concurrency", 2);
        mapper.writeValue(path.toFile(), root);
        ConfigFileJobStore store = new ConfigFileJobStore(new SongsConfigFile(path));

        store.save(new SyncJob("favourite", "apple://source", "file:///target"));
        assertTrue(store.load("favourite").isPresent());
        assertEquals(2, new SongsConfigFile(path).load().settings().concurrency().intValue());
        assertEquals(1, store.list().size());
        assertTrue(store.delete("favourite"));
        assertFalse(store.delete("favourite"));
    }
}