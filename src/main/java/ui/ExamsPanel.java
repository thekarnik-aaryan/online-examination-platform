package ui;

import model.Exam;
import model.Role;
import model.Subject;
import model.User;
import exception.ValidationException;
import security.Session;
import service.ExamService;
import service.UserService;
import util.Validator;

import javax.swing.*;
import java.time.LocalDateTime;
import java.util.List;

public class ExamsPanel extends TablePanel<Exam> {
    public ExamsPanel() {
        super("Title", "Subject", "Faculty", "Minutes", "Starts", "Ends", "Questions", "Marks", "Published");
        addButton("Add exam", () -> edit(null));
        addButton("Edit", () -> edit(selected()));
        addButton("Questions", () -> {
            Exam e = selected();
            new QuestionsDialog(SwingUtilities.getWindowAncestor(this), e).setVisible(true);
            refresh();
        });
        addButton("Delete", this::delete);
        refresh();
    }

    @Override protected List<Exam> load() { return ExamService.exams(); }

    @Override protected Object[] toRow(Exam e) {
        return new Object[]{e.title(), e.subjectName(), e.facultyName(), e.durationMinutes(),
                e.startTime().format(Ui.FMT), e.endTime().format(Ui.FMT), e.questionCount(), e.totalMarks(),
                e.published() ? "Yes" : "No"};
    }

    private void edit(Exam e) {
        List<Subject> subjects = ExamService.subjects();
        if (subjects.isEmpty()) throw new ValidationException("An administrator must create a subject first.");
        boolean admin = Session.requireUser().role() == Role.ADMIN;

        FormPanel f = new FormPanel();
        JComboBox<Subject> subject = f.row("Subject", new JComboBox<Subject>(subjects.toArray(new Subject[0])));
        JComboBox<User> faculty = null;
        if (admin) {
            List<User> fac = UserService.listFaculty();
            if (fac.isEmpty()) throw new ValidationException("Create a faculty user first.");
            faculty = f.row("Faculty", new JComboBox<User>(fac.toArray(new User[0])));
        }
        JTextField title = f.text("Title", 28, e == null ? "" : e.title());
        JTextField desc = f.text("Description", 28, e == null ? "" : e.description());
        JTextField minutes = f.text("Duration (minutes)", 8, e == null ? "30" : String.valueOf(e.durationMinutes()));
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        JTextField start = f.text("Starts (yyyy-MM-dd HH:mm)", 16, (e == null ? now : e.startTime()).format(Ui.FMT));
        JTextField end = f.text("Ends (yyyy-MM-dd HH:mm)", 16, (e == null ? now.plusDays(7) : e.endTime()).format(Ui.FMT));
        JCheckBox published = f.row("Published", new JCheckBox());
        JCheckBox randomize = f.row("Randomize question order", new JCheckBox());
        published.setSelected(e != null && e.published());
        randomize.setSelected(e == null || e.randomize());
        if (e != null) {
            for (int i = 0; i < subject.getItemCount(); i++) if (subject.getItemAt(i).id() == e.subjectId()) subject.setSelectedIndex(i);
            if (faculty != null) for (int i = 0; i < faculty.getItemCount(); i++) if (faculty.getItemAt(i).id() == e.facultyId()) faculty.setSelectedIndex(i);
        }
        final JComboBox<User> facultyBox = faculty;
        boolean saved = Ui.form(this, e == null ? "Add exam" : "Edit exam", f, () -> {
            Subject s = (Subject) subject.getSelectedItem();
            long facultyId = facultyBox == null ? 0 : ((User) facultyBox.getSelectedItem()).id();
            ExamService.saveExam(e == null ? 0 : e.id(), s.id(), facultyId, title.getText(), desc.getText(),
                    Validator.parseInt("Duration", minutes.getText(), 1, 300),
                    Ui.parseDateTime("Start", start.getText()), Ui.parseDateTime("End", end.getText()),
                    published.isSelected(), randomize.isSelected());
        });
        if (saved) refresh();
    }

    private void delete() {
        Exam e = selected();
        if (Ui.confirm(this, "Delete exam '" + e.title() + "' and its questions?")) {
            ExamService.deleteExam(e.id());
            refresh();
        }
    }
}
