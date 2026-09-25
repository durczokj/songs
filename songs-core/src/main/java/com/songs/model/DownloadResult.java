package com.songs.model;

import java.nio.file.Path;

public sealed interface DownloadResult
    permits DownloadResult.Downloaded, DownloadResult.DownloadFailed {
  TrackResolution resolution();

  record Downloaded(TrackResolution resolution, Path outputPath) implements DownloadResult {}

  record DownloadFailed(TrackResolution resolution, String error) implements DownloadResult {}
}
