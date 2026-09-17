package com.songs.cli;

import com.songs.jobs.ConfigFileJobStore;
import com.songs.jobs.JobStore;
import com.songs.jobs.SyncJob;
import com.songs.repository.PlaylistRepositoryRegistry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "create", description = "Create or update a saved sync job.")
final class JobsCreateCommand implements Callable<Integer> {
    @Parameters(index = "0", description = "Job name")
    String name;

    @Parameters(index = "1", description = "Source playlist URI")
    String sourceUri;

    @Parameters(index = "2", description = "Target playlist URI")
    String targetUri;

    @Option(names = "--force", description = "Replace an existing job.")
    boolean force;

    @Override
    public Integer call() {
        try {
            SyncJob job = new SyncJob(name, sourceUri, targetUri);
            PlaylistRepositoryRegistry registry = RepositoryFactory.create(1);
            registry.readerFor(sourceUri);
            registry.writerFor(targetUri);
            JobStore store = new ConfigFileJobStore();
            if (!force && store.load(name).isPresent()) {
                System.err.println("songs jobs create: job already exists: " + name);
                return 3;
            }
            store.save(job);
            System.out.println("Created job " + name);
            return 0;
        } catch (Exception e) {
            System.err.println("songs jobs create: " + e.getMessage());
            return 2;
        }
    }
}