import service.AuthService;
import ui.LoginFrame;
import util.Log;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Log.error("Uncaught exception in thread " + t.getName(), e);
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "An unexpected error occurred. Details were written to the log.", "Error", JOptionPane.ERROR_MESSAGE));
        });
        Runtime.getRuntime().addShutdownHook(new Thread(AuthService::logout));
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                Log.warn("System look-and-feel unavailable: " + e.getMessage());
            }
            new LoginFrame().setVisible(true);
        });
    }
}
