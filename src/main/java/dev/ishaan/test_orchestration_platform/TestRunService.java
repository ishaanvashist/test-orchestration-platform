package dev.ishaan.test_orchestration_platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional                                                       // everything below runs as one all-or-nothing unit
    public TestRun ingestTestRun(TestRunRequest request) {

        logger.info("Ingesting test run for pipeline: {}", request.getPipelineName());

        // Step 1 — create and save the run itself
        TestRun testRun = new TestRun();
        testRun.setPipelineName(request.getPipelineName());
        testRun.setRanAt(request.getRanAt());
        testRun = testRunRepository.save(testRun);

        // Step 2 — for each result in the request, find-or-create the matching test case, then save the result
        for (TestResultRequest resultRequest : request.getResults()) {

            TestCase testCase = testCaseRepository.findByName(resultRequest.getTestName())
                    .orElseGet(() -> {                                    // if not found, create a brand-new one
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
}