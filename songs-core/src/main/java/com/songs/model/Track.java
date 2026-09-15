package com.songs.model;

public record Track(
    String title,
    String artist,
    String album,        // nullable
    Integer durationSec, // nullable
    String isrc          // nullable — industry-standard cross-service identifier
) {
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(title == null ? "?" : title).append(" — ").append(artist == null ? "?" : artist);
        if (isrc != null) {
            sb.append(" [").append(isrc).append("]");
        }
        return sb.toString();
    }
}
