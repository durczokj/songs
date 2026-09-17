package com.songs.jobs;

public record SyncJob(String name, String source, String target) {
    public SyncJob {
        requireNonBlank(name, "name");
        if (name.chars().anyMatch(Character::isWhitespace) || name.contains("/")) {
            throw new IllegalArgumentException("Invalid job name: " + name);
        }
        requireNonBlank(source, "source");
        requireNonBlank(target, "target");
    }

    public static SyncJob of(String name, JobSpec spec) {
        return new SyncJob(name, spec.source(), spec.target());
    }

    public JobSpec spec() {
        return new JobSpec(source, target);
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}