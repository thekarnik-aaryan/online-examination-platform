package dao;

import model.Role;
import model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** JDBC access for users. Contains no business rules. */
public final class UserDao {
    public record Credentials(User user, String hash, int failedAttempts, LocalDateTime lockedUntil) { }

    private static final String COLS = "id, username, full_name, email, role, active";
    private UserDao() {}

    private static User map(ResultSet rs) throws SQLException {
        return new User(rs.getLong("id"), rs.getString("username"), rs.getString("full_name"),
                rs.getString("email"), Role.valueOf(rs.getString("role")), rs.getBoolean("active"));
    }

    public static Credentials findCredentials(Connection c, String username) throws SQLException {
        String sql = "SELECT " + COLS + ", password_hash, failed_attempts, locked_until FROM users WHERE username = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Credentials(map(rs), rs.getString("password_hash"),
                        rs.getInt("failed_attempts"), Sql.ldt(rs, "locked_until"));
            }
        }
    }

    public static String findHashById(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT password_hash FROM users WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        }
    }

    public static User findById(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + COLS + " FROM users WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public static List<User> list(Connection c, Role roleOrNull) throws SQLException {
        String sql = "SELECT " + COLS + " FROM users" + (roleOrNull == null ? "" : " WHERE role = ?") + " ORDER BY username";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            if (roleOrNull != null) ps.setString(1, roleOrNull.name());
            try (ResultSet rs = ps.executeQuery()) {
                List<User> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    public static long insert(Connection c, String username, String hash, String fullName, String email, Role role) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, full_name, email, role) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, fullName);
            ps.setString(4, email);
            ps.setString(5, role.name());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { k.next(); return k.getLong(1); }
        }
    }

    public static void update(Connection c, long id, String fullName, String email, Role role, boolean active) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, email = ?, role = ?, active = ? WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, role.name());
            ps.setBoolean(4, active);
            ps.setLong(5, id);
            ps.executeUpdate();
        }
    }

    public static void updatePassword(Connection c, long id, String hash) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET password_hash = ?, failed_attempts = 0, locked_until = NULL WHERE id = ?")) {
            ps.setString(1, hash);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public static void delete(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public static void recordFailure(Connection c, long id, int newCount, LocalDateTime lockUntil) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE users SET failed_attempts = ?, locked_until = ? WHERE id = ?")) {
            ps.setInt(1, newCount);
            ps.setTimestamp(2, Sql.ts(lockUntil));
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    public static void resetFailures(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE users SET failed_attempts = 0, locked_until = NULL WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}
