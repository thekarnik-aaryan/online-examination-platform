package config;

import exception.AppException;
import util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Connection factory plus helpers that guarantee closing, rollback and safe error translation. */
public final class Database {
    @FunctionalInterface
    public interface SqlFn<T> { T apply(Connection c) throws SQLException; }

    private Database() {}

    private static Connection open() throws SQLException {
        return DriverManager.getConnection(
                AppConfig.get("db.url", "EXAM_DB_URL", "jdbc:mysql://localhost:3306/online_exam"),
                AppConfig.get("db.user", "EXAM_DB_USER", ""),
                AppConfig.get("db.password", "EXAM_DB_PASSWORD", ""));
    }

    /** Runs a unit of work with auto-commit (reads or single statements). */
    public static <T> T read(SqlFn<T> fn) {
        try (Connection c = open()) {
            return fn.apply(c);
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    /** Runs a unit of work in one transaction: commit on success, rollback on any failure. */
    public static <T> T tx(SqlFn<T> fn) {
        try (Connection c = open()) {
            c.setAutoCommit(false);
            try {
                T result = fn.apply(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException re) { Log.error("Rollback failed", re); }
                throw e;
            }
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    public static AppException translate(SQLException e) {
        Log.error("Database error (state=" + e.getSQLState() + ", code=" + e.getErrorCode() + ")", e);
        if (e.getErrorCode() == 1062) return new AppException("That value already exists (duplicate).");
        if (e.getErrorCode() == 1451) return new AppException("Cannot delete: other records depend on this item.");
        if (e.getErrorCode() == 1452) return new AppException("Invalid reference to a related record.");
        if (e.getErrorCode() == 3819 || e.getErrorCode() == 4025) return new AppException("The value violates a data rule.");
        if ("45000".equals(e.getSQLState())) return new AppException("Operation blocked by data-integrity rules.");
        return new AppException("A database error occurred. Please try again or contact the administrator.");
    }
}
