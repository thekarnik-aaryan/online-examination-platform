package config;

import util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/** Loads settings from config.properties (or EXAM_CONFIG path); environment variables override. */
public final class AppConfig {
    private static final Properties PROPS = new Properties();
    static {
        String path = System.getenv("EXAM_CONFIG");
        Path p = Paths.get(path == null || path.isBlank() ? "config.properties" : path);
        if (Files.isReadable(p)) {
            try (InputStream in = Files.newInputStream(p)) {
                PROPS.load(in);
            } catch (IOException e) {
                Log.error("Could not read configuration file", e);
            }
        } else {
            Log.warn("Configuration file not found: " + p.toAbsolutePath() + " (using environment/defaults)");
        }
    }
    private AppConfig() {}

    public static String get(String key, String envVar, String def) {
        String env = envVar == null ? null : System.getenv(envVar);
        if (env != null && !env.isBlank()) return env;
        return PROPS.getProperty(key, def);
    }

    public static int getInt(String key, int def) {
        try { return Integer.parseInt(PROPS.getProperty(key, String.valueOf(def)).trim()); }
        catch (NumberFormatException e) { return def; }
    }
}
