package service;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import config.Database;
import dao.AttemptDao;
import dao.ExamDao;
import dao.QuestionDao;
import exception.AccessDeniedException;
import exception.AppException;
import model.Attempt;
import model.Exam;
import model.Question;
import model.Result;
import model.Role;
import model.User;
import security.Authz;
import util.Log;

/** Student exam flow: eligibility, start, answer saving, submission and evaluation. */
public final class AttemptService {
    public record ExamSession(Attempt attempt, Exam exam, List<Question> questions, Map<Long, Long> answers) { }

    private static final SecureRandom RANDOM = new SecureRandom();
    private AttemptService() {}

    public static List<Exam> availableExams() {
        User me = Authz.require(Role.STUDENT);
        autoSubmitExpired(me);
        return Database.read(c -> ExamDao.availableFor(c, me.id(), LocalDateTime.now()));
    }

    public static List<Result> myResults() {
        User me = Authz.require(Role.STUDENT);
        autoSubmitExpired(me);
        return Database.read(c -> AttemptDao.resultsForStudent(c, me.id()));
    }

    /** Evaluates any attempt of this student whose deadline has passed (e.g. the app was closed mid-exam). */
    private static void autoSubmitExpired(User me) {
        List<Long> ids = Database.read(c -> AttemptDao.expiredInProgress(c, me.id(), LocalDateTime.now()));
        for (long id : ids) {
            try {
                submit(id, true);
            } catch (AppException e) {
                Log.warn("Auto-submit of attempt " + id + " skipped: " + e.getMessage());
            }
        }
    }

    public static ExamSession startExam(long examId) {
        User me = Authz.require(Role.STUDENT);
        AtomicBoolean created = new AtomicBoolean(false);
        ExamSession session = Database.tx(c -> {
            LocalDateTime now = LocalDateTime.now();
            Exam exam = ExamDao.find(c, examId);
            if (exam == null || !exam.published()) throw new AppException("This exam is not available.");
            if (now.isBefore(exam.startTime())) throw new AppException("This exam has not started yet.");
            Attempt a = AttemptDao.findByExamAndStudent(c, examId, me.id(), true);
            if (a != null && !a.inProgress()) throw new AppException("You have already completed this exam.");
            if (a != null && !now.isBefore(a.deadline())) {
                finalizeAttempt(c, a, true, now);
                return null; // reported to the user after the commit below
            }
            if (a == null && now.isAfter(exam.endTime())) throw new AppException("This exam has ended.");

            List<Question> masked = QuestionDao.listByExam(c, examId, false);
            if (masked.isEmpty()) throw new AppException("This exam has no questions yet.");
            if (a == null) {
                List<Long> ids = masked.stream().map(Question::id).collect(Collectors.toList());
                if (exam.randomize()) Collections.shuffle(ids, RANDOM);
                LocalDateTime deadline = now.plusMinutes(exam.durationMinutes());
                if (deadline.isAfter(exam.endTime())) deadline = exam.endTime();
                String order = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
                long id = AttemptDao.create(c, examId, me.id(), now, deadline, order);
                a = AttemptDao.findById(c, id, false);
                created.set(true);
            }
            Map<Long, Question> byId = new HashMap<>();
            for (Question q : masked) byId.put(q.id(), q);
            List<Question> ordered = new ArrayList<>();
            for (String s : a.questionOrder().split(",")) {
                Question q = byId.get(Long.parseLong(s));
                if (q != null) ordered.add(q);
            }
            return new ExamSession(a, exam, ordered, AttemptDao.loadAnswers(c, a.id()));
        });
        if (session == null) throw new AppException("Your time for this exam had already ended. It was submitted automatically - see My Results.");
        if (created.get()) AuditService.log("EXAM_START", "examId=" + examId + ", attemptId=" + session.attempt().id());
        return session;
    }

    public static void saveAnswer(long attemptId, long questionId, Long optionIdOrNull) {
        User me = Authz.require(Role.STUDENT);
        Database.tx(c -> {
            Attempt a = AttemptDao.findById(c, attemptId, true);
            requireOwn(a, me);
            if (!a.inProgress() || !LocalDateTime.now().isBefore(a.deadline()))
                throw new AppException("Time is over or the exam was already submitted. Answers can no longer be changed.");
            boolean valid = optionIdOrNull == null
                    ? AttemptDao.questionBelongs(c, a.examId(), questionId)
                    : AttemptDao.optionBelongs(c, a.examId(), questionId, optionIdOrNull);
            if (!valid) throw new AppException("Invalid answer for this exam.");
            AttemptDao.saveAnswer(c, attemptId, questionId, optionIdOrNull);
            return null;
        });
    }

    /** Evaluates and stores the result atomically. Safe against double submission. */
    public static Result submit(long attemptId, boolean auto) {
        User me = Authz.require(Role.STUDENT);
        Result r = Database.tx(c -> {
            Attempt a = AttemptDao.findById(c, attemptId, true);
            requireOwn(a, me);
            if (!a.inProgress()) throw new AppException("This exam has already been submitted.");
            return finalizeAttempt(c, a, auto, LocalDateTime.now());
        });
        AuditService.log(auto ? "EXAM_AUTO_SUBMIT" : "EXAM_SUBMIT",
                "attemptId=" + attemptId + ", score=" + r.score() + "/" + r.totalMarks());
        return r;
    }

    private static void requireOwn(Attempt a, User me) {
        if (a == null || a.studentId() != me.id()) {
            AuditService.log("ACCESS_DENIED", "attempt not owned by user");
            throw new AccessDeniedException("This attempt does not belong to you.");
        }
    }

    private static Result finalizeAttempt(Connection c, Attempt a, boolean auto, LocalDateTime now) throws SQLException {
        List<Question> questions = QuestionDao.listByExam(c, a.examId(), true);
        Grader.Score s = Grader.grade(questions, AttemptDao.loadAnswers(c, a.id()));
        AttemptDao.insertResult(c, a.id(), s.score(), s.total(), s.percentage(), s.correct(), s.incorrect(), s.unattempted());
        boolean late = auto || now.isAfter(a.deadline());
        AttemptDao.markSubmitted(c, a.id(), late ? "AUTO_SUBMITTED" : "SUBMITTED", now);
        return AttemptDao.resultOf(c, a.id());
    }


public static void reportViolation(long attemptId, String type) {
        User me = Authz.require(Role.STUDENT);
        Attempt a = Database.read(c -> AttemptDao.findById(c, attemptId, false));
        requireOwn(a, me);
        if (a.inProgress()) AuditService.log("EXAM_VIOLATION", "attemptId=" + attemptId + ", type=" + type);
    }
}
