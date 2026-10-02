package dao;

import model.Subject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class SubjectDao {
    private SubjectDao() {}

    public static List<Subject> list(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT id, code, name FROM subjects ORDER BY code");
             ResultSet rs = ps.executeQuery()) {
            List<Subject> out = new ArrayList<>();
            while (rs.next()) out.add(new Subject(rs.getLong(1), rs.getString(2), rs.getString(3)));
            return out;
        }
    }

    public static void insert(Connection c, String code, String name) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO subjects (code, name) VALUES (?, ?)")) {
            ps.setString(1, code);
            ps.setString(2, name);
            ps.executeUpdate();
        }
    }

    public static void update(Connection c, long id, String code, String name) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE subjects SET code = ?, name = ? WHERE id = ?")) {
            ps.setString(1, code);
            ps.setString(2, name);
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    public static void delete(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM subjects WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}
