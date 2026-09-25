package com.songs.model;

import java.util.List;

public record SyncPlan(
    String sourceUri,
    String targetUri,
    List<Track> toAdd,
    List<Track> toRemove,
    List<Track> unchanged) {
  private static final int SAMPLE_LIMIT = 5;

  public SyncPlan {
    toAdd = List.copyOf(toAdd);
    toRemove = List.copyOf(toRemove);
    unchanged = List.copyOf(unchanged);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("SyncPlan\n");
    sb.append("  source:    ").append(sourceUri).append('\n');
    sb.append("  target:    ").append(targetUri).append('\n');
    sb.append("  unchanged: ").append(unchanged.size()).append('\n');
    sb.append("  toAdd:     ").append(toAdd.size()).append('\n');
    appendSample(sb, toAdd);
    sb.append("  toRemove:  ").append(toRemove.size()).append('\n');
    appendSample(sb, toRemove);
    return sb.toString();
  }

  private static void appendSample(StringBuilder sb, List<Track> tracks) {
    int shown = Math.min(SAMPLE_LIMIT, tracks.size());
    for (int i = 0; i < shown; i++) {
      sb.append("    - ").append(tracks.get(i)).append('\n');
    }
    if (tracks.size() > SAMPLE_LIMIT) {
      sb.append("    ... and ").append(tracks.size() - SAMPLE_LIMIT).append(" more\n");
    }
  }
}
