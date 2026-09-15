package com.songs.provider;

import com.songs.concurrency.Concurrency;
import com.songs.model.DownloadResult;
import com.songs.model.Track;
import com.songs.model.TrackResolution;

import java.nio.file.Path;
import java.util.List;

public interface AudioProvider {
    String name();
    TrackResolution resolveOne(Track track);
    DownloadResult downloadOne(TrackResolution resolution, Path outputDir);

    // default methods share fan-out logic; concrete providers override only if needed
    default List<TrackResolution> resolveMany(List<Track> tracks, int concurrency) {
        return Concurrency.parallelMap(tracks, this::resolveOne, concurrency);
    }

    default List<DownloadResult> downloadMany(
        List<TrackResolution> resolutions, Path outputDir, int concurrency
    ) {
        return Concurrency.parallelMap(
            resolutions, r -> downloadOne(r, outputDir), concurrency
        );
    }
}
