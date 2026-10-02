package security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {
    @Test void hashVerifiesAndIsNotPlaintext() {
        String h = PasswordHasher.hash("Secret123");
        assertNotEquals("Secret123", h);
        assertTrue(h.startsWith("$2"));
        assertTrue(PasswordHasher.verify("Secret123", h));
        assertFalse(PasswordHasher.verify("secret123", h));
    }

    @Test void saltsDiffer() { assertNotEquals(PasswordHasher.hash("Secret123"), PasswordHasher.hash("Secret123")); }

    @Test void malformedHashAndNullsAreRejected() {
        assertFalse(PasswordHasher.verify("x", "not-a-hash"));
        assertFalse(PasswordHasher.verify(null, "x"));
        assertFalse(PasswordHasher.verify("x", null));
    }

    @Test void seededDemoHashesMatchDocumentedPasswords() {
        assertTrue(PasswordHasher.verify("Admin@12345", "$2b$12$3MTYM8Shf405YDjOxJy8.eubeJ2feKfwPmla.hf8WmqzMha25G0h6"));
        assertTrue(PasswordHasher.verify("Faculty@12345", "$2b$12$jZvLlLP2BESa2ZgSMbqJ3urKscVbGFLp1r0Du3jMxwMAEUhBN3Yjq"));
        assertTrue(PasswordHasher.verify("Student@12345", "$2b$12$6zAjfNF50ouFnYrlW9Ms.eeCTsJzojAZMk8XZ6Gm5nWNBurlRuPki"));
    }
}
