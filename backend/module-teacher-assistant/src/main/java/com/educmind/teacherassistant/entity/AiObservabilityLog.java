package com.educmind.teacherassistant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Bảng ai_observability_logs — mỗi lượt gọi AI (kể cả lỗi) ghi 1 dòng; cũng là nguồn đếm rate limit và ai-usage. */
@Entity
@Table(name = "ai_observability_logs")
public class AiObservabilityLog {

    public static final String CONTEXT_TEACHER_ASSISTANT = "TEACHER_ASSISTANT";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "request_context_type", nullable = false, length = 30)
    private String requestContextType;

    @Column(name = "request_context_id", nullable = false)
    private UUID requestContextId;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(name = "task_type", nullable = false, length = 30)
    private String taskType;

    @Column(name = "latency_ms", nullable = false)
    private int latencyMs;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "estimated_cost", precision = 10, scale = 6)
    private BigDecimal estimatedCost;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected AiObservabilityLog() {
    }

    public AiObservabilityLog(String requestContextType, UUID requestContextId, UUID actorId,
                              String provider, String model, String taskType, int latencyMs,
                              Integer inputTokens, Integer outputTokens,
                              String status, String errorMessage) {
        this.requestContextType = requestContextType;
        this.requestContextId = requestContextId;
        this.actorId = actorId;
        this.provider = provider;
        this.model = model;
        this.taskType = taskType;
        this.latencyMs = latencyMs;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public UUID getId() { return id; }
    public String getRequestContextType() { return requestContextType; }
    public UUID getRequestContextId() { return requestContextId; }
    public UUID getActorId() { return actorId; }
    public String getProvider() { return provider; }
    public String getModel() { return model; }
    public String getTaskType() { return taskType; }
    public int getLatencyMs() { return latencyMs; }
    public Integer getInputTokens() { return inputTokens; }
    public Integer getOutputTokens() { return outputTokens; }
    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(BigDecimal estimatedCost) { this.estimatedCost = estimatedCost; }
    public String getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
