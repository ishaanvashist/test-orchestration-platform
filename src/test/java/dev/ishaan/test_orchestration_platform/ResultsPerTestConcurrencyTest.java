package dev.ishaan.test_orchestration_platform;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Plain JUnit test: no Spring, no Docker, no database. It runs in about a second.
class ResultsPerTestConcurrencyTest {

    @Test
    void everyResultShouldBeCountedPerTestName_evenWhenThreadsCountAtOnce() throws Exception {
        int threads = 20;                                        // 20 request threads at once
        int resultsPerThread = 10_000;                           // each one records 10,000 results
        List<String> testNames = List.of(                        // 10 test names, shared by every thread
                "login_test", "logout_test", "search_test", "checkout_test", "cart_test",
                "profile_test", "payment_test", "signup_test", "filter_test", "export_test");
        long expectedPerName = (long) threads * resultsPerThread / testNames.size();   // 20,000 for each name
        long expectedTotal = (long) threads * resultsPerThread;                       // 200,000 overall

        IngestionStats stats = new IngestionStats();             // ONE shared map for all threads

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);      // counts threads standing at the line
        CountDownLatch go = new CountDownLatch(1);               // the starting gun
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            futures.add(pool.submit(() -> {
                ready.countDown();                               // I am at the line
                go.await();                                      // wait for the gun
                for (int i = 0; i < resultsPerThread; i++) {
                    stats.recordResult(testNames.get(i % testNames.size()));  // cycle through the 10 names
                }
                return null;
            }));
        }

        ready.await();                                           // wait until all 20 are at the line
        go.countDown();                                          // fire the gun
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);             // wait for everyone to finish

        int crashedThreads = 0;
        for (Future<?> f : futures) {
            try {
                f.get();                                         // did this thread finish normally?
            } catch (Exception e) {
                crashedThreads++;                                // no: a plain HashMap can break inside
                System.out.println("THREAD CRASHED: " + e.getCause());
            }
        }

        long actualTotal = 0;
        for (String name : testNames) {
            long count = stats.getResultCount(name);
            actualTotal += count;
            System.out.println(name + ": " + count + " (expected " + expectedPerName + ")");
        }

        System.out.println("EXPECTED TOTAL:  " + expectedTotal);
        System.out.println("ACTUAL TOTAL:    " + actualTotal);
        System.out.println("LOST RESULTS:    " + (expectedTotal - actualTotal));
        System.out.println("CRASHED THREADS: " + crashedThreads);

        assertEquals(expectedTotal, actualTotal, "Some results were never counted");
    }
}
