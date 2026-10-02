package dao;

import model.Exam;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class ExamDao {
    private static final String BASE =
            "SELECT e.id, e.subject_id, s.name AS sname, e.title, e.description, e.faculty_id, u.full_name AS fname, "
          + "e.duration_minutes, e.start_time, e.end_time, e.published, e.randomize, "
          + "(SELECT COUNT(*) FROM questions q WHERE q.exam_id = e.id) AS qc, "
          + "(SELECT COALESCE(SUM(q.marks), 0) FROM questions q WHERE q.exam_id = e.id) AS tm "
          + "FROM exams e JOIN subjects s ON s.id = e.subject_id JOIN users u ON u.id = e.faculty_id ";
    private ExamDao() {}

    private static Exam map(ResultSet rs) throws SQLException {
        return new Exam(rs.getLong("id"), rs.getLong("subject_id"), rs.getString("sname"), rs.getString("title"),
                rs.getString("description"), rs.getLong("faculty_id"), rs.getString("fname"),
                rs.getInt("duration_minutes"), Sql.ldt(rs, "start_time"), Sql.ldt(rs, "end_time"),
                rs.getBoolean("published"), rs.getBoolean("randomize"), rs.getInt("qc"), rs.getInt("tm"));
    }

    public static Exam find(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(BASE + "WHERE e.id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    /** facultyIdOrNull == null lists all exams. */
    public static List<Exam> list(Connection c, Long facultyIdOrNull) throws SQLException {
        String sql = BASE + (facultyIdOrNull == null ? "" : "WHERE e.faculty_id = ? ") + "ORDER BY e.start_time DESC";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            if (facultyIdOrNull != null) ps.setLong(1, facultyIdOrNull);
            try (ResultSet rs = ps.executeQuery()) {
                List<Exam> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    /** Published exams inside their window, with questions, that the student has not yet completed. */
    public static List<Exam> availableFor(Connection c, long studentId, LocalDateTime now) throws SQLException {
        String sql = BASE + "WHERE e.published = 1 AND e.start_time <= ? AND e.end_time >= ? "
                + "AND EXISTS (SELECT 1 FROM questions q WHERE q.exam_id = e.id) "
                + "AND NOT EXISTS (SELECT 1 FROM exam_attempts a WHERE a.exam_id = e.id AND a.student_id = ? "
                + "AND a.status <> 'IN_PROGRESS') ORDER BY e.end_time";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Sql.ts(now));
            ps.setTimestamp(2, Sql.ts(now));
            ps.setLong(3, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Exam> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    public static void insert(Connection c, Exam e) throws SQLException {
        String sql = "INSERT INTO exams (subject_id, faculty_id, title, description, duration_minutes, start_time, end_time, published, randomize) "
                + "VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, e);
            ps.executeUpdate();
        }
    }

    public static void update(Connection c, Exam e) throws SQLException {
        String sql = "UPDATE exams SET subject_id=?, faculty_id=?, title=?, description=?, duration_minutes=?, "
                + "start_time=?, end_time=?, published=?, randomize=? WHERE id=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, e);
            ps.setLong(10, e.id());
            ps.executeUpdate();
        }
    }

    private static void bind(PreparedStatement ps, Exam e) throws SQLException {
        ps.setLong(1, e.subjectId());
        ps.setLong(2, e.facultyId());
        ps.setString(3, e.title());
        ps.setString(4, e.description());
        ps.setInt(5, e.durationMinutes());
        ps.setTimestamp(6, Sql.ts(e.startTime()));
        ps.setTimestamp(7, Sql.ts(e.endTime()));
        ps.setBoolean(8, e.published());
        ps.setBoolean(9, e.randomize());
    }

    public static void delete(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM exams WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}
