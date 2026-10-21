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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest                                                      // starts the real app, same as the existing test
@Testcontainers                                                      // lets JUnit manage the containers below
class ConcurrentIngestTest {

    @Container                                                       // a real, disposable Postgres, same as the existing test
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @Container                                                       // a real, disposable Redis, same as the existing test
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

    @Autowired
    private TestCaseRepository testCaseRepository;                   // used to count rows afterwards

    @Test
    void sameNewTestName_arrivingAtOnce_shouldCreateOnlyOneTestCase() throws Exception {
        int workers = 20;                                            // how many requests arrive together
        String testName = "race_test_" + System.nanoTime();          // a name the database has never seen

        ExecutorService pool = Executors.newFixedThreadPool(workers); // 20 worker threads
        CountDownLatch ready = new CountDownLatch(workers);          // counts workers standing at the line
        CountDownLatch go = new CountDownLatch(1);                   // the starting gun
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < workers; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();                                   // I am at the line
                go.await();                                          // wait for the gun
                testRunService.ingestTestRun(buildRequest(testName)); // all fire at once
                return null;
            }));
        }

        ready.await();                                               // wait until all 20 are at the line
        go.countDown();                                              // fire the gun
        pool.shutdown();
        pool.awaitTermination(60, TimeUnit.SECONDS);                 // wait for everyone to finish

        int failures = 0;
        for (Future<?> f : futures) {
            try { f.get(); } catch (Exception e) { failures++; System.out.println("FAILURE: " + e.getMessage()); }    // count requests that threw an error
        }

        long rows = testCaseRepository.findAll().stream()
                .filter(t -> testName.equals(t.getName()))
                .count();                                            // how many rows have this name

        System.out.println("REQUESTS THAT FAILED: " + failures + " of " + workers);
        System.out.println("TESTCASE ROWS WITH THIS NAME: " + rows);

        assertEquals(1, rows, "Expected exactly 1 TestCase row, but found " + rows);
    }

    private TestRunRequest buildRequest(String testName) {
        TestResultRequest result = new TestResultRequest();          // one test outcome
        result.setTestName(testName);
        result.setPassed(true);

        TestRunRequest request = new TestRunRequest();               // one pipeline run containing it
        request.setPipelineName("race-pipeline");
        request.setRanAt(LocalDateTime.now().minusMinutes(1));
        request.setResults(List.of(result));
        return request;
    }
}
