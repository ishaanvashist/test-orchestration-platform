package dev.ishaan.test_orchestration_platform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;

public class TestRunRequest {

    @NotBlank(message = "Pipeline name cannot be blank")
    private String pipelineName;

    private LocalDateTime ranAt;

    @NotEmpty(message = "A test run must include at least one result")
    private List<@Valid TestResultRequest> results;                    // the list of individual test outcomes in this run

    public TestRunRequest() {
    }

    public String getPipelineName() { return pipelineName; }
    public void setPipelineName(String pipelineName) { this.pipelineName = pipelineName; }

    public LocalDateTime getRanAt() { return ranAt; }
    public void setRanAt(LocalDateTime ranAt) { this.ranAt = ranAt; }

    public List<TestResultRequest> getResults() { return results; }
    public void setResults(List<TestResultRequest> results) { this.results = results; }
}