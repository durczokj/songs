package com.songs.repository.apple;

import com.songs.model.Playlist;
import com.songs.repository.PlaylistReader;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read-only {@link PlaylistReader} for Apple Music playlists using the catalog API
 * (see {@link AppleMusicTokenProvider}, {@link ApplePlaylistPaginator}).
 */
public class ApplePlaylistRepository implements PlaylistReader {

    private static final Logger logger = LoggerFactory.getLogger(ApplePlaylistRepository.class);
    private static final Pattern PLAYLIST_URI =
            Pattern.compile("music\\.apple\\.com/([^/]+)/playlist/(?:[^/]+/)?([^/?#]+)");

    private final AppleMusicTokenProvider tokenProvider;
    private final ApplePlaylistPaginator paginator;

    public ApplePlaylistRepository() {
        this(new AppleMusicTokenProvider(), new ApplePlaylistPaginator());
    }

    public ApplePlaylistRepository(AppleMusicTokenProvider tokenProvider, ApplePlaylistPaginator paginator) {
        this.tokenProvider = tokenProvider;
        this.paginator = paginator;
    }

    @Override
    public String name() {
        return "apple-music";
    }

    @Override
    public boolean canHandle(String uri) {
        return PLAYLIST_URI.matcher(uri).find();
    }

    @Override
    public Playlist extract(String uri) throws IOException {
        logger.info("Extracting Apple Music playlist from {}", uri);
        Matcher matcher = PLAYLIST_URI.matcher(uri);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid Apple Music playlist URL: " + uri);
        }
        String storefront = matcher.group(1);
        String playlistId = matcher.group(2);

        String bearerToken = tokenProvider.captureToken(uri);
        if (bearerToken == null) {
            logger.warn("Could not capture Apple Music authorization token for {}", uri);
            throw new IOException("Failed to obtain Apple Music authorization token for: " + uri);
        }

        Playlist playlist = paginator.fetchPlaylist(storefront, playlistId, bearerToken, uri);
        logger.info("Extracted {} tracks from Apple Music playlist {}", playlist.tracks().size(), playlist.name());
        return playlist;
    }
}
