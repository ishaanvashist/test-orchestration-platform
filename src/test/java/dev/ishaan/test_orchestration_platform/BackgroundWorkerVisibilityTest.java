package dev.ishaan.test_orchestration_platform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

// Plain JUnit test: no Spring, no Docker, no database. It runs in a few seconds.
class BackgroundWorkerVisibilityTest {

    @Test
    void workerShouldStopAfterStopIsCalled() throws Exception {
        BackgroundWorker worker = new BackgroundWorker();

        Thread workerThread = new Thread(worker::run, "background-worker"); // the worker runs on its own thread
        workerThread.setDaemon(true);            // if it never stops, it won't keep the test run alive forever
        workerThread.start();                    // the worker starts looping

        Thread.sleep(1000);                      // let it loop for 1 second, so Java "warms up" the loop

        worker.stop();                           // the test thread sets running = false
        System.out.println("TEST THREAD: called stop()");

        workerThread.join(3000);                 // wait up to 3 seconds for the worker to finish

        boolean stillRunning = workerThread.isAlive();   // did it notice the stop?
        System.out.println("WORKER STILL RUNNING AFTER 3 SECONDS: " + stillRunning);
        if (!stillRunning) {
            System.out.println("WORKER LOOPED " + worker.getPasses() + " TIMES BEFORE STOPPING");
        }

        assertFalse(stillRunning, "The worker never saw running = false, so it kept looping");
    }
}
