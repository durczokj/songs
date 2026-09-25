package com.songs.cli;

import com.songs.jobs.ConfigFileJobStore;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

@Command(name = "list", description = "List saved sync jobs.", mixinStandardHelpOptions = true)
final class JobsListCommand implements Callable<Integer> {
  @Override
  public Integer call() {
    try {
      new ConfigFileJobStore()
          .list()
          .forEach(
              job -> System.out.printf("%s\t%s\t%s%n", job.name(), job.source(), job.target()));
      return 0;
    } catch (Exception e) {
      System.err.println("songs jobs list: " + e.getMessage());
      return 1;
    }
  }
}
