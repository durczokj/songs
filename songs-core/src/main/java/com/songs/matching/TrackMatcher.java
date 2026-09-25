package com.songs.matching;

import com.songs.model.Track;

public interface TrackMatcher {
  boolean matches(Track a, Track b);
}
