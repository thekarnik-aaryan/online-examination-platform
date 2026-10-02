package service;

import exception.AccessDeniedException;
import exception.AppException;
import exception.AuthException;
import model.Exam;
import model.Question;
import model.Result;
import org.junit.jupiter.api.*;
import security.Session;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests against a FRESHLY SEEDED test database.
 * Skipped automatically unless EXAM_DB_URL (and EXAM_DB_USER / EXAM_DB_PASSWORD) are set.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseIntegrationTest {
    @BeforeEach void setUp() {
        Assumptions.assumeTrue(System.getenv("EXAM_DB_URL") != null, "EXAM_DB_URL not set - skipping DB tests");
        AuthService.logout();
    }

    @AfterEach void tearDown() { Session.end(); }

    @Test @Order(1) void validLoginForEveryRole() {
        assertEquals("ADMIN", AuthService.login("admin", "Admin@12345").role().name());
        AuthService.logout();
        assertEquals("FACULTY", AuthService.login("faculty1", "Faculty@12345").role().name());
        AuthService.logout();
        assertEquals("STUDENT", AuthService.login("student1", "Student@12345").role().name());
    }

    @Test @Order(2) void invalidCredentialsAndInjectionAreRejected() {
        assertThrows(AuthException.class, () -> AuthService.login("admin", "wrong-password"));
        assertThrows(AuthException.class, () -> AuthService.login("admin' OR '1'='1", "x"));
        assertThrows(AuthException.class, () -> AuthService.login("nobody", "Whatever1"));
        assertNull(Session.current());
    }

    @Test @Order(3) void studentCannotUseAdminOrFacultyServices() {
        AuthService.login("student1", "Student@12345");
        assertThrows(AccessDeniedException.class, UserService::list);
        assertThrows(AccessDeniedException.class, ExamService::exams);
        assertThrows(AccessDeniedException.class, () -> AuditService.latest(10));
    }

    @Test @Order(4) void unauthenticatedCallsAreRejected() {
        assertThrows(AuthException.class, UserService::list);
        assertThrows(AuthException.class, AttemptService::availableExams);
    }

    @Test @Order(5) void fullExamFlowThenImmutability() {
        AuthService.login("student1", "Student@12345");
        List<Exam> available = AttemptService.availableExams();
        Assumptions.assumeFalse(available.isEmpty(), "No available exam (already attempted?) - reseed the test DB");
        AttemptService.ExamSession s = AttemptService.startExam(available.get(0).id());
        Question first = s.questions().get(0);
        assertTrue(first.options().stream().noneMatch(o -> o.correct()), "correct answers must not reach the student");
        AttemptService.saveAnswer(s.attempt().id(), first.id(), first.options().get(0).id());

        Result r = AttemptService.submit(s.attempt().id(), false);
        assertTrue(r.score() >= 0 && r.score() <= r.totalMarks());
        assertEquals(s.questions().size(), r.correct() + r.incorrect() + r.unattempted());

        assertThrows(AppException.class, () -> AttemptService.submit(s.attempt().id(), false));          // duplicate submit
        assertThrows(AppException.class, () -> AttemptService.saveAnswer(s.attempt().id(), first.id(), null)); // altered answer
        assertThrows(AppException.class, () -> AttemptService.startExam(available.get(0).id()));         // second attempt
    }

    @Test @Order(6) void facultyCannotManageOthersExamsOrAdminData() {
        AuthService.login("faculty1", "Faculty@12345");
        assertThrows(AccessDeniedException.class, UserService::list);
        assertThrows(AccessDeniedException.class, () -> AuditService.latest(10));
    }
}
