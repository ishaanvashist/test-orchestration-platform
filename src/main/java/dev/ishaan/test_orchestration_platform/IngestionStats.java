package dev.ishaan.test_orchestration_platform;

import java.util.concurrent.atomic.AtomicLong;

// Keeps a live count of how many test runs the app has ingested.
// Many request threads can call recordIngestion() at the same time.
// It is NOT a Spring bean yet, so it doesn't change the live app. Only the test uses it.
public class IngestionStats {

    private final AtomicLong runsIngested = new AtomicLong(0);   // atomic: read-add-write as one unbreakable step

    public void recordIngestion() {          // called once per ingested test run
        runsIngested.incrementAndGet();      // adds 1 safely, even with many threads at once
    }

    public long getRunsIngested() {          // read the current count
        return runsIngested.get();           // reads the real, latest value
    }
}