package dev.ishaan.test_orchestration_platform;

import java.util.List;

public class TestFlakinessResponse {

    private String testName;
    private int totalRuns;
    private int passedRuns;
    private double passRate;                                             // percentage, e.g. 70.0
    private List<TestResult> history;

    public TestFlakinessResponse() {
    }

    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }

    public int getTotalRuns() { return totalRuns; }
    public void setTotalRuns(int totalRuns) { this.totalRuns = totalRuns; }

    public int getPassedRuns() { return passedRuns; }
    public void setPassedRuns(int passedRuns) { this.passedRuns = passedRuns; }

    public double getPassRate() { return passRate; }
    public void setPassRate(double passRate) { this.passRate = passRate; }

    public List<TestResult> getHistory() { return history; }
    public void setHistory(List<TestResult> history) { this.history = history; }
}