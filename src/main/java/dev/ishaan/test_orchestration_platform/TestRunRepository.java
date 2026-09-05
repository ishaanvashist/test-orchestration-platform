package dev.ishaan.test_orchestration_platform;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestRunRepository extends JpaRepository<TestRun, Long> {

    Page<TestRun> findAll(Pageable pageable);

    Page<TestRun> findByPipelineNameContainingIgnoreCase(String pipelineName, Pageable pageable);  // filter by partial, case-insensitive name match

}