package com.fitness.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * One-time application logging setup: console output plus a small rotating
 * log file under the user's application-data directory, so a crash report
 * or bug report has something concrete to attach. Safe to call multiple
 * times; only configures the root logger once.
 */
public final class AppLogger {

    private static boolean configured = false;

    private AppLogger() {}

    public static synchronized void configure() {
        if (configured) {
            return;
        }
        configured = true;

        Logger root = Logger.getLogger("");
        for (var handler : root.getHandlers()) {
            root.removeHandler(handler);
        }

        ConsoleHandler console = new ConsoleHandler();
        console.setLevel(Level.INFO);
        console.setFormatter(new SimpleFormatter());
        root.addHandler(console);
        root.setLevel(Level.INFO);

        try {
            Path logDir = Paths.get(System.getProperty("user.home", "."), ".ai-workout-tracker", "logs");
            Files.createDirectories(logDir);
            FileHandler fileHandler = new FileHandler(logDir.resolve("app-%g.log").toString(),
                    /* limit bytes */ 1_000_000, /* rotating file count */ 3, /* append */ true);
            fileHandler.setLevel(Level.INFO);
            fileHandler.setFormatter(new SimpleFormatter());
            root.addHandler(fileHandler);
        } catch (IOException e) {
            root.log(Level.WARNING, "Could not set up file logging; continuing with console logging only", e);
        }
    }
}
