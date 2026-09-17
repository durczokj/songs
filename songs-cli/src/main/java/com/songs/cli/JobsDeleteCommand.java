package com.songs.cli;

import com.songs.jobs.ConfigFileJobStore;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "delete", description = "Delete a saved sync job.")
final class JobsDeleteCommand implements Callable<Integer> {
    @Parameters(index = "0", description = "Job name")
    String name;

    @Override
    public Integer call() {
        try {
            if (!new ConfigFileJobStore().delete(name)) {
                System.err.println("songs jobs delete: job not found: " + name);
                return 4;
            }
            System.out.println("Deleted job " + name);
            return 0;
        } catch (Exception e) {
            System.err.println("songs jobs delete: " + e.getMessage());
            return 1;
        }
    }
}