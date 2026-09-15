package com.songs.model;

import java.util.List;

public record SyncResult(SyncPlan plan, List<AddResult> added, List<RemoveResult> removed) {
    private static final int FAILURE_LIMIT = 5;

    public SyncResult {
        added = List.copyOf(added);
        removed = List.copyOf(removed);
    }

    @Override
    public String toString() {
        long addedOk = added.stream().filter(AddResult.Added.class::isInstance).count();
        long addedFail = added.size() - addedOk;
        long removedOk = removed.stream().filter(RemoveResult.Removed.class::isInstance).count();
        long removedFail = removed.size() - removedOk;

        StringBuilder sb = new StringBuilder();
        sb.append("SyncResult\n");
        sb.append("  added:   ").append(addedOk).append(" ok, ").append(addedFail).append(" failed\n");
        appendAddFailures(sb, added);
        sb.append("  removed: ").append(removedOk).append(" ok, ").append(removedFail).append(" failed\n");
        appendRemoveFailures(sb, removed);
        return sb.toString();
    }

    private static void appendAddFailures(StringBuilder sb, List<AddResult> results) {
        List<AddResult.AddFailed> failures = results.stream()
            .filter(AddResult.AddFailed.class::isInstance)
            .map(AddResult.AddFailed.class::cast)
            .toList();
        appendFailures(sb, failures.size(),
            i -> failures.get(i).track() + " — " + failures.get(i).error());
    }

    private static void appendRemoveFailures(StringBuilder sb, List<RemoveResult> results) {
        List<RemoveResult.RemoveFailed> failures = results.stream()
            .filter(RemoveResult.RemoveFailed.class::isInstance)
            .map(RemoveResult.RemoveFailed.class::cast)
            .toList();
        appendFailures(sb, failures.size(),
            i -> failures.get(i).track() + " — " + failures.get(i).error());
    }

    private static void appendFailures(StringBuilder sb, int count, java.util.function.IntFunction<String> line) {
        int shown = Math.min(FAILURE_LIMIT, count);
        for (int i = 0; i < shown; i++) {
            sb.append("    - ").append(line.apply(i)).append('\n');
        }
        if (count > FAILURE_LIMIT) {
            sb.append("    ... and ").append(count - FAILURE_LIMIT).append(" more\n");
        }
    }
}
