package com.songs.config;

import com.songs.jobs.JobSpec;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record SongsConfig(SongsSettings settings, Map<String, JobSpec> jobs) {
  public SongsConfig {
    settings = settings == null ? new SongsSettings(null) : settings;
    jobs = jobs == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(jobs));
  }

  public SongsConfig() {
    this(null, null);
  }
}
