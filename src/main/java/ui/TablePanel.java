package ui;

import exception.ValidationException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Reusable "table + button bar" panel. Subclasses only supply data loading and row mapping. */
public abstract class TablePanel<T> extends JPanel {
    protected final DefaultTableModel model;
    protected final JTable table;
    protected List<T> rows = new ArrayList<>();
    private final JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT));

    protected TablePanel(String... columns) {
        super(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        add(buttonBar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        addButton("Refresh", this::refresh);
    }

    protected abstract List<T> load();

    protected abstract Object[] toRow(T item);

    public void refresh() {
        Ui.run(this, () -> {
            rows = load();
            model.setRowCount(0);
            for (T item : rows) model.addRow(toRow(item));
        });
    }

    protected T selected() {
        int i = table.getSelectedRow();
        if (i < 0 || i >= rows.size()) throw new ValidationException("Select a row first.");
        return rows.get(i);
    }

    protected void addButton(String text, Runnable action) {
        JButton b = new JButton(text);
        b.addActionListener(e -> Ui.run(this, action));
        buttonBar.add(b);
    }
}
