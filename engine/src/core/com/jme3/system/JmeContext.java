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

import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.TouchInput;
import com.jme3.renderer.Renderer;

/**
 * Represents a rendering context within the engine.
 */
public interface JmeContext {

    /**
     * The type of context.
     */
    public enum Type {
        /**
         * A display can represent a windowed or a fullscreen-exclusive display.
         * If windowed, the graphics are rendered to a new on-screen surface
         * enclosed in a window defined by the operating system. Implementations
         * are encouraged to not use AWT or Swing to create the OpenGL display
         * but rather use native operating system functions to set up a native
         * display with the windowing system.
         */
        Display,
        
        /**
         * A canvas type context makes a rendering surface available as an
         * AWT {@link java.awt.Canvas} object that can be embedded in a Swing/AWT
         * frame. To retrieve the Canvas object, you should cast the context
         * to {@link JmeCanvasContext}.
         */
        Canvas,
        
        /**
         * An <code>OffscreenSurface</code> is a context that is not visible
         * by the user. The application can use the offscreen surface to do
         * General Purpose GPU computations or render a scene into a buffer
         * in order to save it as a screenshot, video or send through a network.
         */
        OffscreenSurface,

        /**
         * A <code>Headless</code> context is not visible and does not have
         * any drawable surface. The implementation does not provide any
         * display, input, or sound support.
         */
        Headless;
    }

    /**
     * @return The type of the context.
     */
    public Type getType();
    
    /**
     * @param settings the display settings to use for the created context. If
     * the context has already been created, then <code>restart()</code> must be called
     * for the changes to be applied.
     */
    public void setSettings(AppSettings settings);

    /**
     * Sets the listener that will receive events relating to context
     * creation, update, and destroy.
     */
    public void setSystemListener(SystemListener listener);

    /**
     * @return The current display settings. Note that they might be 
     * different from the ones set with setDisplaySettings() if the context
     * was restarted or the settings changed internally.
     */
    public AppSettings getSettings();

    /**
     * @return The renderer for this context, or null if not created yet.
     */
    public Renderer getRenderer();

    /**
     * @return Mouse input implementation. May be null if not available.
     */
    public MouseInput getMouseInput();

    /**
     * @return Keyboard input implementation. May be null if not available.
     */
    public KeyInput getKeyInput();

    /**
     * @return Joystick input implementation. May be null if not available.
     */
    public JoyInput getJoyInput();
    
    /**
     * @return Touch device input implementation. May be null if not available.
     */
    public TouchInput getTouchInput();
    
    /**
     * @return The timer for this context, or null if not created yet.
     */
    public Timer getTimer();
    
    /**
     * Sets the title of the display (if available). This does nothing
     * for fullscreen, headless, or canvas contexts.
     * @param title The new title of the display.
     */
    public void setTitle(String title);

    /**
     * @return True if the context has been created but not yet destroyed.
     */
    public boolean isCreated();

    /**
     * @return True if the context contains a valid render surface,
     * if any of the rendering methods in {@link Renderer} are called
     * while this is <code>false</code>, then the result is undefined.
     */
    public boolean isRenderable();

    /**
     * @param enabled If enabled, the context will automatically flush
     * frames to the video card (swap buffers) after an update cycle.
     */
    public void setAutoFlushFrames(boolean enabled);

    /**
     * Creates the context and makes it active.
     *
     * @param waitFor If true, will wait until context has initialized.
     */
    public void create(boolean waitFor);

    /**
     * Destroys and then re-creates the context. This should be called after
     * the display settings have been changed.
     */
    public void restart();

    /**
     * Destroys the context completely, making it inactive.
     *
     * @param waitFor If true, will wait until the context is destroyed fully.
     */
    public void destroy(boolean waitFor);

}
