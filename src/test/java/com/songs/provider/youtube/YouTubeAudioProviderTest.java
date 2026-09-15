package com.songs.provider.youtube;

import com.songs.http.HttpClient;
import com.songs.model.Track;
import com.songs.model.TrackResolution;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YouTubeAudioProviderTest {

    @Test
    void buildSearchQueryFormatsTitleArtistAndKeywords() {
        Track track = new Track("99 Luftballons", "Nena", null, null, null);

        String query = YouTubeAudioProvider.buildSearchQuery(track);

        assertEquals("99 Luftballons Nena official audio", query);
    }

    @Test
    void extractFirstWatchUrlFindsVideoIdInJsonResponse() throws IOException {
        String json = readFixture();

        String videoUrl = YouTubeAudioProvider.extractFirstWatchUrl(json);

        assertEquals("https://www.youtube.com/watch?v=TMSlTQmDLx4", videoUrl);
    }

    @Test
    void extractFirstWatchUrlReturnsNullWhenNoMatch() {
        assertNull(YouTubeAudioProvider.extractFirstWatchUrl("{\"contents\":{}}"));
        assertNull(YouTubeAudioProvider.extractFirstWatchUrl(""));
        assertNull(YouTubeAudioProvider.extractFirstWatchUrl(null));
    }

    @Test
    void resolveOneReturnsResolvedUsingAFakeHttpClient() throws IOException {
        String json = readFixture();
        HttpClient fakeClient = new FakeHttpClient(json);
        YouTubeAudioProvider provider = new YouTubeAudioProvider(fakeClient, new YtDlpConfig());
        Track track = new Track("99 Luftballons", "Nena", null, null, null);

        TrackResolution resolution = provider.resolveOne(track);

        TrackResolution.Resolved resolved = assertInstanceOf(TrackResolution.Resolved.class, resolution);
        assertEquals(track, resolved.track());
        assertEquals("https://www.youtube.com/watch?v=TMSlTQmDLx4", resolved.ref().url());
        assertEquals("youtube", resolved.ref().provider());
    }

    @Test
    void resolveOneReturnsFailedWhenNoVideoFound() {
        HttpClient fakeClient = new FakeHttpClient("{\"contents\":{}}");
        YouTubeAudioProvider provider = new YouTubeAudioProvider(fakeClient, new YtDlpConfig());
        Track track = new Track("Nonexistent Song", "Nobody", null, null, null);

        TrackResolution resolution = provider.resolveOne(track);

        TrackResolution.Failed failed = assertInstanceOf(TrackResolution.Failed.class, resolution);
        assertEquals("No YouTube video found", failed.error());
    }

    @Test
    void resolveOneReturnsFailedWhenHttpClientThrows() {
        HttpClient fakeClient = new HttpClient() {
            @Override
            public String get(String uri, Duration timeout) throws IOException {
                throw new IOException("network down");
            }

            @Override
            public String post(String uri, String jsonBody, Duration timeout) throws IOException {
                throw new IOException("network down");
            }
        };
        YouTubeAudioProvider provider = new YouTubeAudioProvider(fakeClient, new YtDlpConfig());
        Track track = new Track("Title", "Artist", null, null, null);

        TrackResolution resolution = provider.resolveOne(track);

        TrackResolution.Failed failed = assertInstanceOf(TrackResolution.Failed.class, resolution);
        assertTrue(failed.error().contains("network down"));
    }

    @Test
    void downloadOneFailsFastForAnUnresolvedTrack() {
        HttpClient fakeClient = new FakeHttpClient("");
        YouTubeAudioProvider provider = new YouTubeAudioProvider(fakeClient, new YtDlpConfig());
        Track track = new Track("Title", "Artist", null, null, null);
        TrackResolution failed = new TrackResolution.Failed(track, "no match");

        var result = provider.downloadOne(failed, java.nio.file.Path.of("/tmp"));

        assertInstanceOf(com.songs.model.DownloadResult.DownloadFailed.class, result);
    }

    private String readFixture() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/youtube-search-results.json")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static class FakeHttpClient implements HttpClient {
        private final String response;

        FakeHttpClient(String response) {
            this.response = response;
        }

        @Override
        public String get(String uri, Duration timeout) {
            return response;
        }

        @Override
        public String post(String uri, String jsonBody, Duration timeout) {
            return response;
        }
    }
}
