package com.songs.sync;

import com.songs.matching.ExactTrackMatcher;
import com.songs.matching.TrackMatcher;
import com.songs.model.AddResult;
import com.songs.model.Playlist;
import com.songs.model.RemoveResult;
import com.songs.model.SyncPlan;
import com.songs.model.SyncResult;
import com.songs.model.Track;
import com.songs.repository.PlaylistWriter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PlaylistSynchronizer {

  private static final Logger logger = LoggerFactory.getLogger(PlaylistSynchronizer.class);
  private final TrackMatcher matcher;

  public PlaylistSynchronizer(TrackMatcher matcher) {
    this.matcher = matcher;
  }

  public PlaylistSynchronizer() {
    this(new ExactTrackMatcher());
  }

  /** Pure. No I/O. */
  public SyncPlan plan(Playlist source, Playlist target) {
    logger.info("Planning sync from {} to {}", source.uri(), target.uri());
    List<Track> sourceTracks = deduplicate(source.tracks());
    List<Track> targetTracks = target.tracks();

    // Greedy: each source track claims the first unclaimed target match.
    boolean[] claimed = new boolean[targetTracks.size()];
    List<Track> toAdd = new ArrayList<>();
    List<Track> unchanged = new ArrayList<>();

    for (Track s : sourceTracks) {
      int matchIdx = findFirstUnclaimedMatch(s, targetTracks, claimed);
      if (matchIdx >= 0) {
        claimed[matchIdx] = true;
        unchanged.add(s);
      } else {
        toAdd.add(s);
      }
    }

    List<Track> toRemove = new ArrayList<>();
    for (int i = 0; i < targetTracks.size(); i++) {
      if (!claimed[i]) {
        toRemove.add(targetTracks.get(i));
      }
    }

    SyncPlan plan = new SyncPlan(source.uri(), target.uri(), toAdd, toRemove, unchanged);
    logger.info(
        "Sync plan ready: {} unchanged, {} to add, {} to remove",
        unchanged.size(),
        toAdd.size(),
        toRemove.size());
    logger.debug("Sync plan details: {}", plan);
    return plan;
  }

  private List<Track> deduplicate(List<Track> tracks) {
    List<Track> unique = new ArrayList<>();
    for (Track track : tracks) {
      if (unique.stream().noneMatch(existing -> matcher.matches(track, existing))) {
        unique.add(track);
      }
    }
    return unique;
  }

  /** Effectful. Delegates to the writer; the compiler enforces write-safety of the target. */
  public SyncResult apply(SyncPlan plan, PlaylistWriter target) {
    logger.info(
        "Applying sync plan to {}: {} additions, {} removals",
        plan.targetUri(),
        plan.toAdd().size(),
        plan.toRemove().size());
    List<AddResult> added =
        plan.toAdd().isEmpty() ? List.of() : target.addTracks(plan.targetUri(), plan.toAdd());
    List<RemoveResult> removed =
        plan.toRemove().isEmpty()
            ? List.of()
            : target.removeTracks(plan.targetUri(), plan.toRemove());
    SyncResult result = new SyncResult(plan, added, removed);
    long addFailures = added.stream().filter(item -> item instanceof AddResult.AddFailed).count();
    long removeFailures =
        removed.stream().filter(item -> item instanceof RemoveResult.RemoveFailed).count();
    if (addFailures > 0 || removeFailures > 0) {
      logger.warn(
          "Sync completed with {} add failures and {} remove failures",
          addFailures,
          removeFailures);
    } else {
      logger.info("Sync completed successfully");
    }
    logger.debug("Sync result: {}", result);
    return result;
  }

  private int findFirstUnclaimedMatch(Track s, List<Track> targetTracks, boolean[] claimed) {
    for (int i = 0; i < targetTracks.size(); i++) {
      if (!claimed[i] && matcher.matches(s, targetTracks.get(i))) {
        return i;
      }
    }
    return -1;
  }
}
