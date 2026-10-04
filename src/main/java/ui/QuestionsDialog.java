package ui;

import model.Exam;
import model.Option;
import model.Question;
import service.ExamService;
import util.Validator;

import javax.swing.*;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import service.CsvQuestionImporter;

public class QuestionsDialog extends JDialog {
    public QuestionsDialog(Window owner, Exam exam) {
        super(owner, "Questions - " + exam.title(), ModalityType.APPLICATION_MODAL);
        setContentPane(new QuestionPanel(exam));
        setSize(900, 460);
        setLocationRelativeTo(owner);
    }

    private static class QuestionPanel extends TablePanel<Question> {
        private final Exam exam;

        QuestionPanel(Exam exam) {
            super("#", "Question", "Marks", "Correct option");
            this.exam = exam;
            addButton("Add question", () -> edit(null));
            addButton("Edit", () -> edit(selected()));
            addButton("Delete", this::delete);
            addButton("Download CSV template", this::saveTemplate);
            addButton("Import CSV", this::importCsv);
            refresh();
        }

        @Override protected List<Question> load() { return ExamService.questions(exam.id()); }

        @Override protected Object[] toRow(Question q) {
            String correct = q.options().stream().filter(Option::correct).map(o -> String.valueOf(o.optionNo()))
                    .findFirst().orElse("?");
            return new Object[]{rows.indexOf(q) + 1, q.text(), q.marks(), correct};
        }

        private void edit(Question q) {
            FormPanel f = new FormPanel();
            JTextArea text = new JTextArea(q == null ? "" : q.text(), 4, 40);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            f.row("Question", new JScrollPane(text));
            JTextField marks = f.text("Marks", 6, q == null ? "1" : String.valueOf(q.marks()));
            List<JTextField> opts = new ArrayList<>();
            for (int i = 1; i <= 4; i++) {
                String initial = q != null && q.options().size() >= i ? q.options().get(i - 1).text() : "";
                opts.add(f.text("Option " + i, 35, initial));
            }
            JComboBox<Integer> correct = f.row("Correct option", new JComboBox<Integer>(new Integer[]{1, 2, 3, 4}));
            if (q != null) {
                for (Option o : q.options()) if (o.correct()) correct.setSelectedItem(o.optionNo());
            }
            boolean saved = Ui.form(this, q == null ? "Add question" : "Edit question", f, () -> {
                List<String> optionTexts = new ArrayList<>();
                for (JTextField t : opts) optionTexts.add(t.getText());
                ExamService.saveQuestion(exam.id(), q == null ? 0 : q.id(), text.getText(),
                        Validator.parseInt("Marks", marks.getText(), 1, 100), optionTexts, (Integer) correct.getSelectedItem());
            });
            if (saved) refresh();
        }

        private void saveTemplate() {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new java.io.File("questions_template.csv"));
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            java.io.File f = fc.getSelectedFile();
            if (f.exists() && !Ui.confirm(this, "Replace the existing file?")) return;
            CsvQuestionImporter.writeTemplate(f.toPath());
            Ui.info(this, "Template saved. Open it in Excel, add your questions, then Save As -> CSV.");
        }

        private void importCsv() {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV files", "csv"));
            if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            List<CsvQuestionImporter.Row> parsed = CsvQuestionImporter.parse(fc.getSelectedFile().toPath());
            if (!Ui.confirm(this, parsed.size() + " question(s) found. Import them into '" + exam.title() + "'?")) return;
            int n = ExamService.importQuestions(exam.id(), parsed);
            Ui.info(this, n + " question(s) imported.");
            refresh();
        }
        private void delete() {
            Question q = selected();
            if (Ui.confirm(this, "Delete this question?")) {
                ExamService.deleteQuestion(exam.id(), q.id());
                refresh();
            }
        }
    }
}
