package service;

import config.Database;
import dao.SettingsDao;
import dao.UserDao;
import exception.AppException;
import exception.AuthException;
import exception.ValidationException;
import model.Role;
import model.User;
import security.Authz;
import security.PasswordHasher;
import security.Session;
import util.Validator;

import java.util.List;

public final class UserService {
    private UserService() {}

    public static List<User> list() {
        Authz.require(Role.ADMIN);
        return Database.read(c -> UserDao.list(c, null));
    }

    public static List<User> listFaculty() {
        Authz.require(Role.ADMIN);
        return Database.read(c -> UserDao.list(c, Role.FACULTY));
    }

    public static void create(String username, String password, String fullName, String email, Role role) {
        Authz.require(Role.ADMIN);
        String u = Validator.username(username);
        Validator.password(password);
        String name = Validator.text("Full name", fullName, 2, 100);
        String mail = Validator.optionalEmail(email);
        if (role == null) throw new ValidationException("Select a role.");
        String hash = PasswordHasher.hash(password);
        Database.tx(c -> {
            if (UserDao.findCredentials(c, u) != null) throw new ValidationException("That username is already taken.");
            return UserDao.insert(c, u, hash, name, mail, role);
        });
        AuditService.log("USER_CREATE", "username=" + u + ", role=" + role);
    }

    public static void update(long id, String fullName, String email, Role role, boolean active) {
        User me = Authz.require(Role.ADMIN);
        String name = Validator.text("Full name", fullName, 2, 100);
        String mail = Validator.optionalEmail(email);
        if (role == null) throw new ValidationException("Select a role.");
        if (id == me.id() && (!active || role != Role.ADMIN))
            throw new ValidationException("You cannot deactivate or demote your own account.");
        Database.tx(c -> {
            if (UserDao.findById(c, id) == null) throw new ValidationException("User not found.");
            UserDao.update(c, id, name, mail, role, active);
            return null;
        });
        AuditService.log("USER_UPDATE", "userId=" + id + ", role=" + role + ", active=" + active);
    }

    public static void resetPassword(long id, String newPassword) {
        Authz.require(Role.ADMIN);
        Validator.password(newPassword);
        String hash = PasswordHasher.hash(newPassword);
        Database.tx(c -> { UserDao.updatePassword(c, id, hash); return null; });
        AuditService.log("PASSWORD_RESET", "userId=" + id);
    }

    public static void delete(long id) {
        User me = Authz.require(Role.ADMIN);
        if (id == me.id()) throw new ValidationException("You cannot delete your own account.");
        Database.tx(c -> { UserDao.delete(c, id); return null; });
        AuditService.log("USER_DELETE", "userId=" + id);
    }

    public static void setRegistrationEnabled(boolean enabled) {
        Authz.require(Role.ADMIN);
        Database.tx(c -> { SettingsDao.set(c, "student_registration_enabled", String.valueOf(enabled)); return null; });
        AuditService.log("SETTING_CHANGE", "student_registration_enabled=" + enabled);
    }

    public static void changeOwnPassword(String oldPassword, String newPassword) {
        User me = Session.requireUser();
        Validator.password(newPassword);
        String hash = PasswordHasher.hash(newPassword);
        Database.tx(c -> {
            String current = UserDao.findHashById(c, me.id());
            if (!PasswordHasher.verify(oldPassword, current)) throw new AuthException("Current password is incorrect.");
            UserDao.updatePassword(c, me.id(), hash);
            return null;
        });
        AuditService.log("PASSWORD_CHANGE", null);
    }
}
