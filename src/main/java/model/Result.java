package model;

import java.time.LocalDateTime;

public record Result(long attemptId, String examTitle, String studentName, String username,
                     int score, int totalMarks, double percentage,
                     int correct, int incorrect, int unattempted, LocalDateTime submittedAt) { }
