package dao;

import model.Attempt;
import model.Result;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public final class AttemptDao {
    private static final String ACOLS = "id, exam_id, student_id, started_at, deadline, submitted_at, status, question_order";
    private static final String RBASE =
            "SELECT r.attempt_id, e.title, u.full_name, u.username, r.score, r.total_marks, r.percentage, "
          + "r.correct_count, r.incorrect_count, r.unattempted_count, a.submitted_at "
          + "FROM results r JOIN exam_attempts a ON a.id = r.attempt_id "
          + "JOIN exams e ON e.id = a.exam_id JOIN users u ON u.id = a.student_id ";
    private AttemptDao() {}

    private static Attempt map(ResultSet rs) throws SQLException {
        return new Attempt(rs.getLong("id"), rs.getLong("exam_id"), rs.getLong("student_id"),
                Sql.ldt(rs, "started_at"), Sql.ldt(rs, "deadline"), Sql.ldt(rs, "submitted_at"),
                rs.getString("status"), rs.getString("question_order"));
    }

    public static Attempt findByExamAndStudent(Connection c, long examId, long studentId, boolean lock) throws SQLException {
        String sql = "SELECT " + ACOLS + " FROM exam_attempts WHERE exam_id = ? AND student_id = ?" + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, examId);
            ps.setLong(2, studentId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public static Attempt findById(Connection c, long id, boolean lock) throws SQLException {
        String sql = "SELECT " + ACOLS + " FROM exam_attempts WHERE id = ?" + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public static long create(Connection c, long examId, long studentId, LocalDateTime start, LocalDateTime deadline, String order) throws SQLException {
        String sql = "INSERT INTO exam_attempts (exam_id, student_id, started_at, deadline, question_order) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, examId);
            ps.setLong(2, studentId);
            ps.setTimestamp(3, Sql.ts(start));
            ps.setTimestamp(4, Sql.ts(deadline));
            ps.setString(5, order);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { k.next(); return k.getLong(1); }
        }
    }

    public static boolean examHasAttempts(Connection c, long examId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM exam_attempts WHERE exam_id = ? LIMIT 1")) {
            ps.setLong(1, examId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    /** True if the option exists, belongs to the question, and the question belongs to the exam. */
    public static boolean optionBelongs(Connection c, long examId, long questionId, long optionId) throws SQLException {
        String sql = "SELECT 1 FROM options o JOIN questions q ON q.id = o.question_id "
                + "WHERE o.id = ? AND q.id = ? AND q.exam_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, optionId);
            ps.setLong(2, questionId);
            ps.setLong(3, examId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public static boolean questionBelongs(Connection c, long examId, long questionId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM questions WHERE id = ? AND exam_id = ?")) {
            ps.setLong(1, questionId);
            ps.setLong(2, examId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public static void saveAnswer(Connection c, long attemptId, long questionId, Long optionIdOrNull) throws SQLException {
        String sql = "INSERT INTO student_answers (attempt_id, question_id, option_id) VALUES (?,?,?) "
                + "ON DUPLICATE KEY UPDATE option_id = VALUES(option_id), answered_at = CURRENT_TIMESTAMP";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, attemptId);
            ps.setLong(2, questionId);
            if (optionIdOrNull == null) ps.setNull(3, Types.BIGINT); else ps.setLong(3, optionIdOrNull);
            ps.executeUpdate();
        }
    }

    /** question id -> chosen option id (null answers omitted). */
    public static Map<Long, Long> loadAnswers(Connection c, long attemptId) throws SQLException {
        Map<Long, Long> out = new HashMap<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT question_id, option_id FROM student_answers WHERE attempt_id = ? AND option_id IS NOT NULL")) {
            ps.setLong(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getLong(1), rs.getLong(2));
            }
        }
        return out;
    }

    public static void markSubmitted(Connection c, long attemptId, String status, LocalDateTime when) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE exam_attempts SET status = ?, submitted_at = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setTimestamp(2, Sql.ts(when));
            ps.setLong(3, attemptId);
            ps.executeUpdate();
        }
    }

    public static void insertResult(Connection c, long attemptId, int score, int total, double pct,
                                    int correct, int incorrect, int unattempted) throws SQLException {
        String sql = "INSERT INTO results (attempt_id, score, total_marks, percentage, correct_count, incorrect_count, unattempted_count) "
                + "VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, attemptId);
            ps.setInt(2, score);
            ps.setInt(3, total);
            ps.setDouble(4, pct);
            ps.setInt(5, correct);
            ps.setInt(6, incorrect);
            ps.setInt(7, unattempted);
            ps.executeUpdate();
        }
    }

    public static List<Long> expiredInProgress(Connection c, long studentId, LocalDateTime now) throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id FROM exam_attempts WHERE student_id = ? AND status = 'IN_PROGRESS' AND deadline <= ?")) {
            ps.setLong(1, studentId);
            ps.setTimestamp(2, Sql.ts(now));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) ids.add(rs.getLong(1)); }
        }
        return ids;
    }

    public static Result resultOf(Connection c, long attemptId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(RBASE + "WHERE r.attempt_id = ?")) {
            ps.setLong(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapResult(rs) : null; }
        }
    }

    public static List<Result> resultsForStudent(Connection c, long studentId) throws SQLException {
        return queryResults(c, "WHERE a.student_id = ? ORDER BY a.submitted_at DESC", studentId);
    }

    public static List<Result> resultsForFaculty(Connection c, long facultyId) throws SQLException {
        return queryResults(c, "WHERE e.faculty_id = ? ORDER BY a.submitted_at DESC", facultyId);
    }

    public static List<Result> allResults(Connection c) throws SQLException {
        return queryResults(c, "ORDER BY a.submitted_at DESC", null);
    }

    private static List<Result> queryResults(Connection c, String tail, Long param) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(RBASE + tail)) {
            if (param != null) ps.setLong(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                List<Result> out = new ArrayList<>();
                while (rs.next()) out.add(mapResult(rs));
                return out;
            }
        }
    }

    private static Result mapResult(ResultSet rs) throws SQLException {
        return new Result(rs.getLong("attempt_id"), rs.getString("title"), rs.getString("full_name"), rs.getString("username"),
                rs.getInt("score"), rs.getInt("total_marks"), rs.getDouble("percentage"),
                rs.getInt("correct_count"), rs.getInt("incorrect_count"), rs.getInt("unattempted_count"),
                Sql.ldt(rs, "submitted_at"));
    }
}
