/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

package com.jme3.system;

/**
 * The <code>ContextListener> provides a means for an application
 * to receive events relating to a context.
 */
public interface SystemListener {

    /**
     * Callback to indicate the application to initialize. This method
     * is called in the GL/Rendering thread so any GL-dependent resources
     * can be initialized.
     */
    public void initialize();

    /**
     * Called to notify the application that the resolution has changed.
     * @param width
     * @param height
     */
    public void reshape(int width, int height);

    /**
     * Callback to update the application state, and render the scene
     * to the back buffer.
     */
    public void update();

    /**
     * Called when the user requests to close the application. This
     * could happen when he clicks the X button on the window, presses
     * the Alt-F4 combination, attempts to shutdown the process from 
     * the task manager, or presses ESC. 
     * @param esc If true, the user pressed ESC to close the application.
     */
    public void requestClose(boolean esc);

    /**
     * Called when the application gained focus. The display
     * implementation is not allowed to call this method before
     * initialize() has been called or after destroy() has been called.
     */
    public void gainFocus();

    /**
     * Called when the application lost focus. The display
     * implementation is not allowed to call this method before
     * initialize() has been called or after destroy() has been called.
     */
    public void loseFocus();

    /**
     * Called when an error has occured. This is typically
     * invoked when an uncought exception is thrown in the render thread.
     * @param errorMsg The error message, if any, or null.
     * @param t Throwable object, or null.
     */
    public void handleError(String errorMsg, Throwable t);

    /**
     * Callback to indicate that the context has been destroyed (either
     * by the user or requested by the application itself). Typically
     * cleanup of native resources should happen here. This method is called
     * in the GL/Rendering thread.
     */
    public void destroy();
}
