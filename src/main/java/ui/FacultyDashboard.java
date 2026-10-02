package ui;

import service.ExamService;

public class FacultyDashboard extends DashboardFrame {
    public FacultyDashboard() {
        super("Faculty");
        tabs.addTab("My Exams", new ExamsPanel());
        tabs.addTab("Student Results", new ResultsPanel(ExamService::results));
        tabs.addTab("Account", new AccountPanel());
    }
}
