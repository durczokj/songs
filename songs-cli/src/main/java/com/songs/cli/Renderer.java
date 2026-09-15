package com.songs.cli;

import com.songs.model.AddResult;
import com.songs.model.RemoveResult;
import com.songs.model.SyncPlan;
import com.songs.model.SyncResult;

import java.io.PrintWriter;

final class Renderer {
    private Renderer() {
    }

    static void plan(SyncPlan plan, PrintWriter out) {
        out.printf("Plan %s -> %s%n", plan.sourceUri(), plan.targetUri());
        out.printf("  unchanged %d  add %d  remove %d%n",
            plan.unchanged().size(), plan.toAdd().size(), plan.toRemove().size());
        printTracks(out, "To add", plan.toAdd());
        printTracks(out, "To remove", plan.toRemove());
    }

    static void result(SyncResult result, PrintWriter out) {
        long added = result.added().stream().filter(AddResult.Added.class::isInstance).count();
        long removed = result.removed().stream().filter(RemoveResult.Removed.class::isInstance).count();
        out.printf("Added %d/%d%n", added, result.added().size());
        result.added().stream()
            .filter(AddResult.AddFailed.class::isInstance)
            .map(AddResult.AddFailed.class::cast)
            .forEach(failure -> out.println("  ! " + failure.track() + " - " + failure.error()));
        out.printf("Removed %d/%d%n", removed, result.removed().size());
        result.removed().stream()
            .filter(RemoveResult.RemoveFailed.class::isInstance)
            .map(RemoveResult.RemoveFailed.class::cast)
            .forEach(failure -> out.println("  ! " + failure.track() + " - " + failure.error()));
    }

    private static void printTracks(PrintWriter out, String heading, java.util.List<com.songs.model.Track> tracks) {
        if (tracks.isEmpty()) {
            return;
        }
        out.println(heading + ":");
        tracks.forEach(track -> out.println("  - " + track));
    }
}
