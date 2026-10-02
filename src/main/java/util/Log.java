package util;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;

/** Centralized logging. Technical details go to logs/exam-system.log, never to the user. */
public final class Log {
    private static final Logger LOGGER = Logger.getLogger("exam");
    static {
        LOGGER.setUseParentHandlers(true);
        try {
            new File("logs").mkdirs();
            FileHandler fh = new FileHandler("logs/exam-system.log", 1_000_000, 3, true);
            fh.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fh);
        } catch (IOException | SecurityException e) {
            LOGGER.log(Level.WARNING, "File logging unavailable; using console only", e);
        }
    }
    private Log() {}
    public static void info(String msg) { LOGGER.info(msg); }
    public static void warn(String msg) { LOGGER.warning(msg); }
    public static void error(String msg, Throwable t) { LOGGER.log(Level.SEVERE, msg, t); }
}
