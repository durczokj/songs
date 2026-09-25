package com.songs.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SyncJobTest {
  @Test
  void convertsToAndFromTheStoredSpec() {
    SyncJob job = new SyncJob("favourite", "apple://source", "file:///target");

    assertEquals(job, SyncJob.of("favourite", job.spec()));
  }

  @Test
  void rejectsInvalidNamesAndMissingUris() {
    assertThrows(IllegalArgumentException.class, () -> new SyncJob("has space", "a", "b"));
    assertThrows(IllegalArgumentException.class, () -> new SyncJob("nested/name", "a", "b"));
    assertThrows(IllegalArgumentException.class, () -> new SyncJob("job", "", "b"));
    assertThrows(IllegalArgumentException.class, () -> new SyncJob("job", "a", null));
  }
}
