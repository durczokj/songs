package com.songs.cli;

import com.songs.model.AddResult;
import com.songs.model.RemoveResult;
import com.songs.model.SyncPlan;
import com.songs.model.SyncResult;
import com.songs.repository.PlaylistRepositoryRegistry;
import com.songs.repository.PlaylistWriter;
import com.songs.sync.SyncPlaylistUseCase;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.PrintWriter;
import java.util.concurrent.Callable;

@Command(name = "sync", description = "Make the target playlist match the source playlist.")
final class SyncCommand implements Callable<Integer> {
    @Parameters(index = "0", description = "Source playlist URI")
    String sourceUri;

    @Parameters(index = "1", description = "Target playlist URI")
    String targetUri;

    @Option(names = "--dry-run", description = "Show the plan without changing the target.")
    boolean dryRun;

    @Option(names = "--concurrency", description = "Maximum parallel audio operations (default: ${DEFAULT-VALUE}).", defaultValue = "4")
    int concurrency;

    @Override
    public Integer call() {
        if (concurrency < 1) {
            System.err.println("--concurrency must be >= 1");
            return 2;
        }
        try {
            PlaylistRepositoryRegistry registry = RepositoryFactory.create(concurrency);
            SyncPlaylistUseCase useCase = new SyncPlaylistUseCase(new com.songs.sync.PlaylistSynchronizer());
            PrintWriter out = new PrintWriter(System.out, true);
            if (dryRun) {
                SyncPlan plan = useCase.preview(
                    registry.readerFor(sourceUri), sourceUri,
                    registry.readerFor(targetUri), targetUri
                );
                Renderer.plan(plan, out);
                return 0;
            }
            PlaylistWriter writer = registry.writerFor(targetUri);
            SyncResult result = useCase.execute(
                registry.readerFor(sourceUri), sourceUri,
                registry.readerFor(targetUri), targetUri,
                writer
            );
            Renderer.result(result, out);
            return hasFailures(result) ? 3 : 0;
        } catch (Exception e) {
            System.err.println("songs sync: " + e.getMessage());
            return 1;
        }
    }

    private static boolean hasFailures(SyncResult result) {
        return result.added().stream().anyMatch(AddResult.AddFailed.class::isInstance)
            || result.removed().stream().anyMatch(RemoveResult.RemoveFailed.class::isInstance);
    }
}
