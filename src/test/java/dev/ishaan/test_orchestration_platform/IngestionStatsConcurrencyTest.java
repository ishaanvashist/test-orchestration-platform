package dev.ishaan.test_orchestration_platform;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Plain JUnit test: no Spring, no Docker, no database. It runs in about a second.
class IngestionStatsConcurrencyTest {

    @Test
    void everyIngestionShouldBeCounted_evenWhenThreadsCountAtOnce() throws Exception {
        int threads = 20;                                        // 20 request threads at once
        int runsPerThread = 10_000;                              // each one records 10,000 ingestions
        long expected = (long) threads * runsPerThread;          // so the count should be exactly 200,000

        IngestionStats stats = new IngestionStats();             // ONE shared counter for all threads

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);      // counts threads standing at the line
        CountDownLatch go = new CountDownLatch(1);               // the starting gun

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                ready.countDown();                               // I am at the line
                go.await();                                      // wait for the gun
                for (int i = 0; i < runsPerThread; i++) {
                    stats.recordIngestion();                     // everyone adds to the same counter
                }
                return null;
            });
        }

        ready.await();                                           // wait until all 20 are at the line
        go.countDown();                                          // fire the gun
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);             // wait for everyone to finish

        long actual = stats.getRunsIngested();
        System.out.println("EXPECTED COUNT: " + expected);
        System.out.println("ACTUAL COUNT:   " + actual);
        System.out.println("LOST UPDATES:   " + (expected - actual));

        assertEquals(expected, actual, "Some ingestions were never counted");
    }
}
