package common;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ChatLogger {

    private static final String LOG_DIRECTORY = "logs";
    private static final String LOG_FILE =
            LOG_DIRECTORY + File.separator + "chat_history.txt";

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ChatLogger() {
        // Utility class
    }

    public static synchronized void log(String message) {

        File directory = new File(LOG_DIRECTORY);

        if (!directory.exists() && !directory.mkdirs()) {
            System.err.println("Unable to create log directory.");
            return;
        }

        try (PrintWriter writer =
                     new PrintWriter(
                             new FileWriter(LOG_FILE, true))) {

            writer.println(
                    "[" + getCurrentTimestamp() + "] " + message
            );

        } catch (IOException e) {

            System.err.println(
                    "Logging error: " + e.getMessage()
            );
        }
    }

    public static String getCurrentTimestamp() {

        return LocalDateTime.now()
                .format(FORMATTER);
    }

    public static String getCurrentTime() {

        return LocalDateTime.now()
                .format(
                        DateTimeFormatter.ofPattern("HH:mm:ss")
                );
    }
}