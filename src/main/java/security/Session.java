package security;

import exception.AuthException;
import model.User;

/** Holds the single logged-in user of this desktop process. */
public final class Session {
    private static volatile User current;
    private Session() {}

    public static void start(User user) { current = user; }
    public static void end() { current = null; }
    public static User current() { return current; }

    public static User requireUser() {
        User u = current;
        if (u == null) throw new AuthException("Please log in to continue.");
        return u;
    }
}
