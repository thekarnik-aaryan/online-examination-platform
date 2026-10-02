package service;

import config.Database;
import dao.AttemptDao;
import dao.ExamDao;
import dao.QuestionDao;
import dao.SubjectDao;
import exception.AccessDeniedException;
import exception.AppException;
import exception.ValidationException;
import model.*;
import security.Authz;
import util.Validator;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Subject, exam and question management for Admin and Faculty. */
public final class ExamService {
    public static final int MAX_QUESTIONS = 100;
    private ExamService() {}

    // ---------- subjects ----------
    public static List<Subject> subjects() {
        Authz.require(Role.ADMIN, Role.FACULTY);
        return Database.read(SubjectDao::list);
    }

    public static void saveSubject(long id, String code, String name) {
        Authz.require(Role.ADMIN);
        String cd = Validator.text("Subject code", code, 2, 20).toUpperCase(Locale.ROOT);
        String nm = Validator.text("Subject name", name, 2, 100);
        Database.tx(c -> {
            if (id == 0) SubjectDao.insert(c, cd, nm); else SubjectDao.update(c, id, cd, nm);
            return null;
        });
        AuditService.log(id == 0 ? "SUBJECT_CREATE" : "SUBJECT_UPDATE", "code=" + cd);
    }

    public static void deleteSubject(long id) {
        Authz.require(Role.ADMIN);
        Database.tx(c -> { SubjectDao.delete(c, id); return null; });
        AuditService.log("SUBJECT_DELETE", "subjectId=" + id);
    }

    // ---------- exams ----------
    public static List<Exam> exams() {
        User u = Authz.require(Role.ADMIN, Role.FACULTY);
        return Database.read(c -> ExamDao.list(c, u.role() == Role.ADMIN ? null : u.id()));
    }

    private static Exam requireManageable(Connection c, User u, long examId) throws SQLException {
        Exam e = ExamDao.find(c, examId);
        if (e == null) throw new ValidationException("Exam not found.");
        if (u.role() == Role.FACULTY && e.facultyId() != u.id()) {
            AuditService.log("ACCESS_DENIED", "faculty tried to manage exam " + examId);
            throw new AccessDeniedException("You can only manage exams assigned to you.");
        }
        return e;
    }

    public static void saveExam(long id, long subjectId, long facultyId, String title, String description,
                                int durationMinutes, LocalDateTime start, LocalDateTime end,
                                boolean published, boolean randomize) {
        User u = Authz.require(Role.ADMIN, Role.FACULTY);
        String t = Validator.text("Title", title, 3, 150);
        String d = Validator.text("Description", description == null ? "" : description, 0, 500);
        Validator.intRange("Duration (minutes)", durationMinutes, 1, 300);
        if (subjectId <= 0) throw new ValidationException("Select a subject.");
        if (start == null || end == null) throw new ValidationException("Start and end date/time are required.");
        if (!end.isAfter(start)) throw new ValidationException("End time must be after start time.");
        long owner = u.role() == Role.FACULTY ? u.id() : facultyId;
        if (owner <= 0) throw new ValidationException("Select the faculty member for this exam.");
        Database.tx(c -> {
            if (id != 0) {
                Exam old = requireManageable(c, u, id);
                if (published && old.questionCount() == 0) throw new ValidationException("Add at least one question before publishing.");
            } else if (published) {
                throw new ValidationException("Create the exam first, add questions, then publish it.");
            }
            Exam e = new Exam(id, subjectId, null, t, d, owner, null, durationMinutes, start, end, published, randomize, 0, 0);
            if (id == 0) ExamDao.insert(c, e); else ExamDao.update(c, e);
            return null;
        });
        AuditService.log(id == 0 ? "EXAM_CREATE" : "EXAM_UPDATE", "title=" + t + ", published=" + published);
    }

    public static void deleteExam(long id) {
        User u = Authz.require(Role.ADMIN, Role.FACULTY);
        Database.tx(c -> {
            requireManageable(c, u, id);
            if (AttemptDao.examHasAttempts(c, id))
                throw new AppException("This exam has student attempts and cannot be deleted. Unpublish it instead.");
            ExamDao.delete(c, id);
            return null;
        });
        AuditService.log("EXAM_DELETE", "examId=" + id);
    }

    // ---------- questions ----------
    public static List<Question> questions(long examId) {
        User u = Authz.require(Role.ADMIN, Role.FACULTY);
        return Database.read(c -> {
            requireManageable(c, u, examId);
            return QuestionDao.listByExam(c, examId, true);
        });
    }

    public static void saveQuestion(long examId, long questionId, String text, int marks, List<String> options, int correctNo) {
        User u = Authz.require(Role.ADMIN, Role.FACULTY);
        String q = Validator.text("Question", text, 3, 1000);
        Validator.intRange("Marks", marks, 1, 100);
        if (options == null || options.size() != 4) throw new ValidationException("Exactly 4 options are required.");
        List<String> clean = options.stream().map(o -> Validator.text("Option", o, 1, 300)).toList();
        Set<String> seen = new HashSet<>();
        for (String o : clean) {
            if (!seen.add(o.toLowerCase(Locale.ROOT))) throw new ValidationException("Options must be different from each other.");
        }
        Validator.intRange("Correct option", correctNo, 1, 4);
        Database.tx(c -> {
            requireManageable(c, u, examId);
            if (AttemptDao.examHasAttempts(c, examId))
                throw new AppException("Questions cannot be changed after students have attempted this exam.");
            if (questionId == 0) {
                if (QuestionDao.countForExam(c, examId) >= MAX_QUESTIONS)
                    throw new ValidationException("An exam can have at most " + MAX_QUESTIONS + " questions.");
                QuestionDao.insert(c, examId, q, marks, clean, correctNo);
            } else {
                if (QuestionDao.examIdOfQuestion(c, questionId) != examId) throw new ValidationException("Question not found in this exam.");
                QuestionDao.update(c, questionId, q, marks, clean, correctNo);
            }
            return null;
        });
        AuditService.log(questionId == 0 ? "QUESTION_CREATE" : "QUESTION_UPDATE", "examId=" + examId);
    }

    public static void deleteQuestion(long examId, long questionId) {
        User u = Authz.require(Role.ADMIN, Role.FACULTY);
        Database.tx(c -> {
            Exam e = requireManageable(c, u, examId);
            if (AttemptDao.examHasAttempts(c, examId))
                throw new AppException("Questions cannot be changed after students have attempted this exam.");
            if (QuestionDao.examIdOfQuestion(c, questionId) != examId) throw new ValidationException("Question not found in this exam.");
            if (e.published() && e.questionCount() <= 1) throw new ValidationException("Unpublish the exam before deleting its last question.");
            QuestionDao.delete(c, questionId);
            return null;
        });
        AuditService.log("QUESTION_DELETE", "examId=" + examId + ", questionId=" + questionId);
    }

    // ---------- results (read-only) ----------
    public static List<Result> results() {
        User u = Authz.require(Role.ADMIN, Role.FACULTY);
        return Database.read(c -> u.role() == Role.ADMIN
                ? dao.AttemptDao.allResults(c) : dao.AttemptDao.resultsForFaculty(c, u.id()));
    }
}
