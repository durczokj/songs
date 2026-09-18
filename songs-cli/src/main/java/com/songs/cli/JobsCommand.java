package com.songs.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(
    name = "jobs",
    description = "Define and run playlist synchronization jobs.",
    mixinStandardHelpOptions = true,
    subcommands = {
        JobsCreateCommand.class,
        JobsListCommand.class,
        JobsGetCommand.class,
        JobsDeleteCommand.class,
        JobsRunCommand.class
    }
)
final class JobsCommand implements Runnable {
    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}