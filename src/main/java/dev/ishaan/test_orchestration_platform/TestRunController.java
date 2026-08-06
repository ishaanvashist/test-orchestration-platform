package dev.ishaan.test_orchestration_platform;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/test-runs")
public class TestRunController {

    private final TestRunService testRunService;

    public TestRunController(TestRunService testRunService) {
        this.testRunService = testRunService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestRun ingestTestRun(@Valid @RequestBody TestRunRequest request) {
        return testRunService.ingestTestRun(request);
    }

    @GetMapping
    public List<TestRun> getAllTestRuns() {
        return testRunService.getAllTestRuns();
    }

    @GetMapping("/{id}")
    public TestRun getTestRunById(@PathVariable Long id) {
        return testRunService.getTestRunById(id);
    }

}