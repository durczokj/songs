package com.songs.model;

public sealed interface TrackResolution permits TrackResolution.Resolved, TrackResolution.Failed {
  Track track();

  record Resolved(Track track, AudioRef ref) implements TrackResolution {}

  record Failed(Track track, String error) implements TrackResolution {}
}
