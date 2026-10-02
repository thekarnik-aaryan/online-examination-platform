package ui;

import service.ExamService;

public class AdminDashboard extends DashboardFrame {
    public AdminDashboard() {
        super("Admin");
        tabs.addTab("Users", new UsersPanel());
        tabs.addTab("Subjects", new SubjectsPanel());
        tabs.addTab("Exams", new ExamsPanel());
        tabs.addTab("Results", new ResultsPanel(ExamService::results));
        tabs.addTab("Audit Logs", new AuditPanel());
        tabs.addTab("Account / Settings", new AccountPanel());
    }
}
