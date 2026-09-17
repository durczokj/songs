package com.songs.cli;

import picocli.CommandLine.Command;

@Command(
    name = "jobs",
    description = "Define and run playlist synchronization jobs.",
    subcommands = {
        JobsCreateCommand.class,
        JobsListCommand.class,
        JobsGetCommand.class,
        JobsDeleteCommand.class,
        JobsRunCommand.class
    }
)
final class JobsCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Use 'songs jobs --help' to see available commands.");
    }
}