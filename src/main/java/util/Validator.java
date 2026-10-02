package util;

import exception.ValidationException;

import java.util.regex.Pattern;

/** Central input validation with sensible length/range limits. */
public final class Validator {
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_.]{3,30}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private Validator() {}

    public static String text(String label, String value, int min, int max) {
        String v = value == null ? "" : value.trim();
        if (v.length() < min || v.length() > max) {
            throw new ValidationException(label + " must be " + (min == max ? "exactly " + min : min + " to " + max) + " characters.");
        }
        return v;
    }

    public static String username(String value) {
        String v = value == null ? "" : value.trim();
        if (!USERNAME.matcher(v).matches())
            throw new ValidationException("Username must be 3-30 characters: letters, digits, '_' or '.'.");
        return v;
    }

    public static String password(String value) {
        if (value == null || value.length() < 8 || value.length() > 64)
            throw new ValidationException("Password must be 8 to 64 characters.");
        boolean letter = value.chars().anyMatch(Character::isLetter);
        boolean digit = value.chars().anyMatch(Character::isDigit);
        if (!letter || !digit) throw new ValidationException("Password must contain at least one letter and one digit.");
        return value;
    }

    public static String optionalEmail(String value) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) return null;
        if (v.length() > 100 || !EMAIL.matcher(v).matches()) throw new ValidationException("Email address is not valid.");
        return v;
    }

    public static int intRange(String label, int value, int min, int max) {
        if (value < min || value > max) throw new ValidationException(label + " must be between " + min + " and " + max + ".");
        return value;
    }

    public static int parseInt(String label, String value, int min, int max) {
        try {
            return intRange(label, Integer.parseInt(value == null ? "" : value.trim()), min, max);
        } catch (NumberFormatException e) {
            throw new ValidationException(label + " must be a whole number.");
        }
    }
}
