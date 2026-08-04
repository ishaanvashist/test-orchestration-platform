package dev.ishaan.test_orchestration_platform;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;

@Entity
@Table(name = "test_results")
public class TestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean passed;                                            // true = passed, false = failed

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_run_id")                                  // connects this result to one specific run
    private TestRun testRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id")                                 // connects this result to one specific test
    private TestCase testCase;

    public TestResult() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }

    public TestRun getTestRun() { return testRun; }
    public void setTestRun(TestRun testRun) { this.testRun = testRun; }

    public TestCase getTestCase() { return testCase; }
    public void setTestCase(TestCase testCase) { this.testCase = testCase; }
}