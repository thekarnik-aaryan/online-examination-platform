package service;

import model.Option;
import model.Question;

import java.util.List;
import java.util.Map;

/** Pure evaluation logic (no I/O) so it can be unit-tested. No negative marking. */
public final class Grader {
    public record Score(int score, int total, double percentage, int correct, int incorrect, int unattempted) { }
    private Grader() {}

    /** @param answers questionId -> chosen optionId (missing = unattempted) */
    public static Score grade(List<Question> questions, Map<Long, Long> answers) {
        int score = 0, total = 0, correct = 0, incorrect = 0, unattempted = 0;
        for (Question q : questions) {
            total += q.marks();
            Long chosen = answers.get(q.id());
            if (chosen == null) { unattempted++; continue; }
            boolean right = false;
            for (Option o : q.options()) {
                if (o.id() == chosen && o.correct()) { right = true; break; }
            }
            if (right) { correct++; score += q.marks(); } else { incorrect++; }
        }
        double pct = total == 0 ? 0 : Math.round(score * 10000.0 / total) / 100.0;
        return new Score(score, total, pct, correct, incorrect, unattempted);
    }
}
