package dev.ishaan.test_orchestration_platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TestRunService {

    private static final Logger logger = LoggerFactory.getLogger(TestRunService.class);

    private final TestRunRepository testRunRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestResultRepository testResultRepository;
    private final CacheEvictionService cacheEvictionService;

    public TestRunService(TestRunRepository testRunRepository,
                          TestCaseRepository testCaseRepository,
                          TestResultRepository testResultRepository,
                          CacheEvictionService cacheEvictionService) {
        this.testRunRepository = testRunRepository;
        this.testCaseRepository = testCaseRepository;
        this.testResultRepository = testResultRepository;
        this.cacheEvictionService = cacheEvictionService;
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

            cacheEvictionService.evictTestHistoryCache(resultRequest.getTestName());
        }

        logger.info("Successfully ingested {} results for run id {}", request.getResults().size(), testRun.getId());

        return testRun;
    }

    public Page<TestRun> getAllTestRuns(Pageable pageable, String pipelineName) {
        if (pipelineName != null && !pipelineName.isBlank()) {
            return testRunRepository.findByPipelineNameContainingIgnoreCase(pipelineName, pageable);
        }
        return testRunRepository.findAll(pageable);
    }

    public TestRun getTestRunById(Long id) {
        return testRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test run not found with id: " + id));  // now returns a proper 404, not a generic 500
    }

    @Cacheable(value = "testHistory", key = "#testName")
    public TestFlakinessResponse getTestHistory(String testName) {

        logger.info("Fetching test history from DATABASE for: {}", testName);

        TestCase testCase = testCaseRepository.findByName(testName)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with name: " + testName));  // now returns a proper 404, not a generic 500

        List<TestResult> history = testResultRepository.findByTestCaseIdWithRun(testCase.getId());

        int totalRuns = history.size();
        long passedRuns = history.stream().filter(TestResult::isPassed).count();
        double passRate = totalRuns == 0 ? 0.0 : (passedRuns * 100.0) / totalRuns;

        TestFlakinessResponse response = new TestFlakinessResponse();
        response.setTestName(testName);
        response.setTotalRuns(totalRuns);
        response.setPassedRuns((int) passedRuns);
        response.setPassRate(passRate);
        response.setHistory(history);

        return response;
    }


}