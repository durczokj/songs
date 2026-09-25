package com.songs.cli;

import com.songs.http.JdkHttpClient;
import com.songs.matching.ExactTrackMatcher;
import com.songs.provider.youtube.YouTubeAudioProvider;
import com.songs.provider.youtube.YtDlpConfig;
import com.songs.repository.PlaylistRepositoryRegistry;
import com.songs.repository.apple.ApplePlaylistRepository;
import com.songs.repository.local.LocalPlaylistRepository;
import com.songs.repository.local.TagReader;

final class RepositoryFactory {
  private RepositoryFactory() {}

  static PlaylistRepositoryRegistry create(int concurrency) {
    JdkHttpClient httpClient = new JdkHttpClient();
    YouTubeAudioProvider audioProvider = new YouTubeAudioProvider(httpClient, new YtDlpConfig());
    LocalPlaylistRepository local =
        new LocalPlaylistRepository(
            audioProvider, new TagReader(), new ExactTrackMatcher(), concurrency);
    return new PlaylistRepositoryRegistry(new ApplePlaylistRepository(), local);
  }
}
