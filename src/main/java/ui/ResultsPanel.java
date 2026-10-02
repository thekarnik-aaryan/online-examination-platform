package ui;

import model.Result;

import java.util.List;
import java.util.function.Supplier;

/** Read-only results table; used by all roles with a different data supplier. */
public class ResultsPanel extends TablePanel<Result> {
    private final Supplier<List<Result>> source;

    public ResultsPanel(Supplier<List<Result>> source) {
        super("Exam", "Student", "Username", "Score", "Total", "Percent", "Correct", "Incorrect", "Unattempted", "Submitted");
        this.source = source;
        refresh();
    }

    @Override protected List<Result> load() { return source.get(); }

    @Override protected Object[] toRow(Result r) {
        return new Object[]{r.examTitle(), r.studentName(), r.username(), r.score(), r.totalMarks(),
                String.format("%.2f%%", r.percentage()), r.correct(), r.incorrect(), r.unattempted(),
                r.submittedAt() == null ? "" : r.submittedAt().format(Ui.FMT)};
    }
}
