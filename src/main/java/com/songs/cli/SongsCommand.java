package com.songs.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "songs",
    description = "Synchronize music playlists.",
    mixinStandardHelpOptions = true,
    version = "songs 0.1.0",
    subcommands = {SyncCommand.class, ShowCommand.class, DoctorCommand.class}
)
public final class SongsCommand implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SongsCommand()).execute(args);
        System.exit(exitCode);
    }
}
