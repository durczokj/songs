package com.songs.cli;

import com.songs.repository.PlaylistRepositoryRegistry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "show", description = "Print a playlist's tracks.")
final class ShowCommand implements Callable<Integer> {
    @Parameters(index = "0", description = "Playlist URI")
    String uri;

    @Override
    public Integer call() {
        try {
            PlaylistRepositoryRegistry registry = RepositoryFactory.create(1);
            var playlist = registry.readerFor(uri).extract(uri);
            System.out.printf("%s (%d tracks)%n",
                playlist.name() == null ? uri : playlist.name(), playlist.tracks().size());
            playlist.tracks().forEach(track -> System.out.println("- " + track));
            return 0;
        } catch (Exception e) {
            System.err.println("songs show: " + e.getMessage());
            return 1;
        }
    }
}
