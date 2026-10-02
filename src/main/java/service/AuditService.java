package service;

import config.Database;
import dao.AuditDao;
import model.AuditEntry;
import model.Role;
import model.User;
import security.Authz;
import security.Session;
import util.Log;

import java.util.List;

public final class AuditService {
    private AuditService() {}

    /** Logs an event for the current session user (or anonymous). Never throws. */
    public static void log(String action, String details) {
        User u = Session.current();
        logFor(u == null ? null : u.id(), u == null ? null : u.username(), action, details);
    }

    public static void logFor(Long userId, String username, String action, String details) {
        String name = username == null ? null : (username.length() > 50 ? username.substring(0, 50) : username);
        String det = details == null ? null : (details.length() > 500 ? details.substring(0, 500) : details);
        try {
            Database.read(c -> { AuditDao.insert(c, userId, name, action, det); return null; });
        } catch (RuntimeException e) {
            Log.error("Audit write failed for action " + action, e);
        }
    }

    public static List<AuditEntry> latest(int limit) {
        Authz.require(Role.ADMIN);
        return Database.read(c -> AuditDao.latest(c, Math.max(1, Math.min(limit, 1000))));
    }
}
