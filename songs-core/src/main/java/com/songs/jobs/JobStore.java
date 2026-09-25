package com.songs.jobs;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface JobStore {
  void save(SyncJob job) throws IOException;

  Optional<SyncJob> load(String name) throws IOException;

  boolean delete(String name) throws IOException;

  List<SyncJob> list() throws IOException;
}
