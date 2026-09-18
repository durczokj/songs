package com.songs.cli;

import com.songs.config.SongsConfigFile;
import com.songs.jobs.ConfigFileJobStore;
import com.songs.jobs.SyncJob;
import com.songs.jobs.SyncJobRunner;
import com.songs.model.AddResult;
import com.songs.model.RemoveResult;
import com.songs.model.SyncPlan;
import com.songs.model.SyncResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.PrintWriter;
import java.util.Objects;
import java.util.concurrent.Callable;

@Command(name = "run", description = "Run a saved or inline sync job.", mixinStandardHelpOptions = true)
final class JobsRunCommand implements Callable<Integer> {
    @Parameters(index = "0", arity = "0..1", description = "Saved job name")
    String name;

    @Option(names = "--source", description = "Source playlist URI for an inline job.")
    String sourceUri;

    @Option(names = "--target", description = "Target playlist URI for an inline job.")
    String targetUri;

    @Option(names = "--dry-run", description = "Show the plan without changing the target.")
    boolean dryRun;

    @Option(names = "--concurrency", description = "Maximum parallel audio operations.")
    Integer concurrency;

    @Override
    public Integer call() {
        try {
            SyncJob job = resolveJob();
            int effectiveConcurrency = concurrency != null
                ? concurrency.intValue()
                : Objects.requireNonNullElse(new SongsConfigFile().load().settings().concurrency(), 4);
            if (effectiveConcurrency < 1) {
                System.err.println("--concurrency must be >= 1");
                return 2;
            }
            SyncJobRunner runner = new SyncJobRunner(
                RepositoryFactory.create(effectiveConcurrency),
                new com.songs.sync.PlaylistSynchronizer()
            );
            PrintWriter out = new PrintWriter(System.out, true);
            if (dryRun) {
                SyncPlan plan = runner.preview(job);
                Renderer.plan(plan, out);
                return 0;
            }
            SyncResult result = runner.run(job);
            Renderer.result(result, out);
            return hasFailures(result) ? 3 : 0;
        } catch (Exception e) {
            System.err.println("songs jobs run: " + e.getMessage());
            return e instanceof IllegalArgumentException ? 2 : 1;
        }
    }

    private SyncJob resolveJob() throws java.io.IOException {
        if (name != null && (sourceUri != null || targetUri != null)) {
            throw new IllegalArgumentException("provide either a job name or --source/--target");
        }
        if (name != null) {
            return new ConfigFileJobStore().load(name)
                .orElseThrow(() -> new IllegalArgumentException("job not found: " + name));
        }
        if (sourceUri == null || targetUri == null) {
            throw new IllegalArgumentException(
                "Missing required parameters: provide a job name or both --source and --target"
            );
        }
        return new SyncJob("inline", sourceUri, targetUri);
    }

    private static boolean hasFailures(SyncResult result) {
        return result.added().stream().anyMatch(AddResult.AddFailed.class::isInstance)
            || result.removed().stream().anyMatch(RemoveResult.RemoveFailed.class::isInstance);
    }
}