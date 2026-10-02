package dao;

import java.sql.*;

public final class SettingsDao {
    private SettingsDao() {}

    public static String get(Connection c, String key, String def) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT setting_value FROM settings WHERE setting_key = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : def; }
        }
    }

    public static void set(Connection c, String key, String value) throws SQLException {
        String sql = "INSERT INTO settings (setting_key, setting_value) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }
}
