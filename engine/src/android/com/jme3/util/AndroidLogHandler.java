package com.jme3.util;

import android.util.Log;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Converts from Java based logging ({@link Logger} to Android based
 * logging {@link Log}.
 */
public class AndroidLogHandler extends Handler {

    @Override
    public void close() {
    }

    @Override
    public void flush() {
    }

    @Override
    public void publish(LogRecord record) {
        Level level = record.getLevel();
        String clsName = record.getSourceClassName();
        String msg = record.getMessage();
        Throwable t = record.getThrown();
        if (level == Level.INFO) {
            Log.i(clsName, msg, t);
        } else if (level == Level.SEVERE) {
            Log.e(clsName, msg, t);
        } else if (level == Level.WARNING) {
            Log.w(clsName, msg, t);
        } else if (level == Level.CONFIG) {
            Log.d(clsName, msg, t);
        } else if (level == Level.FINE || level == Level.FINER || level == Level.FINEST) {
            Log.v(clsName, msg, t);
        }
    }
}
