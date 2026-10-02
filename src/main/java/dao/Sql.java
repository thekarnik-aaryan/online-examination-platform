package dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/** Small JDBC helpers shared by DAOs. */
final class Sql {
    private Sql() {}
    static Timestamp ts(LocalDateTime t) { return t == null ? null : Timestamp.valueOf(t); }
    static LocalDateTime ldt(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toLocalDateTime();
    }
}
