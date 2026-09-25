package com.songs.jobs;

import com.songs.config.SongsConfig;
import com.songs.config.SongsConfigFile;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ConfigFileJobStore implements JobStore {
  private final SongsConfigFile configFile;

  public ConfigFileJobStore() {
    this(new SongsConfigFile());
  }

  public ConfigFileJobStore(SongsConfigFile configFile) {
    this.configFile = configFile;
  }

  @Override
  public void save(SyncJob job) throws IOException {
    SongsConfig config = configFile.load();
    Map<String, JobSpec> jobs = new LinkedHashMap<>(config.jobs());
    jobs.put(job.name(), job.spec());
    configFile.save(new SongsConfig(config.settings(), jobs));
  }

  @Override
  public Optional<SyncJob> load(String name) throws IOException {
    return Optional.ofNullable(configFile.load().jobs().get(name))
        .map(spec -> SyncJob.of(name, spec));
  }

  @Override
  public boolean delete(String name) throws IOException {
    SongsConfig config = configFile.load();
    Map<String, JobSpec> jobs = new LinkedHashMap<>(config.jobs());
    if (jobs.remove(name) == null) {
      return false;
    }
    configFile.save(new SongsConfig(config.settings(), jobs));
    return true;
  }

  @Override
  public List<SyncJob> list() throws IOException {
    return configFile.load().jobs().entrySet().stream()
        .map(entry -> SyncJob.of(entry.getKey(), entry.getValue()))
        .sorted(Comparator.comparing(SyncJob::name))
        .toList();
  }
}
