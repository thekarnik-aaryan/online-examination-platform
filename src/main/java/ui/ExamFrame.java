package ui;

import exception.AppException;
import model.Option;
import model.Question;
import model.Result;
import service.AttemptService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** Timed MCQ exam screen. All persistence and rules live in AttemptService. */
public class ExamFrame extends JFrame {
    private final AttemptService.ExamSession session;
    private final Map<Long, Long> answers = new HashMap<>();
    private final Runnable onDone;
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> questionList = new JList<>(listModel);
    private final JLabel timerLabel = new JLabel("--:--", SwingConstants.RIGHT);
    private final JLabel headerLabel = new JLabel();
    private final JTextArea questionText = new JTextArea(5, 50);
    private final JPanel optionsPanel = new JPanel();
    private final javax.swing.Timer timer;
    private int index;
    private boolean finished;
    private boolean updating;

    public ExamFrame(AttemptService.ExamSession session, Runnable onDone) {
        super("Exam - " + session.exam().title());
        this.session = session;
        this.onDone = onDone;
        answers.putAll(session.answers());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { leave(); }
        });
        buildUi();
        for (int i = 0; i < session.questions().size(); i++) listModel.addElement("");
        refreshList();
        show(0);
        timer = new javax.swing.Timer(1000, e -> tick());
        timer.start();
        tick();
        setSize(950, 600);
        setLocationRelativeTo(null);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 16f));
        timerLabel.setFont(timerLabel.getFont().deriveFont(Font.BOLD, 20f));
        JPanel top = new JPanel(new BorderLayout());
        top.add(headerLabel, BorderLayout.WEST);
        top.add(timerLabel, BorderLayout.EAST);
        root.add(top, BorderLayout.NORTH);

        questionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        questionList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !updating && questionList.getSelectedIndex() >= 0) show(questionList.getSelectedIndex());
        });
        JScrollPane left = new JScrollPane(questionList);
        left.setPreferredSize(new Dimension(190, 100));
        root.add(left, BorderLayout.WEST);

        questionText.setEditable(false);
        questionText.setLineWrap(true);
        questionText.setWrapStyleWord(true);
        questionText.setFont(questionText.getFont().deriveFont(15f));
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(new JScrollPane(questionText), BorderLayout.NORTH);
        center.add(optionsPanel, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        JButton prev = new JButton("Previous");
        JButton next = new JButton("Next");
        JButton clear = new JButton("Clear answer");
        JButton submit = new JButton("Submit exam");
        prev.addActionListener(e -> { if (index > 0) show(index - 1); });
        next.addActionListener(e -> { if (index < session.questions().size() - 1) show(index + 1); });
        clear.addActionListener(e -> select(null));
        submit.addActionListener(e -> submitManually());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        bottom.add(prev);
        bottom.add(next);
        bottom.add(clear);
        bottom.add(submit);
        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private Question current() { return session.questions().get(index); }

    private void show(int i) {
        index = i;
        Question q = current();
        headerLabel.setText(session.exam().title() + "  -  Question " + (i + 1) + " of " + session.questions().size()
                + "  (" + q.marks() + " mark" + (q.marks() == 1 ? "" : "s") + ")");
        questionText.setText(q.text());
        optionsPanel.removeAll();
        ButtonGroup group = new ButtonGroup();
        Long chosen = answers.get(q.id());
        for (Option o : q.options()) {
            JRadioButton rb = new JRadioButton(o.optionNo() + ".  " + o.text());
            rb.setFont(rb.getFont().deriveFont(14f));
            rb.setBorder(new EmptyBorder(6, 6, 6, 6));
            rb.setSelected(chosen != null && chosen == o.id());
            rb.addActionListener(e -> select(o));
            group.add(rb);
            optionsPanel.add(rb);
        }
        optionsPanel.revalidate();
        optionsPanel.repaint();
        refreshList();
    }

    private void refreshList() {
        updating = true;
        for (int i = 0; i < session.questions().size(); i++) {
            boolean done = answers.containsKey(session.questions().get(i).id());
            listModel.setElementAt("Q" + (i + 1) + (done ? "   [answered]" : "   [not answered]"), i);
        }
        questionList.setSelectedIndex(index);
        updating = false;
    }

    private void select(Option o) {
        if (finished) return;
        Question q = current();
        Ui.run(this, () -> {
            AttemptService.saveAnswer(session.attempt().id(), q.id(), o == null ? null : o.id());
            if (o == null) answers.remove(q.id()); else answers.put(q.id(), o.id());
        });
        show(index); // re-sync radio state with what was actually stored
    }

    private void tick() {
        if (finished) return;
        long secs = Duration.between(LocalDateTime.now(), session.attempt().deadline()).getSeconds();
        if (secs <= 0) {
            timerLabel.setText("00:00");
            finish(true);
            return;
        }
        timerLabel.setText(String.format("%02d:%02d", secs / 60, secs % 60));
        timerLabel.setForeground(secs <= 60 ? Color.RED : Color.BLACK);
    }

    private void submitManually() {
        int unanswered = session.questions().size() - answers.size();
        String msg = "Submit the exam now?" + (unanswered > 0 ? "\n" + unanswered + " question(s) are not answered." : "")
                + "\nYou cannot change your answers after submitting.";
        if (Ui.confirm(this, msg)) finish(false);
    }

    private void finish(boolean auto) {
        if (finished) return;
        finished = true;
        timer.stop();
        try {
            Result r = AttemptService.submit(session.attempt().id(), auto);
            String header = auto ? "Time is up - your exam was submitted automatically.\n\n" : "Exam submitted.\n\n";
            Ui.info(this, header + "Score: " + r.score() + " / " + r.totalMarks() + "\nPercentage: "
                    + String.format("%.2f%%", r.percentage()) + "\nCorrect: " + r.correct()
                    + "\nIncorrect: " + r.incorrect() + "\nUnattempted: " + r.unattempted());
            closeScreen();
        } catch (AppException e) {
            if (e.getMessage() != null && e.getMessage().contains("already been submitted")) {
                Ui.info(this, "This exam was already submitted. See My Results.");
                closeScreen();
            } else {
                finished = false; // let the student retry
                Ui.error(this, e.getMessage());
            }
        }
    }

    private void leave() {
        if (finished) { closeScreen(); return; }
        if (Ui.confirm(this, "The exam is still running and the timer keeps counting.\nLeave now? You can resume until time runs out.")) {
            timer.stop();
            closeScreen();
        }
    }

    private void closeScreen() {
        timer.stop();
        dispose();
        onDone.run();
    }
}
