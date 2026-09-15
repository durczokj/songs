package com.songs.repository;

import com.songs.model.Playlist;

import java.io.IOException;

public interface PlaylistReader {
    String name();
    boolean canHandle(String uri);
    Playlist extract(String uri) throws IOException;
}
