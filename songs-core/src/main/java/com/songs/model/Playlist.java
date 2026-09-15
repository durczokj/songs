package com.songs.model;

import java.util.List;

public record Playlist(String uri, List<Track> tracks, String name) {
    public Playlist {
        tracks = List.copyOf(tracks); // defensive copy → immutable
    }

    @Override
    public String toString() {
        return "Playlist{name=" + (name == null ? "?" : name)
            + ", tracks=" + tracks.size()
            + ", uri=" + uri + "}";
    }
}
