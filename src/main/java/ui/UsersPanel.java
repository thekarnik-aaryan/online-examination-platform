package ui;

import model.Role;
import model.User;
import service.UserService;

import javax.swing.*;
import java.util.List;

public class UsersPanel extends TablePanel<User> {
    public UsersPanel() {
        super("Username", "Full name", "Email", "Role", "Active");
        addButton("Add user", () -> edit(null));
        addButton("Edit", () -> edit(selected()));
        addButton("Reset password", this::resetPassword);
        addButton("Delete", this::delete);
        refresh();
    }

    @Override protected List<User> load() { return UserService.list(); }

    @Override protected Object[] toRow(User u) {
        return new Object[]{u.username(), u.fullName(), u.email() == null ? "" : u.email(), u.role(), u.active() ? "Yes" : "No"};
    }

    private void edit(User u) {
        FormPanel f = new FormPanel();
        JTextField username = f.text("Username", 20, u == null ? "" : u.username());
        username.setEnabled(u == null);
        JPasswordField pw = u == null ? f.password("Password") : null;
        JTextField name = f.text("Full name", 25, u == null ? "" : u.fullName());
        JTextField email = f.text("Email", 25, u == null ? "" : u.email());
        JComboBox<Role> role = f.row("Role", new JComboBox<Role>(Role.values()));
        JCheckBox active = f.row("Active", new JCheckBox());
        if (u != null) role.setSelectedItem(u.role());
        active.setSelected(u == null || u.active());
        boolean saved = Ui.form(this, u == null ? "Add user" : "Edit user", f, () -> {
            Role r = (Role) role.getSelectedItem();
            if (u == null) {
                UserService.create(username.getText(), new String(pw.getPassword()), name.getText(), email.getText(), r);
            } else {
                UserService.update(u.id(), name.getText(), email.getText(), r, active.isSelected());
            }
        });
        if (saved) refresh();
    }

    private void resetPassword() {
        User u = selected();
        FormPanel f = new FormPanel();
        JPasswordField pw = f.password("New password");
        Ui.form(this, "Reset password for " + u.username(), f, () -> {
            UserService.resetPassword(u.id(), new String(pw.getPassword()));
            Ui.info(this, "Password updated.");
        });
    }

    private void delete() {
        User u = selected();
        if (Ui.confirm(this, "Delete user '" + u.username() + "'? This cannot be undone.")) {
            UserService.delete(u.id());
            refresh();
        }
    }
}
