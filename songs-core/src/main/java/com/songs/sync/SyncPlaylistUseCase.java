package com.songs.sync;

import com.songs.model.Playlist;
import com.songs.model.SyncPlan;
import com.songs.model.SyncResult;
import com.songs.repository.PlaylistReader;
import com.songs.repository.PlaylistWriter;

import java.io.IOException;

public final class SyncPlaylistUseCase {
    private final PlaylistSynchronizer synchronizer;

    public SyncPlaylistUseCase(PlaylistSynchronizer synchronizer) {
        this.synchronizer = synchronizer;
    }

    public SyncPlan preview(
        PlaylistReader sourceReader, String sourceUri,
        PlaylistReader targetReader, String targetUri
    ) throws IOException {
        return synchronizer.plan(
            sourceReader.extract(sourceUri),
            targetReader.extract(targetUri)
        );
    }

    public SyncResult execute(
        PlaylistReader sourceReader, String sourceUri,
        PlaylistReader targetReader, String targetUri,
        PlaylistWriter targetWriter
    ) throws IOException {
        SyncPlan plan = preview(sourceReader, sourceUri, targetReader, targetUri);
        return synchronizer.apply(plan, targetWriter);
    }
}
