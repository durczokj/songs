package com.songs.sync;

import com.songs.http.JdkHttpClient;
import com.songs.model.AddResult;
import com.songs.model.Playlist;
import com.songs.model.SyncPlan;
import com.songs.model.SyncResult;
import com.songs.model.Track;
import com.songs.provider.youtube.YouTubeAudioProvider;
import com.songs.provider.youtube.YtDlpConfig;
import com.songs.repository.apple.ApplePlaylistRepository;
import com.songs.repository.local.LocalPlaylistRepository;
import com.songs.repository.local.TagReader;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// End-to-end: read a real Apple playlist, download the first two tracks via yt-dlp, verify
// the target directory ends up in sync. Requires network, the yt-dlp binary, and ffmpeg.
// Drive with: mvn -Dgroups=integration -Dapple.playlist.uri=<uri> test
@Tag("integration")
class PlaylistSynchronizerIntegrationTest {

    private static final int MAX_TRACKS = 2;

    @Test
    void syncsAppleSourceToEmptyLocalTargetAndConvergesOnRerun(@TempDir Path tempDir) throws IOException {
        String appleUri = System.getProperty("apple.playlist.uri");
        assumeTrue(appleUri != null && !appleUri.isBlank(),
            "set -Dapple.playlist.uri=<url> to run this integration test");
        assumeTrue(isCommandAvailable("yt-dlp", "--version"), "yt-dlp is not installed");
        assumeTrue(isCommandAvailable("ffmpeg", "-version"), "ffmpeg is not installed");

        // 1. Read source, cap to a small number of tracks so the test stays bounded.
        ApplePlaylistRepository apple = new ApplePlaylistRepository();
        Playlist fullSource = apple.extract(appleUri);
        assumeTrue(!fullSource.tracks().isEmpty(), "Apple playlist returned no tracks");
        List<Track> smallSlice = fullSource.tracks().subList(0, Math.min(MAX_TRACKS, fullSource.tracks().size()));
        Playlist source = new Playlist(fullSource.uri(), smallSlice, fullSource.name());

        // 2. Set up an empty local target.
        Path targetDir = tempDir.resolve("target");
        Files.createDirectories(targetDir);
        String targetUri = targetDir.toUri().toString();
        LocalPlaylistRepository local = new LocalPlaylistRepository(
            new YouTubeAudioProvider(new JdkHttpClient(), new YtDlpConfig()),
            new TagReader()
        );

        // 3. First sync: everything should be added.
        PlaylistSynchronizer sync = new PlaylistSynchronizer();
        Playlist emptyTarget = local.extract(targetUri);
        SyncPlan firstPlan = sync.plan(source, emptyTarget);
        assertEquals(smallSlice.size(), firstPlan.toAdd().size(), "empty target => everything to add");
        assertTrue(firstPlan.toRemove().isEmpty());
        assertTrue(firstPlan.unchanged().isEmpty());

        SyncResult firstResult = sync.apply(firstPlan, local);
        long addedOk = firstResult.added().stream().filter(AddResult.Added.class::isInstance).count();
        assertTrue(addedOk >= 1, "at least one track should have downloaded successfully");

        // 4. Filesystem should now contain mp3s.
        try (var stream = Files.list(targetDir)) {
            long mp3s = stream.filter(p -> p.toString().endsWith(".mp3")).count();
            assertTrue(mp3s >= 1, "target dir should contain at least one .mp3");
        }

        // 5. Second sync: successfully-added tracks should now be unchanged.
        Playlist reReadTarget = local.extract(targetUri);
        SyncPlan secondPlan = sync.plan(source, reReadTarget);
        assertTrue(secondPlan.unchanged().size() >= addedOk,
            "tracks added on first sync should be unchanged on second sync");

        // 6. apply with an empty plan is a no-op.
        SyncResult noop = sync.apply(
            new SyncPlan(source.uri(), targetUri, List.of(), List.of(), List.of()),
            local
        );
        assertInstanceOf(SyncResult.class, noop);
        assertTrue(noop.added().isEmpty());
        assertTrue(noop.removed().isEmpty());
    }

    private static boolean isCommandAvailable(String... command) {
        try {
            Process process = new ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
            return process.waitFor() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
