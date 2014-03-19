/*
 * Copyright (c) 2003-2012 jMonkeyEngine
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.core.util.notify;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.Timer;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.Notification;
import org.openide.awt.NotificationDisplayer;

/**
 *
 * @author qbeukes.blogspot.com, used by metalklesk
 */
public class NotifyUtil {

    private NotifyUtil() {
    }

    /**
     * Show message with the specified type and action listener
     */
    public static Notification show(String title, String message, MessageType type, ActionListener actionListener, final int timeout) {
        if (message == null) {
            message = "null";
        }
        if (title == null) {
            title = "null";
        }
        final Notification n = (Notification) NotificationDisplayer.getDefault().notify(title, type.getIcon(), message, actionListener);
        if (timeout > 0) {
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    Timer timer = new Timer(timeout, new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            n.clear();
                        }
                    });
                    timer.setRepeats(false);
                    timer.start();
                }
            });
        }
        return n;
    }

    /**
     * Show message with the specified type and a default action which displays
     * the message using {@link MessageUtil} with the same message type
     */
    public static Notification show(String title, final String message, final MessageType type, int timeout) {
        ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageUtil.show(message, type);
            }
        };

        return show(title, message, type, actionListener, timeout);
    }

    /**
     * Show an information notification
     *
     * @param message
     */
    public static Notification info(String title, String message) {
        return info(title, message, true);
    }

    /**
     * Show an information notification
     *
     * @param message
     */
    public static Notification info(String title, String message, boolean clear) {
        return show(title, message, MessageType.INFO, clear ? 3000 : 0);
    }

    /**
     * Show an error notification
     *
     * @param message
     */
    public static Notification error(String title, String message, boolean clear) {
        return show(title, message, MessageType.ERROR, clear ? 10000 : 0);
    }

    /**
     * Show an exception
     *
     * @param exception
     */
    public static Notification error(Throwable exception) {
        return error("Exception", exception.getMessage(), exception, false);
    }

    public static Notification error(String title, Throwable exception) {
        return error(title, exception, false);
    }

    public static Notification error(String title, Throwable exception, boolean clear) {
        return error(title, exception.getMessage(), exception, clear);
    }

    /**
     * Show an error notification for an exception
     *
     * @param message
     * @param exception
     */
    public static Notification error(String title, String message, final Throwable exception, boolean clear) {
        final NoteKeeper keeper = new NoteKeeper();
        if (message == null) {
            message = exception.getMessage();
        }
        if (title == null) {
            message = exception.getMessage();
        }
        ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                StringWriter out = new StringWriter();
                exception.printStackTrace(new PrintWriter(out));
                String exception = out.toString();
                DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message(exception, NotifyDescriptor.ERROR_MESSAGE));
                keeper.note.clear();
            }
        };

        keeper.note = show(title, message, MessageType.EXCEPTION, actionListener, clear ? 10000 : 0);
        return keeper.note;
    }

    /**
     * Show an warning notification
     *
     * @param message
     */
    public static Notification warn(String title, String message, boolean clear) {
        return show(title, message, MessageType.WARNING, clear ? 5000 : 0);
    }

    /**
     * Show an plain notification
     *
     * @param message
     */
    public static Notification plain(String title, String message, boolean clear) {
        return show(title, message, MessageType.PLAIN, clear ? 3000 : 0);
    }

    private static class NoteKeeper {

        Notification note;
    }
}
