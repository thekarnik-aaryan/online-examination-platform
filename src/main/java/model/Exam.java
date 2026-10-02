package model;

import java.time.LocalDateTime;

public record Exam(long id, long subjectId, String subjectName, String title, String description,
                   long facultyId, String facultyName, int durationMinutes,
                   LocalDateTime startTime, LocalDateTime endTime,
                   boolean published, boolean randomize, int questionCount, int totalMarks) {
    @Override public String toString() { return title; }
}
