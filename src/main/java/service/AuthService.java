package service;

import config.AppConfig;
import config.Database;
import dao.SettingsDao;
import dao.UserDao;
import exception.AuthException;
import exception.ValidationException;
import model.Role;
import model.User;
import security.PasswordHasher;
import security.Session;
import util.Validator;

import java.time.LocalDateTime;

public final class AuthService {
    private static final String GENERIC = "Invalid username or password.";
    private static String dummyHash; // used to equalize timing for unknown usernames
    private AuthService() {}

    private static synchronized String dummy() {
        if (dummyHash == null) dummyHash = PasswordHasher.hash("not-a-real-password-1");
        return dummyHash;
    }

    public static User login(String usernameIn, String password) {
        String username = usernameIn == null ? "" : usernameIn.trim();
        if (username.isEmpty() || password == null || password.isEmpty())
            throw new ValidationException("Enter both username and password.");
        if (username.length() > 30 || password.length() > 128) throw new AuthException(GENERIC);

        int max = AppConfig.getInt("security.maxFailedLogins", 5);
        int lockMinutes = AppConfig.getInt("security.lockMinutes", 5);
        LocalDateTime now = LocalDateTime.now();

        UserDao.Credentials cred = Database.read(c -> UserDao.findCredentials(c, username));
        if (cred == null) {
            PasswordHasher.verify(password, dummy());
            AuditService.logFor(null, username, "LOGIN_FAILURE", "unknown username");
            throw new AuthException(GENERIC);
        }
        User user = cred.user();
        if (cred.lockedUntil() != null && cred.lockedUntil().isAfter(now)) {
            AuditService.logFor(user.id(), username, "LOGIN_BLOCKED", "account temporarily locked");
            throw new AuthException("Too many failed attempts. Please try again later.");
        }
        boolean passwordOk = PasswordHasher.verify(password, cred.hash());
        if (!passwordOk) {
            final int failures = cred.failedAttempts() + 1;
            final boolean lock = failures >= max;
            final LocalDateTime until = lock ? now.plusMinutes(lockMinutes) : null;
            Database.read(c -> { UserDao.recordFailure(c, user.id(), lock ? 0 : failures, until); return null; });
            AuditService.logFor(user.id(), username, "LOGIN_FAILURE", lock ? "bad password; account locked" : "bad password");
            throw new AuthException(GENERIC);
        }
        if (!user.active()) {
            AuditService.logFor(user.id(), username, "LOGIN_FAILURE", "inactive account");
            throw new AuthException("This account is disabled. Contact the administrator.");
        }
        if (cred.failedAttempts() > 0 || cred.lockedUntil() != null) {
            Database.read(c -> { UserDao.resetFailures(c, user.id()); return null; });
        }
        Session.start(user);
        AuditService.log("LOGIN_SUCCESS", "role=" + user.role());
        return user;
    }

    public static void logout() {
        if (Session.current() != null) AuditService.log("LOGOUT", null);
        Session.end();
    }

    public static boolean isRegistrationEnabled() {
        return "true".equals(Database.read(c -> SettingsDao.get(c, "student_registration_enabled", "false")));
    }

    public static void registerStudent(String username, String password, String fullName, String email) {
        if (!isRegistrationEnabled()) throw new AuthException("Student registration is currently disabled.");
        String u = Validator.username(username);
        Validator.password(password);
        String name = Validator.text("Full name", fullName, 2, 100);
        String mail = Validator.optionalEmail(email);
        String hash = PasswordHasher.hash(password);
        long id = Database.tx(c -> {
            if (UserDao.findCredentials(c, u) != null) throw new ValidationException("That username is already taken.");
            return UserDao.insert(c, u, hash, name, mail, Role.STUDENT);
        });
        AuditService.logFor(id, u, "STUDENT_REGISTERED", "self-registration");
    }
}
