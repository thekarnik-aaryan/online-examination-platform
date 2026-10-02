package ui;

import model.Role;
import security.Session;
import service.AuthService;
import service.UserService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Change own password; admins can also toggle student self-registration. */
public class AccountPanel extends JPanel {
    public AccountPanel() {
        super(new BorderLayout());
        setBorder(new EmptyBorder(20, 20, 20, 20));
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        FormPanel f = new FormPanel();
        JPasswordField oldPw = f.password("Current password");
        JPasswordField newPw = f.password("New password");
        JPasswordField confirm = f.password("Confirm new password");
        JButton change = new JButton("Change password");
        change.addActionListener(e -> Ui.run(this, () -> {
            String n = new String(newPw.getPassword());
            if (!n.equals(new String(confirm.getPassword()))) { Ui.error(this, "New passwords do not match."); return; }
            UserService.changeOwnPassword(new String(oldPw.getPassword()), n);
            oldPw.setText(""); newPw.setText(""); confirm.setText("");
            Ui.info(this, "Password changed.");
        }));
        box.add(new JLabel("Signed in as: " + Session.requireUser().fullName() + " (" + Session.requireUser().role() + ")"));
        box.add(Box.createVerticalStrut(10));
        box.add(f);
        box.add(change);

        if (Session.requireUser().role() == Role.ADMIN) {
            box.add(Box.createVerticalStrut(25));
            JCheckBox reg = new JCheckBox("Allow students to self-register");
            Ui.run(this, () -> reg.setSelected(AuthService.isRegistrationEnabled()));
            reg.addActionListener(e -> Ui.run(this, () -> UserService.setRegistrationEnabled(reg.isSelected())));
            box.add(reg);
        }
        add(box, BorderLayout.NORTH);
    }
}
