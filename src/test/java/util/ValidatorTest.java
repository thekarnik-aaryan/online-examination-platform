package util;

import exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorTest {
    @Test void acceptsValidUsername() { assertEquals("john_doe.1", Validator.username(" john_doe.1 ")); }

    @Test void rejectsBadUsernames() {
        for (String bad : new String[]{"", "ab", "has space", "x' OR '1'='1", "a;DROP TABLE users", "x".repeat(31), null})
            assertThrows(ValidationException.class, () -> Validator.username(bad), String.valueOf(bad));
    }

    @Test void passwordRules() {
        assertDoesNotThrow(() -> Validator.password("Abcdef12"));
        for (String bad : new String[]{null, "short1", "onlyletters", "12345678", "a1".repeat(33)})
            assertThrows(ValidationException.class, () -> Validator.password(bad));
    }

    @Test void textBoundaries() {
        assertEquals("abc", Validator.text("T", " abc ", 3, 5));
        assertThrows(ValidationException.class, () -> Validator.text("T", "ab", 3, 5));
        assertThrows(ValidationException.class, () -> Validator.text("T", "abcdef", 3, 5));
        assertThrows(ValidationException.class, () -> Validator.text("T", null, 1, 5));
    }

    @Test void rangeBoundaries() {
        assertEquals(1, Validator.intRange("Duration", 1, 1, 300));
        assertEquals(300, Validator.intRange("Duration", 300, 1, 300));
        assertThrows(ValidationException.class, () -> Validator.intRange("Duration", 0, 1, 300));
        assertThrows(ValidationException.class, () -> Validator.intRange("Duration", 301, 1, 300));
        assertThrows(ValidationException.class, () -> Validator.parseInt("Marks", "abc", 1, 100));
        assertThrows(ValidationException.class, () -> Validator.parseInt("Marks", "", 1, 100));
    }

    @Test void emailOptional() {
        assertNull(Validator.optionalEmail("  "));
        assertEquals("a@b.co", Validator.optionalEmail("a@b.co"));
        assertThrows(ValidationException.class, () -> Validator.optionalEmail("not-an-email"));
    }
}
