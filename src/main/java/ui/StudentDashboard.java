package ui;

import service.AttemptService;

public class StudentDashboard extends DashboardFrame {
    public StudentDashboard() {
        super("Student");
        ResultsPanel results = new ResultsPanel(AttemptService::myResults);
        tabs.addTab("Available Exams", new AvailableExamsPanel(results::refresh));
        tabs.addTab("My Results", results);
        tabs.addTab("Account", new AccountPanel());
    }
}
