package security;

import at.favre.lib.crypto.bcrypt.BCrypt;

/** BCrypt hashing (cost 12). Plaintext passwords are never stored or logged. */
public final class PasswordHasher {
    private static final int COST = 12;
    private PasswordHasher() {}

    public static String hash(String password) {
        return BCrypt.withDefaults().hashToString(COST, password.toCharArray());
    }

    public static boolean verify(String password, String hash) {
        if (password == null || hash == null) return false;
        try {
            return BCrypt.verifyer().verify(password.toCharArray(), hash).verified;
        } catch (RuntimeException e) {
            return false; // malformed hash
        }
    }
}
