package dao;

import model.Option;
import model.Question;

import java.sql.*;
import java.util.*;

public final class QuestionDao {
    private QuestionDao() {}

    /** includeCorrect=false masks the correct flag (used when sending questions to students). */
    public static List<Question> listByExam(Connection c, long examId, boolean includeCorrect) throws SQLException {
        Map<Long, List<Option>> opts = new HashMap<>();
        String osql = "SELECT o.id, o.question_id, o.option_no, o.option_text, o.is_correct FROM options o "
                + "JOIN questions q ON q.id = o.question_id WHERE q.exam_id = ? ORDER BY o.question_id, o.option_no";
        try (PreparedStatement ps = c.prepareStatement(osql)) {
            ps.setLong(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    opts.computeIfAbsent(rs.getLong("question_id"), k -> new ArrayList<>())
                        .add(new Option(rs.getLong("id"), rs.getInt("option_no"), rs.getString("option_text"),
                                includeCorrect && rs.getBoolean("is_correct")));
                }
            }
        }
        List<Question> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT id, exam_id, question_text, marks FROM questions WHERE exam_id = ? ORDER BY id")) {
            ps.setLong(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    out.add(new Question(id, examId, rs.getString("question_text"), rs.getInt("marks"),
                            opts.getOrDefault(id, List.of())));
                }
            }
        }
        return out;
    }

    public static long examIdOfQuestion(Connection c, long questionId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT exam_id FROM questions WHERE id = ?")) {
            ps.setLong(1, questionId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getLong(1) : -1; }
        }
    }

    public static int countForExam(Connection c, long examId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM questions WHERE exam_id = ?")) {
            ps.setLong(1, examId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    public static void insert(Connection c, long examId, String text, int marks, List<String> optionTexts, int correctNo) throws SQLException {
        long qid;
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO questions (exam_id, question_text, marks) VALUES (?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, examId);
            ps.setString(2, text);
            ps.setInt(3, marks);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { k.next(); qid = k.getLong(1); }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO options (question_id, option_no, option_text, is_correct) VALUES (?,?,?,?)")) {
            for (int i = 0; i < optionTexts.size(); i++) {
                ps.setLong(1, qid);
                ps.setInt(2, i + 1);
                ps.setString(3, optionTexts.get(i));
                ps.setBoolean(4, i + 1 == correctNo);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public static void update(Connection c, long qid, String text, int marks, List<String> optionTexts, int correctNo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE questions SET question_text = ?, marks = ? WHERE id = ?")) {
            ps.setString(1, text);
            ps.setInt(2, marks);
            ps.setLong(3, qid);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE options SET option_text = ?, is_correct = ? WHERE question_id = ? AND option_no = ?")) {
            for (int i = 0; i < optionTexts.size(); i++) {
                ps.setString(1, optionTexts.get(i));
                ps.setBoolean(2, i + 1 == correctNo);
                ps.setLong(3, qid);
                ps.setInt(4, i + 1);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public static void delete(Connection c, long qid) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM questions WHERE id = ?")) {
            ps.setLong(1, qid);
            ps.executeUpdate();
        }
    }
}
