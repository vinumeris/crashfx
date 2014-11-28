package com.vinumeris.crashfx;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
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
            LOGGER.accept("CRASH!: " + getStackTrace(throwable));
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
