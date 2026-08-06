package dev.ishaan.test_orchestration_platform;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tests")
public class TestCaseController {

    private final TestRunService testRunService;

    public TestCaseController(TestRunService testRunService) {
        this.testRunService = testRunService;
    }

    @GetMapping("/{name}/history")
    public TestFlakinessResponse getTestHistory(@PathVariable String name) {
        return testRunService.getTestHistory(name);
    }
}