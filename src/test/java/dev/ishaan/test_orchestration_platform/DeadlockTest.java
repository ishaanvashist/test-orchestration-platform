package dev.ishaan.test_orchestration_platform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest                                                      // starts the real app, same as ConcurrentIngestTest
@Testcontainers                                                      // lets JUnit manage the containers below
class DeadlockTest {

    @Container                                                       // a real, disposable Postgres
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @Container                                                       // a real, disposable Redis
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379);

    @DynamicPropertySource                                           // point Spring at these containers
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private TestRunService testRunService;                           // the ingestion code we want to hit

    @Test
    void twoRunsWithSameNamesInOppositeOrder_shouldBothSucceed() throws Exception {
        int rounds = 5;                                              // repeat a few times so the timing clash shows up
        int namesPerRun = 50;                                        // each run carries 50 test names
        int failures = 0;

        for (int round = 1; round <= rounds; round++) {
            String base = "deadlock_" + System.nanoTime() + "_";     // brand-new names every round

            List<String> forward = new ArrayList<>();                // run A: name_0, name_1, ... name_49
            for (int i = 0; i < namesPerRun; i++) {
                forward.add(base + i);
            }
            List<String> backward = new ArrayList<>(forward);        // run B: the same names, reversed
            Collections.reverse(backward);

            ExecutorService pool = Executors.newFixedThreadPool(2);  // two worker threads: one per run
            CountDownLatch ready = new CountDownLatch(2);            // counts workers standing at the line
            CountDownLatch go = new CountDownLatch(1);               // the starting gun

            List<Future<?>> futures = new ArrayList<>();
            for (List<String> names : List.of(forward, backward)) {
                futures.add(pool.submit(() -> {
                    ready.countDown();                               // I am at the line
                    go.await();                                      // wait for the gun
                    testRunService.ingestTestRun(buildRequest(names)); // both runs fire at once
                    return null;
                }));
            }

            ready.await();                                           // wait until both are at the line
            go.countDown();                                          // fire the gun
            pool.shutdown();
            pool.awaitTermination(60, TimeUnit.SECONDS);             // wait for both to finish

            for (Future<?> f : futures) {
                try {
                    f.get();                                         // did this run succeed?
                } catch (Exception e) {
                    failures++;                                      // no: count it and show why
                    System.out.println("ROUND " + round + " FAILURE: " + e.getMessage());
                }
            }
        }

        System.out.println("RUNS THAT FAILED: " + failures + " of " + (rounds * 2));

        assertEquals(0, failures, "Expected every run to succeed, but " + failures + " failed");
    }

    private TestRunRequest buildRequest(List<String> names) {
        List<TestResultRequest> results = new ArrayList<>();
        for (String name : names) {                                  // one passed result per name, in this order
            TestResultRequest result = new TestResultRequest();
            result.setTestName(name);
            result.setPassed(true);
            results.add(result);
        }

        TestRunRequest request = new TestRunRequest();               // one pipeline run carrying all of them
        request.setPipelineName("deadlock-pipeline");
        request.setRanAt(LocalDateTime.now().minusMinutes(1));
        request.setResults(results);
        return request;
    }
}
