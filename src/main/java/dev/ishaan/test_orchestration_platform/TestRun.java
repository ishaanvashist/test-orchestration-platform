package dev.ishaan.test_orchestration_platform;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_runs")
public class TestRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Pipeline name cannot be blank")
    private String pipelineName;                                      // e.g. "GitHub Actions - task-api CI"

    private LocalDateTime ranAt;                                       // when this batch of tests actually ran

    public TestRun() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPipelineName() { return pipelineName; }
    public void setPipelineName(String pipelineName) { this.pipelineName = pipelineName; }

    public LocalDateTime getRanAt() { return ranAt; }
    public void setRanAt(LocalDateTime ranAt) { this.ranAt = ranAt; }
}