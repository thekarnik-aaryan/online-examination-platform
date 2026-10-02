package ui;

import exception.AppException;
import exception.ValidationException;
import util.Log;

import javax.swing.*;
import java.awt.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Shared UI helpers: uniform, safe error display (no stack traces or SQL for users). */
public final class Ui {
    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String GENERIC = "Something went wrong. Please try again. If it continues, contact the administrator.";
    private Ui() {}

    public static void run(Component parent, Runnable action) {
        try {
            action.run();
        } catch (AppException e) {
            error(parent, e.getMessage());
        } catch (RuntimeException e) {
            Log.error("Unexpected UI error", e);
            error(parent, GENERIC);
        }
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Notice", JOptionPane.WARNING_MESSAGE);
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Please confirm", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** Shows a form; on OK runs save. Validation errors reopen the form with the entered values kept. */
    public static boolean form(Component parent, String title, JComponent form, Runnable save) {
        while (true) {
            int r = JOptionPane.showConfirmDialog(parent, form, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (r != JOptionPane.OK_OPTION) return false;
            try {
                save.run();
                return true;
            } catch (AppException e) {
                error(parent, e.getMessage());
            } catch (RuntimeException e) {
                Log.error("Unexpected UI error", e);
                error(parent, GENERIC);
                return false;
            }
        }
    }

    public static LocalDateTime parseDateTime(String label, String text) {
        try {
            return LocalDateTime.parse(text.trim().replace(' ', 'T'));
        } catch (RuntimeException e) {
            throw new ValidationException(label + " must look like 2026-12-31 14:30.");
        }
    }
}
