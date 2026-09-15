package com.songs.model;

public sealed interface RemoveResult permits RemoveResult.Removed, RemoveResult.RemoveFailed {
    Track track();

    record Removed(Track track) implements RemoveResult {}
    record RemoveFailed(Track track, String error) implements RemoveResult {}
}
