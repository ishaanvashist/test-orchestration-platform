package dev.ishaan.test_orchestration_platform;

// A tiny background worker: it keeps looping until someone tells it to stop.
// Real workers (cleanup jobs, pollers, queue consumers) follow this same shape.
// It is NOT a Spring bean, so it never starts by itself in the live app. Only the test starts it.
public class BackgroundWorker {

    private volatile boolean running = true; // volatile: always read the real value, so the stop is seen         // the stop flag: NOT volatile yet (this is the bug)
    private long passes = 0;                 // how many loops it did, read after it stops

    public void run() {                      // the worker thread runs this
        long count = 0;
        while (running) {                    // keep going until running becomes false
            count++;                         // the "work": kept tiny on purpose, see the notes
        }
        passes = count;                      // only reached if the worker sees running = false
    }

    public void stop() {                     // another thread (here, the test) calls this
        running = false;
    }

    public long getPasses() {
        return passes;
    }
}
