package dev.ishaan.test_orchestration_platform;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, Long> {

    @Query("SELECT r FROM TestResult r JOIN FETCH r.testRun WHERE r.testCase.id = :testCaseId ORDER BY r.testRun.ranAt DESC")
    List<TestResult> findByTestCaseIdWithRun(Long testCaseId);

}