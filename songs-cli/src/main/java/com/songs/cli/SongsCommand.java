package com.songs.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "songs",
    description = "Synchronize music playlists.",
    mixinStandardHelpOptions = true,
    version = "songs 0.1.0",
    subcommands = {SyncCommand.class, ShowCommand.class, DoctorCommand.class, SetupBrowserCommand.class}
)
public final class SongsCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Enable debug logging.")
    boolean verbose;

    @Option(names = {"-q", "--quiet"}, description = "Show errors only.")
    boolean quiet;

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    public static void main(String[] args) {
        configureLogging(args);
        int exitCode = new CommandLine(new SongsCommand()).execute(args);
        System.exit(exitCode);
    }

    private static void configureLogging(String[] args) {
        String level = "INFO";
        for (String arg : args) {
            if (arg.equals("-q") || arg.equals("--quiet")) {
                level = "ERROR";
            } else if (arg.equals("-v") || arg.equals("--verbose")) {
                level = "DEBUG";
            }
        }
        System.setProperty("LOG_LEVEL", level);
    }
}
