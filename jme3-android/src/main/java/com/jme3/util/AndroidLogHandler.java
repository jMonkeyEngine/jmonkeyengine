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

    private static final Formatter JME_FORMATTER = new JmeFormatter() {

        String lineSeperator = System.getProperty("line.separator");

        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();

            sb.append(record.getLevel().getLocalizedName()).append(" ");
            sb.append(formatMessage(record)).append(lineSeperator);

            if (record.getThrown() != null) {
                try {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    record.getThrown().printStackTrace(pw);
                    pw.close();
                    sb.append(sw.toString());
                } catch (Exception ex) {
                }
            }

            return sb.toString();
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

        int level = getAndroidLevel(record.getLevel());
//        String tag = loggerNameToTag(record.getLoggerName());
        String tag = record.getLoggerName();

        try {
            String message = JME_FORMATTER.format(record);
            Log.println(level, tag, message);
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

    /**
     * Returns the short logger tag for the given logger name.
     * Traditionally loggers are named by fully-qualified Java classes; this
     * method attempts to return a concise identifying part of such names.
     * 
     * @param loggerName the logger name, or null for anonymous
     * @return the short logger tag
     */
    public static String loggerNameToTag(String loggerName) {
        // Anonymous logger.
        if (loggerName == null) {
            return "null";
        }

        int length = loggerName.length();
        int lastPeriod = loggerName.lastIndexOf(".");

        if (lastPeriod == -1) {
            return loggerName;
        }

        return loggerName.substring(lastPeriod + 1);
    }

}
