package com.songs.concurrency;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcurrencyTest {

    @Test
    void preservesInputOrderEvenWhenTasksCompleteOutOfOrder() {
        List<Integer> items = List.of(0, 1, 2, 3, 4);
        // Earlier items sleep longer, so later items finish first if order weren't preserved.
        List<Integer> results = Concurrency.parallelMap(items, i -> {
            sleep((items.size() - i) * 20L);
            return i * 10;
        }, 5);

        assertEquals(List.of(0, 10, 20, 30, 40), results);
    }

    @Test
    void appliesFunctionToEveryItem() {
        List<Integer> items = List.of(1, 2, 3, 4, 5);

        List<Integer> results = Concurrency.parallelMap(items, i -> i * i, 2);

        assertEquals(List.of(1, 4, 9, 16, 25), results);
    }

    @Test
    void neverRunsMoreThanConcurrencyLimitAtOnce() {
        List<Integer> items = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        AtomicInteger running = new AtomicInteger(0);
        AtomicInteger maxObserved = new AtomicInteger(0);

        Concurrency.parallelMap(items, i -> {
            int current = running.incrementAndGet();
            maxObserved.updateAndGet(max -> Math.max(max, current));
            sleep(20);
            running.decrementAndGet();
            return i;
        }, 3);

        assertTrue(maxObserved.get() <= 3, "expected at most 3 concurrent tasks, saw " + maxObserved.get());
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
