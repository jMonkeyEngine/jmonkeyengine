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
import javax.swing.Timer;
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
    public static void show(String title, String message, MessageType type, ActionListener actionListener, int timeout) {
        final Notification n = (Notification) NotificationDisplayer.getDefault().notify(title, type.getIcon(), message, actionListener);
        if (timeout > 0) {
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    Timer timer = new Timer(10000, new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            n.clear();
                        }
                    });
                    timer.setRepeats(false);
                    timer.start();
                }
            });
        }
    }

    /**
     * Show message with the specified type and a default action which displays
     * the message using {@link MessageUtil} with the same message type
     */
    public static void show(String title, final String message, final MessageType type, int timeout) {
        ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageUtil.show(message, type);
            }
        };

        show(title, message, type, actionListener, timeout);
    }

    /**
     * Show an information notification
     *
     * @param message
     */
    public static void info(String title, String message) {
        error(title, message, true);
    }

    /**
     * Show an information notification
     *
     * @param message
     */
    public static void info(String title, String message, boolean clear) {
        show(title, message, MessageType.INFO, 3000);
    }

    /**
     * Show an exception
     *
     * @param exception
     */
    public static void error(Throwable exception) {
        error("Exception in SDK!", exception.getMessage(), exception, true);
    }

    /**
     * Show an error notification
     *
     * @param message
     */
    public static void error(String title, String message, boolean clear) {
        show(title, message, MessageType.ERROR, 10000);
    }

    /**
     * Show an error notification for an exception
     *
     * @param message
     * @param exception
     */
    public static void error(String title, final String message, final Throwable exception, boolean clear) {
        ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                ErrorManager.getDefault().notify(exception);
                MessageUtil.showException(message, exception);
            }
        };

        show(title, message, MessageType.EXCEPTION, actionListener, 10000);
    }

    /**
     * Show an warning notification
     *
     * @param message
     */
    public static void warn(String title, String message, boolean clear) {
        show(title, message, MessageType.WARNING, 5000);
    }

    /**
     * Show an plain notification
     *
     * @param message
     */
    public static void plain(String title, String message, boolean clear) {
        show(title, message, MessageType.PLAIN, 5000);
    }
}
