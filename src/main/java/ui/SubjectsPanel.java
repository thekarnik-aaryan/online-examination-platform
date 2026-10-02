package ui;

import model.Subject;
import service.ExamService;

import javax.swing.*;
import java.util.List;

public class SubjectsPanel extends TablePanel<Subject> {
    public SubjectsPanel() {
        super("Code", "Name");
        addButton("Add subject", () -> edit(null));
        addButton("Edit", () -> edit(selected()));
        addButton("Delete", this::delete);
        refresh();
    }

    @Override protected List<Subject> load() { return ExamService.subjects(); }

    @Override protected Object[] toRow(Subject s) { return new Object[]{s.code(), s.name()}; }

    private void edit(Subject s) {
        FormPanel f = new FormPanel();
        JTextField code = f.text("Code", 15, s == null ? "" : s.code());
        JTextField name = f.text("Name", 25, s == null ? "" : s.name());
        if (Ui.form(this, s == null ? "Add subject" : "Edit subject", f,
                () -> ExamService.saveSubject(s == null ? 0 : s.id(), code.getText(), name.getText()))) refresh();
    }

    private void delete() {
        Subject s = selected();
        if (Ui.confirm(this, "Delete subject '" + s.code() + "'?")) {
            ExamService.deleteSubject(s.id());
            refresh();
        }
    }
}
