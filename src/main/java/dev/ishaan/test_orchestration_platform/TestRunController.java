package dev.ishaan.test_orchestration_platform;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test-runs")
public class TestRunController {

    private static final int MAX_PAGE_SIZE = 50;                            // hard limit, regardless of what's requested

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
    public Page<TestRun> getAllTestRuns(Pageable pageable) {
        Pageable safePageable = pageable.getPageSize() > MAX_PAGE_SIZE
                ? PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort())
                : pageable;
        return testRunService.getAllTestRuns(safePageable);
    }

    @GetMapping("/{id}")
    public TestRun getTestRunById(@PathVariable Long id) {
        return testRunService.getTestRunById(id);
    }

}