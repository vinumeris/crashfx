package com.vinumeris.crashfx;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * CrashFX provides utility methods to set up crash handling in your application.
 */
public class CrashFX {
    /**
     * A string that is printed before the stack trace in the details log, e.g. should be the version of your app.
     * Also used for the HTTP user agent when submitting crash reports.
     */
    public static String APP_IDENTIFIER = "";

    /**
     * The path where crash reports are to be stored until they can be uploaded. Should really be a subdirectory of
     * your app private directory. If null, crash reports will not be saved or uploaded anywhere.
     */
    public static Path DIRECTORY = null;

    /**
     * Should be an HTTP[S] URL where crash reports will be POSTd to at startup. If null, upload will not be available.
     */
    public static URI UPLOAD_URI = null;

    /**
     * If true, then you will not get clipped exceptions (caused by ....) but rather, a single stack trace with the
     * root cause in it. These can be easier to read, but lose some information.
     */
    public static boolean UNWRAP_EXCEPTIONS = true;

    /**
     * A function that logs a message in whatever way is appropriate for your app. Used to print exceptions in one or
     * two places. Usually you'd set this to be a call to whatever logging framework you're using.
     */
    public static Consumer<String> LOGGER = System.err::println;

    /**
     * How many lines taken either from the JDK logging framework or from {@link #recordLogLine(String)} will be stored
     * in memory and attached to a crash report. Defaults to 1000.
     */
    public static int LOG_LINES_TO_REPORT = 1000;

    /**
     * Call this to set the thread uncaught exception handlers to point to CrashFX. Crash reports will NOT be saved
     * or offered for upload in this configuration.
     */
    public static void setup() {
        setup("", null, null);
    }

    /**
     * Call this to set the thread uncaught exception handlers to point to CrashFX.
     * @param appIdentifier This string will be assigned to {@link com.vinumeris.crashfx.CrashFX#APP_IDENTIFIER},
     *                      and will be put at the top of any crash reports and in the upload HTTP user agent field, so
     *                      include your app version here at least.
     * @param crashReportsDirectory Where to save crash report files to . If any crash reports are found there that
     *                              were not uploaded yet, this method will trigger background upload for them.
     * @param uploadURI URI where crash reports will be uploaded; successfully uploaded reports are deleted from disk.
     */
    public static void setup(String appIdentifier, Path crashReportsDirectory, URI uploadURI) {
        CrashFX.APP_IDENTIFIER = appIdentifier;
        Thread.UncaughtExceptionHandler handler = (thread, throwable) -> {
            throwable.printStackTrace();
            CrashWindow.open(throwable);
        };
        Thread.setDefaultUncaughtExceptionHandler(handler);
        Thread.currentThread().setUncaughtExceptionHandler(handler);
        if (crashReportsDirectory != null) {
            CrashFX.DIRECTORY = crashReportsDirectory;
            if (uploadURI != null) {
                CrashFX.UPLOAD_URI = uploadURI;
                uploadPendingReports();
            }
        }
        Logger logger = Logger.getLogger("");
        logger.addHandler(new Handler() {
            private final MessageFormat messageFormat = new MessageFormat("{3,date,hh:mm:ss} {0} {1}.{2}: {4}\n{5}");

            @Override
            public void publish(LogRecord logRecord) {
                Object[] arguments = new Object[6];
                arguments[0] = logRecord.getThreadID();
                String fullClassName = logRecord.getSourceClassName();
                int lastDot = fullClassName.lastIndexOf('.');
                String className = fullClassName.substring(lastDot + 1);
                arguments[1] = className;
                arguments[2] = logRecord.getSourceMethodName();
                arguments[3] = new Date(logRecord.getMillis());
                arguments[4] = logRecord.getMessage();
                if (logRecord.getThrown() != null) {
                    Writer result = new StringWriter();
                    logRecord.getThrown().printStackTrace(new PrintWriter(result));
                    arguments[5] = result.toString();
                } else {
                    arguments[5] = "";
                }
                recordLogLine(messageFormat.format(arguments));
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        });
    }

    private static LinkedList<String> recentLoggedStrings = new LinkedList<>();

    /** Call this from your logging framework if not using JDK logging, to add strings to the ring buffer */
    public static void recordLogLine(String line) {
        recentLoggedStrings.add(line);
        if (recentLoggedStrings.size() > LOG_LINES_TO_REPORT)
            recentLoggedStrings.poll();
    }

    static String getRecentLogs() {
        StringBuilder builder = new StringBuilder();
        recentLoggedStrings.forEach(builder::append);
        return builder.toString();
    }

    private static void uploadPendingReports() {
        assert DIRECTORY != null;
        assert UPLOAD_URI != null;
        Thread thread = new Thread("CrashFX report upload") {
            @Override
            public void run() {
                for (Path path : listDir(DIRECTORY)) {
                    if (!path.toString().endsWith(".crash.txt") || !attemptReportUpload(path))
                        break;
                    uncheck(() -> Files.delete(path));
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
    }

    private static boolean attemptReportUpload(Path path) {
        try {
            HttpURLConnection conn = (HttpURLConnection) unchecked(() -> UPLOAD_URI.toURL().openConnection());
            conn.addRequestProperty("User-Agent", APP_IDENTIFIER);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            try (OutputStream stream = conn.getOutputStream()) {
                stream.write(Files.readAllBytes(path));
            }
            int code = conn.getResponseCode();
            if (code != 200 && code != 204 /* No content */) {
                LOGGER.accept(String.format("Failed to upload crash report %s, server response was %d %s",
                        path.getFileName(),
                        code,
                        conn.getResponseMessage()));
                return false;
            } else {
                LOGGER.accept("Successfully uploaded crash report " + path.getFileName());
                return true;
            }
        } catch (IOException e) {
            LOGGER.accept("Failed to upload crash report " + path);
            LOGGER.accept("Encountered IO exception, will try again later: " + e.getMessage());
            return false;
        }
    }

    //region Generic Java 8 enhancements

    /** Returns the stack trace of the throwable, respecting the UNWRAP_EXCEPTIONS setting. */
    public static String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        Throwable rootCause = throwable;
        if (CrashFX.UNWRAP_EXCEPTIONS)
            while (rootCause.getCause() != null) rootCause = rootCause.getCause();
        rootCause.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /** Returns a linked list with all the entries in the given directory. Throws a runtime exception in case of error. */
    public static List<Path> listDir(Path dir) {
        List<Path> contents = new LinkedList<>();
        try (Stream<Path> list = unchecked(() -> Files.list(dir))) {
            list.forEach(contents::add);
        }
        return contents;
    }

    /** Like Callable but allowed to throw any exception, and can return a result. */
    public interface UncheckedCallable<T> {
        public T call() throws Throwable;
    }

    /** Like Runnable but can throw any exception. */
    public interface UncheckedRunnable {
        public void run() throws Throwable;
    }

    private static <T> T propagate(Throwable throwable) {
        if (throwable instanceof RuntimeException)
            throw (RuntimeException) throwable;
        else if (throwable instanceof Error)
            throw (Error) throwable;
        else
            throw new RuntimeException(throwable);
    }

    /** Returns the result of the given function with any exceptions rethrown inside RuntimeException if necessary. */
    public static <T> T unchecked(UncheckedCallable<T> run) {
        try {
            return run.call();
        } catch (Throwable throwable) {
            return propagate(throwable);
        }
    }

    /** Runs the given function, with any exceptions rethrown inside RuntimeException if necessary. */
    public static void uncheck(UncheckedRunnable run) {
        try {
            run.run();
        } catch (Throwable throwable) {
            propagate(throwable);
        }
    }

    /** Runs the given function, calling LOGGER with the stack trace of any exceptions, which are then ignored. */
    public static void ignoreAndLog(UncheckedRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            LOGGER.accept(getStackTrace(t));
        }
    }

    /**
     * Returns the result of the given function, calling LOGGER with the stack trace of any exceptions, which are
     * ignored and null returned in the error case.
     */
    public static <T> T ignoredAndLogged(UncheckedCallable<T> runnable) {
        try {
            return runnable.call();
        } catch (Throwable t) {
            LOGGER.accept(getStackTrace(t));
            return null;
        }
    }

    /** Returns true if the given function resulted in an exception, false otherwise. */
    public static boolean didThrow(UncheckedCallable run) {
        try {
            run.call();
            return false;
        } catch (Throwable throwable) {
            return true;
        }
    }

    /** Returns true if the given function resulted in an exception, false otherwise. */
    public static boolean didThrow(UncheckedRunnable run) {
        try {
            run.run();
            return false;
        } catch (Throwable throwable) {
            return true;
        }
    }
    //endregion
}
