package ui;

import model.User;
import service.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginFrame extends JFrame {
    private final JTextField username = new JTextField(20);
    private final JPasswordField password = new JPasswordField(20);

    public LoginFrame() {
        super("Online Examination System - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JLabel title = new JLabel("Online Examination System", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));

        FormPanel form = new FormPanel();
        form.row("Username", username);
        form.row("Password", password);

        JButton login = new JButton("Login");
        JButton register = new JButton("Student registration");
        login.addActionListener(e -> Ui.run(this, this::doLogin));
        register.addActionListener(e -> Ui.run(this, this::doRegister));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttons.add(login);
        buttons.add(register);

        JPanel root = new JPanel(new BorderLayout(10, 15));
        root.setBorder(new EmptyBorder(25, 30, 20, 30));
        root.add(title, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(login);
        pack();
        setLocationRelativeTo(null);
    }

    private void doLogin() {
        User u;
        try {
            u = AuthService.login(username.getText(), new String(password.getPassword()));
        } finally {
            password.setText("");
        }
        JFrame next = switch (u.role()) {
            case ADMIN -> new AdminDashboard();
            case FACULTY -> new FacultyDashboard();
            case STUDENT -> new StudentDashboard();
        };
        dispose();
        next.setVisible(true);
    }

    private void doRegister() {
        if (!AuthService.isRegistrationEnabled()) {
            Ui.error(this, "Student registration is currently disabled by the administrator.");
            return;
        }
        FormPanel f = new FormPanel();
        JTextField user = f.text("Username", 20, "");
        JPasswordField pw = f.password("Password");
        JTextField name = f.text("Full name", 20, "");
        JTextField email = f.text("Email (optional)", 20, "");
        if (Ui.form(this, "Student registration", f, () ->
                AuthService.registerStudent(user.getText(), new String(pw.getPassword()), name.getText(), email.getText()))) {
            Ui.info(this, "Registration successful. You can now log in.");
        }
    }
}
