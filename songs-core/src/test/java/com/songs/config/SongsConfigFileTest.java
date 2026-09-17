package com.songs.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.songs.jobs.JobSpec;

class SongsConfigFileTest {
    @Test
    void missingFileLoadsEmptyConfig(@TempDir Path tempDir) throws Exception {
        SongsConfig config = new SongsConfigFile(tempDir.resolve("config.json")).load();

        assertTrue(config.jobs().isEmpty());
        assertNull(config.settings().concurrency());
    }

    @Test
    void readsTypedFieldsAndToleratesUnknownOnes(@TempDir Path tempDir) throws Exception {
        Path path = tempDir.resolve("config.json");
        Files.writeString(path, """
            {
              "settings": { "concurrency": 8, "futureSetting": "ignored" },
              "jobs": {
                "favourite": {
                  "source": "apple://source",
                  "target": "file:///target",
                  "futureJobField": "ignored"
                }
              }
            }
            """);

        SongsConfig config = new SongsConfigFile(path).load();

        assertEquals(8, config.settings().concurrency().intValue());
        assertEquals("apple://source", config.jobs().get("favourite").source());
    }

    @Test
    void savedConfigRoundTrips(@TempDir Path tempDir) throws Exception {
        Path path = tempDir.resolve("config.json");
        SongsConfigFile file = new SongsConfigFile(path);
        Map<String, JobSpec> jobs = new LinkedHashMap<>();
        jobs.put("favourite", new JobSpec("apple://source", "file:///target"));
        SongsConfig config = new SongsConfig(new SongsSettings(8), jobs);

        file.save(config);

        assertEquals(config, file.load());
    }
}