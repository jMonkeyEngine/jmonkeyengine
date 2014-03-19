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

import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 *
 * @author qbeukes.blogspot.com, used by metalklesk
 */
public class MessageUtil {

    private MessageUtil() {}

    /**
    * @return The dialog displayer used to show message boxes
    */
    public static DialogDisplayer getDialogDisplayer() {
        return DialogDisplayer.getDefault();
    }

    /**
    * Show a message of the specified type
    *
    * @param message
    * @param messageType As in {@link NotifyDescription} message type constants.
    */
    public static void show(String message, MessageType messageType) {
        getDialogDisplayer().notify(new NotifyDescriptor.Message(message,
        messageType.getNotifyDescriptorType()));
    }

    /**
    * Show an exception message dialog
    *
    * @param message
    * @param exception
    */
    public static void showException(String message, Throwable exception) {
        getDialogDisplayer().notifyLater(new NotifyDescriptor.Exception(exception, message));
    }

    /**
    * Show an information dialog
    * @param message
    */
    public static void info(String message) {
        show(message, MessageType.INFO);
    }

    /**
    * Show an error dialog
    * @param message
    */
    public static void error(String message) {
        show(message, MessageType.ERROR);
    }

    /**
    * Show an error dialog for an exception
    * @param message
    * @param exception
    */
    public static void error(String message, Throwable exception) {
        showException(message, exception);
    }

    /**
    * Show an question dialog
    * @param message
    */
    public static void question(String message) {
        show(message, MessageType.QUESTION);
    }

    /**
    * Show an warning dialog
    * @param message
    */
    public static void warn(String message) {
        show(message, MessageType.WARNING);
    }

    /**
    * Show an plain dialog
    * @param message
    */
    public static void plain(String message) {
        show(message, MessageType.PLAIN);
    }
}