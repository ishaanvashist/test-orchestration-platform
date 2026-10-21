package dev.ishaan.test_orchestration_platform;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    Optional<TestCase> findByName(String name); // needed to check if a test case already exists

    @Modifying
    // this query changes data (an insert), not just reads it
    @Query(value = "INSERT INTO test_cases (name) VALUES (:name) ON CONFLICT (name) DO NOTHING",
            nativeQuery = true)                                     // plain Postgres SQL, sent as written
    void insertIfMissing(@Param("name") String name);              // :name in the SQL gets this value
}