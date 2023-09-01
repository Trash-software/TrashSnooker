package trashsoftware.trashSnooker.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EventLogger {

    private static final String LOG_DIR = "logs";
    public static final String CRASH = "crash";
    public static final String ERROR = "error";
    public static final String WARNING = "warning";
    public static final String INFO = "info";
    public static final String DEBUG = "debug";
    //    private static final String LOG_BASE_NAME = LOG_DIR + File.separator + "error-";
    private static final String DATE_FMT = "yyyy-MM-dd HH-mm-ss";

    private static String baseName(String level) {
        return LOG_DIR + File.separator + level + "-";
    }

    /**
     * Logs an error that causes the App to crash
     *
     * @param throwable the error
     */
    public static void crash(Throwable throwable) {
        log(throwable, CRASH, true);
    }

    /**
     * Logs and prints complete error message and stack trace to a new log file.
     *
     * @param throwable error
     */
    public static void error(Throwable throwable) {
        error(throwable, true);
    }

    /**
     * Logs complete error message and stack trace to a new log file.
     *
     * @param throwable error
     * @param print     whether to print stack trace to stderr
     */
    public static void error(Throwable throwable, boolean print) {
        log(throwable, ERROR, print);
    }

    /**
     * Logs and prints complete warning message and stack trace to a new log file.
     *
     * @param throwable not severe error
     */
    public static void warning(Throwable throwable) {
        warning(throwable, true);
    }

    /**
     * Logs complete warning message and stack trace to a new log file.
     *
     * @param throwable not severe error
     * @param print     whether to print stack trace to stderr
     */
    public static void warning(Throwable throwable, boolean print) {
        log(throwable, WARNING, print);
    }

    /**
     * Logs complete error message and stack trace to a new log file.
     *
     * @param throwable error
     * @param level     the level of this exception
     * @param print     whether to print stack trace to stderr
     */
    public static void log(Throwable throwable, String level, boolean print) {
        if (print) throwable.printStackTrace();

        createLogDirIfNone();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FMT);
        String realName = baseName(level) + sdf.format(new Date()) + ".log";
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
    public static void error(String message) {
        error(message, true);
    }

    /**
     * Logs text warning message to a new log file.
     *
     * @param message text message
     * @param print   whether to also print message in console
     */
    public static void warning(String message, boolean print) {
        log(message, WARNING, print);
    }

    /**
     * Logs text warning message to a new log file.
     *
     * @param message text message
     */
    public static void warning(String message) {
        warning(message, true);
    }

    /**
     * Logs text error message to a new log file.
     *
     * @param message text message
     * @param print   whether to also print message in console
     */
    public static void error(String message, boolean print) {
        log(message, ERROR, print);
    }

    /**
     * Logs text error message to a new log file.
     *
     * @param message text message
     * @param level   the level of this exception
     * @param print   whether to also print message in console
     */
    public static void log(String message, String level, boolean print) {
        if (print) System.err.println(message);
        createLogDirIfNone();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FMT);
        String realName = baseName(level) + sdf.format(new Date()) + ".log";
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
