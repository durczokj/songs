package com.songs.model;

public sealed interface AddResult permits AddResult.Added, AddResult.AddFailed {
  Track track();

  record Added(Track track, String addedRef) implements AddResult {}

  record AddFailed(Track track, String error) implements AddResult {}
}
