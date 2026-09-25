package com.songs.jobs;

import com.songs.model.Playlist;
import com.songs.model.SyncPlan;
import com.songs.model.SyncResult;
import com.songs.repository.PlaylistRepositoryRegistry;
import com.songs.sync.PlaylistSynchronizer;
import java.io.IOException;

public final class SyncJobRunner {
  private final PlaylistRepositoryRegistry registry;
  private final PlaylistSynchronizer synchronizer;

  public SyncJobRunner(PlaylistRepositoryRegistry registry, PlaylistSynchronizer synchronizer) {
    this.registry = registry;
    this.synchronizer = synchronizer;
  }

  public SyncPlan preview(SyncJob job) throws IOException {
    Playlist source = registry.readerFor(job.source()).extract(job.source());
    Playlist target = registry.readerFor(job.target()).extract(job.target());
    return synchronizer.plan(source, target);
  }

  public SyncResult run(SyncJob job) throws IOException {
    return synchronizer.apply(preview(job), registry.writerFor(job.target()));
  }
}
