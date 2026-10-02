package ui;

import javax.swing.*;
import java.awt.*;

/** Minimal label/field form layout. */
public class FormPanel extends JPanel {
    private int row;

    public FormPanel() { super(new GridBagLayout()); }

    public <T extends JComponent> T row(String label, T comp) {
        GridBagConstraints l = new GridBagConstraints();
        l.gridx = 0; l.gridy = row; l.anchor = GridBagConstraints.NORTHWEST; l.insets = new Insets(4, 4, 4, 8);
        add(new JLabel(label), l);
        GridBagConstraints f = new GridBagConstraints();
        f.gridx = 1; f.gridy = row; f.fill = GridBagConstraints.HORIZONTAL; f.weightx = 1; f.insets = new Insets(4, 0, 4, 4);
        add(comp, f);
        row++;
        return comp;
    }

    public JTextField text(String label, int cols, String initial) {
        return row(label, new JTextField(initial == null ? "" : initial, cols));
    }

    public JPasswordField password(String label) { return row(label, new JPasswordField(20)); }
}
