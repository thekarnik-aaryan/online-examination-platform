package service;

import model.Option;
import model.Question;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GraderTest {
    private static Question q(long id, int marks, long correctOptionId) {
        return new Question(id, 1, "Q" + id, marks, List.of(
                new Option(id * 10 + 1, 1, "a", correctOptionId == id * 10 + 1),
                new Option(id * 10 + 2, 2, "b", correctOptionId == id * 10 + 2)));
    }

    private final List<Question> qs = List.of(q(1, 2, 11), q(2, 3, 22), q(3, 5, 31));

    @Test void mixedAnswers() {
        Grader.Score s = Grader.grade(qs, Map.of(1L, 11L, 2L, 21L));
        assertEquals(2, s.score());
        assertEquals(10, s.total());
        assertEquals(1, s.correct());
        assertEquals(1, s.incorrect());
        assertEquals(1, s.unattempted());
        assertEquals(20.0, s.percentage());
    }

    @Test void allCorrectAndNoneAnswered() {
        assertEquals(100.0, Grader.grade(qs, Map.of(1L, 11L, 2L, 22L, 3L, 31L)).percentage());
        Grader.Score none = Grader.grade(qs, Map.of());
        assertEquals(0, none.score());
        assertEquals(3, none.unattempted());
    }

    @Test void answerForAnotherQuestionsOptionIsWrong() {
        assertEquals(1, Grader.grade(qs, Map.of(1L, 22L)).incorrect());
    }

    @Test void emptyExamDoesNotDivideByZero() {
        assertEquals(0.0, Grader.grade(List.of(), Map.of()).percentage());
    }
}
