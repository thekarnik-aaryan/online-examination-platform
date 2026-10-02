package model;

import java.time.LocalDateTime;

public record Attempt(long id, long examId, long studentId, LocalDateTime startedAt, LocalDateTime deadline,
                      LocalDateTime submittedAt, String status, String questionOrder) {
    public boolean inProgress() { return "IN_PROGRESS".equals(status); }
}
