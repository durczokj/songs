package com.songs.repository;

import com.songs.model.AddResult;
import com.songs.model.RemoveResult;
import com.songs.model.Track;

import java.util.List;

public interface PlaylistWriter {
    List<AddResult> addTracks(String uri, List<Track> tracks);
    List<RemoveResult> removeTracks(String uri, List<Track> tracks);
}
