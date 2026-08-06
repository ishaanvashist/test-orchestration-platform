package dev.ishaan.test_orchestration_platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TestRunService {

    private static final Logger logger = LoggerFactory.getLogger(TestRunService.class);

    private final TestRunRepository testRunRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestResultRepository testResultRepository;

    public TestRunService(TestRunRepository testRunRepository,
                          TestCaseRepository testCaseRepository,
                          TestResultRepository testResultRepository) {
        this.testRunRepository = testRunRepository;
        this.testCaseRepository = testCaseRepository;
        this.testResultRepository = testResultRepository;
    }

    @Transactional
    public TestRun ingestTestRun(TestRunRequest request) {

        logger.info("Ingesting test run for pipeline: {}", request.getPipelineName());

        TestRun testRun = new TestRun();
        testRun.setPipelineName(request.getPipelineName());
        testRun.setRanAt(request.getRanAt());
        testRun = testRunRepository.save(testRun);

        for (TestResultRequest resultRequest : request.getResults()) {

            TestCase testCase = testCaseRepository.findByName(resultRequest.getTestName())
                    .orElseGet(() -> {
                        TestCase newCase = new TestCase();
                        newCase.setName(resultRequest.getTestName());
                        return testCaseRepository.save(newCase);
                    });

            TestResult testResult = new TestResult();
            testResult.setTestRun(testRun);
            testResult.setTestCase(testCase);
            testResult.setPassed(resultRequest.isPassed());
            testResultRepository.save(testResult);
        }

        logger.info("Successfully ingested {} results for run id {}", request.getResults().size(), testRun.getId());

        return testRun;
    }

    public List<TestRun> getAllTestRuns() {
        return testRunRepository.findAll();
    }

    public TestRun getTestRunById(Long id) {
        return testRunRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test run not found with id: " + id));
    }

    public TestFlakinessResponse getTestHistory(String testName) {

        // Step 1 — find the test case by name
        TestCase testCase = testCaseRepository.findByName(testName)
                .orElseThrow(() -> new RuntimeException("Test not found with name: " + testName));

        // Step 2 — fetch every result for this test case, newest first
        List<TestResult> history = testResultRepository.findByTestCaseIdWithRun(testCase.getId());

        // Step 3 — calculate the numbers
        int totalRuns = history.size();
        long passedRuns = history.stream().filter(TestResult::isPassed).count();
        double passRate = totalRuns == 0 ? 0.0 : (passedRuns * 100.0) / totalRuns;

        // Step 4 — build the response
        TestFlakinessResponse response = new TestFlakinessResponse();
        response.setTestName(testName);
        response.setTotalRuns(totalRuns);
        response.setPassedRuns((int) passedRuns);
        response.setPassRate(passRate);
        response.setHistory(history);

        return response;
    }
}