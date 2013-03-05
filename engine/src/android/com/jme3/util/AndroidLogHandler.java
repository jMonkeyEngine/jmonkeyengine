package com.jme3.util;

import android.util.Log;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Converts from Java based logging ({@link Logger} to Android based logging
 * {@link Log}.
 */
public class AndroidLogHandler extends Handler {

    private static final Formatter THE_FORMATTER = new Formatter() {
        @Override
        public String format(LogRecord r) {
            Throwable thrown = r.getThrown();
            if (thrown != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                sw.write(r.getMessage());
                sw.write("\n");
                thrown.printStackTrace(pw);
                pw.flush();
                return sw.toString();
            } else {
                return r.getMessage();
            }
        }
    };

    @Override
    public void close() {
    }

    @Override
    public void flush() {
    }

    @Override
    public void publish(LogRecord record) {

        try {
            Level level = record.getLevel();
            String tag = record.getLoggerName();
            String msg = THE_FORMATTER.format(record);
            int lv = getAndroidLevel(level);

            Log.println(lv, tag, msg);

        } catch (RuntimeException e) {
            Log.e("AndroidHandler", "Error logging message.", e);
        }
    }

    /**
     * Converts a {@link java.util.logging.Logger} logging level into an Android
     * one.
     *
     * @param level The {@link java.util.logging.Logger} logging level.
     *
     * @return The resulting Android logging level.
     */
    static int getAndroidLevel(Level level) {
        int value = level.intValue();
        if (value >= 1000) { // SEVERE
            return Log.ERROR;
        } else if (value >= 900) { // WARNING
            return Log.WARN;
        } else if (value >= 800) { // INFO
            return Log.INFO;
        } else {
            return Log.DEBUG;
        }
    }
}
