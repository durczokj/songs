package com.songs.cli;

import com.songs.jobs.ConfigFileJobStore;
import com.songs.jobs.SyncJob;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "get", description = "Show a saved sync job.")
final class JobsGetCommand implements Callable<Integer> {
    @Parameters(index = "0", description = "Job name")
    String name;

    @Override
    public Integer call() {
        try {
            SyncJob job = new ConfigFileJobStore().load(name).orElse(null);
            if (job == null) {
                System.err.println("songs jobs get: job not found: " + name);
                return 4;
            }
            System.out.println("name: " + job.name());
            System.out.println("source: " + job.source());
            System.out.println("target: " + job.target());
            return 0;
        } catch (Exception e) {
            System.err.println("songs jobs get: " + e.getMessage());
            return 1;
        }
    }
}