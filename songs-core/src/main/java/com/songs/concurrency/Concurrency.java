package com.songs.concurrency;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Concurrency {

  private static final Logger logger = LoggerFactory.getLogger(Concurrency.class);

  private Concurrency() {}

  /**
   * Order-preserving fan-out: result[i] corresponds to items[i], regardless of completion order.
   */
  public static <T, R> List<R> parallelMap(List<T> items, Function<T, R> fn, int concurrency) {
    logger.debug(
        "Starting parallel map of {} items with concurrency {}", items.size(), concurrency);
    Semaphore gate = new Semaphore(concurrency);

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<CompletableFuture<R>> futures =
          items.stream()
              .map(
                  item ->
                      CompletableFuture.supplyAsync(
                          () -> {
                            gate.acquireUninterruptibly();
                            try {
                              return fn.apply(item);
                            } finally {
                              gate.release();
                            }
                          },
                          executor))
              .toList();

      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
      List<R> results = futures.stream().map(CompletableFuture::join).toList();
      logger.debug("Completed parallel map of {} items", items.size());
      return results;
    }
  }
}
