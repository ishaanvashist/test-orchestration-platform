package dev.ishaan.test_orchestration_platform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TestResultRequest {

    @NotBlank(message = "Test name cannot be blank")
    @Size(max = 255, message = "Test name cannot exceed 255 characters")
    private String testName;                                           // just the name, like "test_login" — not a full TestCase object

    private boolean passed;

    public TestResultRequest() {
    }

    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }
}