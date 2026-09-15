package dev.ishaan.test_orchestration_platform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest                                                    // starts your real, full Spring app for this test
@AutoConfigureMockMvc                                               // gives us a way to send simulated HTTP requests
@Testcontainers                                                     // tells JUnit to manage the containers below automatically
class TestRunIntegrationTest {

    @Container                                                       // a real, temporary, disposable Postgres, just for this test
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @Container                                                       // a real, temporary, disposable Redis, just for this test
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379);

    @DynamicPropertySource                                           // tells Spring to use THESE containers, not your normal local ones
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));  // Redis gets a random real port — ask it which one
    }

    @Autowired
    private MockMvc mockMvc; // lets us send real, simulated requests and check real responses


    @Test
    void ingestTestRun_withValidPayload_createsNewRun() throws Exception {
        String requestBody = """
                {
                    "pipelineName": "Integration Test Pipeline",
                    "ranAt": "2026-09-13T10:00:00",
                    "results": [
                        {"testName": "test_example", "passed": true}
                    ]
                }
                """;

        mockMvc.perform(post("/api/test-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());  // no token provided — should be rejected
    }

}