package ui;

import model.User;
import security.Session;
import service.AuthService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Common shell for the role dashboards: header, logout and tabs. */
public abstract class DashboardFrame extends JFrame {
    protected final JTabbedPane tabs = new JTabbedPane();

    protected DashboardFrame(String roleTitle) {
        super("Online Examination System - " + roleTitle);
        User u = Session.requireUser();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                AuthService.logout();
                dispose();
                System.exit(0);
            }
        });
        JLabel welcome = new JLabel("  Welcome, " + u.fullName() + " (" + u.role() + ")");
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD, 15f));
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> {
            AuthService.logout();
            dispose();
            new LoginFrame().setVisible(true);
        });
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 8));
        header.add(welcome, BorderLayout.WEST);
        header.add(logout, BorderLayout.EAST);
        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        tabs.addChangeListener(e -> {
            Component c = tabs.getSelectedComponent();
            if (c instanceof TablePanel<?> tp) tp.refresh();
        });
        setSize(1050, 620);
        setLocationRelativeTo(null);
    }
}
