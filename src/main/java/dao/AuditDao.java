package dao;

import model.AuditEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class AuditDao {
    private AuditDao() {}

    public static void insert(Connection c, Long userIdOrNull, String username, String action, String details) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO audit_logs (user_id, username, action, details) VALUES (?,?,?,?)")) {
            if (userIdOrNull == null) ps.setNull(1, Types.BIGINT); else ps.setLong(1, userIdOrNull);
            ps.setString(2, username);
            ps.setString(3, action);
            ps.setString(4, details);
            ps.executeUpdate();
        }
    }

    public static List<AuditEntry> latest(Connection c, int limit) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, created_at, username, action, details FROM audit_logs ORDER BY id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<AuditEntry> out = new ArrayList<>();
                while (rs.next()) out.add(new AuditEntry(rs.getLong(1), Sql.ldt(rs, "created_at"),
                        rs.getString(3), rs.getString(4), rs.getString(5)));
                return out;
            }
        }
    }
}
