package com.songs.provider;

import com.songs.model.AudioRef;
import com.songs.model.DownloadResult;
import com.songs.model.Track;
import com.songs.model.TrackResolution;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AudioProviderTest {

    private final AudioProvider provider = new FakeAudioProvider();

    @Test
    void resolveManyPreservesInputOrder() {
        List<Track> tracks = List.of(
            new Track("A", "Artist A", null, null, null),
            new Track("B", "Artist B", null, null, null),
            new Track("C", "Artist C", null, null, null)
        );

        List<TrackResolution> resolutions = provider.resolveMany(tracks, 2);

        assertEquals(3, resolutions.size());
        for (int i = 0; i < tracks.size(); i++) {
            assertEquals(tracks.get(i), resolutions.get(i).track());
        }
    }

    @Test
    void downloadManyPreservesInputOrder() {
        List<Track> tracks = List.of(
            new Track("A", "Artist A", null, null, null),
            new Track("B", "Artist B", null, null, null)
        );
        List<TrackResolution> resolutions = provider.resolveMany(tracks, 2);

        List<DownloadResult> downloads = provider.downloadMany(resolutions, Path.of("/tmp"), 2);

        assertEquals(2, downloads.size());
        for (int i = 0; i < resolutions.size(); i++) {
            assertInstanceOf(DownloadResult.Downloaded.class, downloads.get(i));
            assertEquals(resolutions.get(i), downloads.get(i).resolution());
        }
    }

    private static class FakeAudioProvider implements AudioProvider {
        @Override
        public String name() {
            return "fake";
        }

        @Override
        public TrackResolution resolveOne(Track track) {
            return new TrackResolution.Resolved(track, new AudioRef("fake", "fake://" + track.title()));
        }

        @Override
        public DownloadResult downloadOne(TrackResolution resolution, Path outputDir) {
            return new DownloadResult.Downloaded(resolution, outputDir.resolve(resolution.track().title() + ".mp3"));
        }
    }
}
