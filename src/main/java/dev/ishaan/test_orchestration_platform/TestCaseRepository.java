package dev.ishaan.test_orchestration_platform;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    Optional<TestCase> findByName(String name);                        // needed to check if a test case already exists
}