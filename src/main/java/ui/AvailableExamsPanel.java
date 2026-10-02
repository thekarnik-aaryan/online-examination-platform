package ui;

import model.Exam;
import service.AttemptService;

import java.util.List;

public class AvailableExamsPanel extends TablePanel<Exam> {
    public AvailableExamsPanel(Runnable onExamFinished) {
        super("Title", "Subject", "Minutes", "Questions", "Marks", "Closes");
        addButton("Start / resume exam", () -> {
            Exam e = selected();
            String instructions = "Exam: " + e.title() + "\n\n"
                    + "Duration: " + e.durationMinutes() + " minutes   Questions: " + e.questionCount()
                    + "   Total marks: " + e.totalMarks() + "\n\n"
                    + "- The timer starts now and cannot be paused.\n"
                    + "- Each answer is saved as soon as you select it.\n"
                    + "- The exam is submitted automatically when time runs out.\n"
                    + "- After submission answers cannot be changed. You get one attempt.\n\n"
                    + "Start the exam?";
            if (!Ui.confirm(this, instructions)) return;
            AttemptService.ExamSession session = AttemptService.startExam(e.id());
            new ExamFrame(session, () -> { refresh(); onExamFinished.run(); }).setVisible(true);
        });
        refresh();
    }

    @Override protected List<Exam> load() { return AttemptService.availableExams(); }

    @Override protected Object[] toRow(Exam e) {
        return new Object[]{e.title(), e.subjectName(), e.durationMinutes(), e.questionCount(), e.totalMarks(),
                e.endTime().format(Ui.FMT)};
    }
}
