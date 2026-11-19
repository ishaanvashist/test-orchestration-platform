package dev.ishaan.test_orchestration_platform;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// Keeps live counts of what the app has ingested.
// Many request threads can call these methods at the same time.
// It is NOT a Spring bean yet, so it doesn't change the live app. Only the tests use it.
public class IngestionStats {

    private final AtomicLong runsIngested = new AtomicLong(0);   // atomic: read-add-write as one unbreakable step

    private final Map<String, Long> resultsPerTest = new ConcurrentHashMap<>();  // thread-safe map, used with an atomic method below

    public void recordIngestion() {          // called once per ingested test run
        runsIngested.incrementAndGet();      // adds 1 safely, even with many threads at once
    }

    public long getRunsIngested() {          // read the current count
        return runsIngested.get();           // reads the real, latest value
    }

    public void recordResult(String testName) {                  // called once per test result, e.g. "login_test"
        resultsPerTest.merge(testName, 1L, Long::sum);           // look up, add 1, write back: ONE atomic step
    }                                                            // (if the name is new, it starts at 1)

    public long getResultCount(String testName) {                // read the count for one test name
        Long count = resultsPerTest.get(testName);
        return (count == null) ? 0 : count;
    }
}