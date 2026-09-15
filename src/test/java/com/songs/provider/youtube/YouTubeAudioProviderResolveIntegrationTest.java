package com.songs.provider.youtube;

import com.songs.http.JdkHttpClient;
import com.songs.model.Track;
import com.songs.model.TrackResolution;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Hits the real network (youtube.com search); excluded from the default `mvn test` run.
// Run with: mvn test -DexcludedGroups=
@Tag("integration")
class YouTubeAudioProviderResolveIntegrationTest {

    @Test
    void resolvesARealTrackAgainstLiveYouTube() {
        YouTubeAudioProvider provider = new YouTubeAudioProvider(new JdkHttpClient(), new YtDlpConfig());
        Track track = new Track("99 Luftballons", "Nena", null, null, null);

        TrackResolution resolution = provider.resolveOne(track);

        TrackResolution.Resolved resolved = assertInstanceOf(TrackResolution.Resolved.class, resolution);
        assertTrue(resolved.ref().url().startsWith("https://www.youtube.com/watch?v="));
        System.out.println("Resolved \"" + track.title() + "\" to " + resolved.ref().url());
    }
}
