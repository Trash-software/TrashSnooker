package trashsoftware.trashSnooker.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EventLogger {

    private static final String LOG_DIR = "logs";
    private static final String LOG_BASE_NAME = LOG_DIR + File.separator + "error-";
    private static final String DATE_FMT = "yyyy-MM-dd HH-mm-ss";

    /**
     * Logs and prints complete error message and stack trace to a new log file.
     *
     * @param throwable error
     */
    public static void log(Throwable throwable) {
        log(throwable, true);
    }

    /**
     * Logs complete error message and stack trace to a new log file.
     *
     * @param throwable error
     * @param print whether to print stack trace to stderr
     */
    public static void log(Throwable throwable, boolean print) {
        if (print) throwable.printStackTrace();

        createLogDirIfNone();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FMT);
        String realName = LOG_BASE_NAME + sdf.format(new Date()) + ".log";
        try (PrintWriter pw = new PrintWriter(new FileWriter(realName))) {
            throwable.printStackTrace(pw);

            pw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Logs text error message to a new log file.
     *
     * @param message text message
     */
    public static void log(String message) {
        log(message, true);
    }

    /**
     * Logs text error message to a new log file.
     *
     * @param message text message
     * @param print   whether to also print message in console
     */
    public static void log(String message, boolean print) {
        if (print) System.err.println(message);
        createLogDirIfNone();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FMT);
        String realName = LOG_BASE_NAME + sdf.format(new Date()) + ".log";
        try (FileWriter fileWriter = new FileWriter(realName)) {
            fileWriter.write(message);

            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createLogDirIfNone() {
        File dir = new File(LOG_DIR);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                System.out.println("Failed to create log directory.");
            }
        }
    }
}
