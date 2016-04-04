/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.app;

import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.audio.Listener;
import com.jme3.input.InputManager;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.system.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * The <code>Application</code> interface represents the minimum exposed
 * capabilities of a concrete jME3 application.
 */
public interface Application {

    /**
     * Determine the application's behavior when unfocused.
     *
     * @return The lost focus behavior of the application.
     */
    public LostFocusBehavior getLostFocusBehavior();

    /**
     * Change the application's behavior when unfocused.
     *
     * By default, the application will
     * {@link LostFocusBehavior#ThrottleOnLostFocus throttle the update loop}
     * so as to not take 100% CPU usage when it is not in focus, e.g.
     * alt-tabbed, minimized, or obstructed by another window.
     *
     * @param lostFocusBehavior The new lost focus behavior to use.
     *
     * @see LostFocusBehavior
     */
    public void setLostFocusBehavior(LostFocusBehavior lostFocusBehavior);

    /**
     * Returns true if pause on lost focus is enabled, false otherwise.
     *
     * @return true if pause on lost focus is enabled
     *
     * @see #getLostFocusBehavior()
     */
    public boolean isPauseOnLostFocus();

    /**
     * Enable or disable pause on lost focus.
     * <p>
     * By default, pause on lost focus is enabled.
     * If enabled, the application will stop updating
     * when it loses focus or becomes inactive (e.g. alt-tab).
     * For online or real-time applications, this might not be preferable,
     * so this feature should be set to disabled. For other applications,
     * it is best to keep it on so that CPU usage is not used when
     * not necessary.
     *
     * @param pauseOnLostFocus True to enable pause on lost focus, false
     * otherwise.
     *
     * @see #setLostFocusBehavior(com.jme3.app.LostFocusBehavior)
     */
    public void setPauseOnLostFocus(boolean pauseOnLostFocus);

    /**
     * Set the display settings to define the display created.
     * <p>
     * Examples of display parameters include display pixel width and height,
     * color bit depth, z-buffer bits, anti-aliasing samples, and update frequency.
     * If this method is called while the application is already running, then
     * {@link #restart() } must be called to apply the settings to the display.
     *
     * @param settings The settings to set.
     */
    public void setSettings(AppSettings settings);

    /**
     * Sets the Timer implementation that will be used for calculating
     * frame times.  By default, Application will use the Timer as returned
     * by the current JmeContext implementation.
     */
    public void setTimer(Timer timer);

    public Timer getTimer();

    /**
     * @return The {@link AssetManager asset manager} for this application.
     */
    public AssetManager getAssetManager();

    /**
     * @return the {@link InputManager input manager}.
     */
    public InputManager getInputManager();

    /**
     * @return the {@link AppStateManager app state manager}
     */
    public AppStateManager getStateManager();

    /**
     * @return the {@link RenderManager render manager}
     */
    public RenderManager getRenderManager();

    /**
     * @return The {@link Renderer renderer} for the application
     */
    public Renderer getRenderer();

    /**
     * @return The {@link AudioRenderer audio renderer} for the application
     */
    public AudioRenderer getAudioRenderer();

    /**
     * @return The {@link Listener listener} object for audio
     */
    public Listener getListener();

    /**
     * @return The {@link JmeContext display context} for the application
     */
    public JmeContext getContext();

    /**
     * @return The main {@link Camera camera} for the application
     */
    public Camera getCamera();

    /**
     * Starts the application.
     */
    public void start();

    /**
     * Starts the application.
     */
    public void start(boolean waitFor);

    /**
     * Sets an AppProfiler hook that will be called back for
     * specific steps within a single update frame.  Value defaults
     * to null.
     */
    public void setAppProfiler(AppProfiler prof);

    /**
     * Returns the current AppProfiler hook, or null if none is set.
     */
    public AppProfiler getAppProfiler();

    /**
     * Restarts the context, applying any changed settings.
     * <p>
     * Changes to the {@link AppSettings} of this Application are not
     * applied immediately; calling this method forces the context
     * to restart, applying the new settings.
     */
    public void restart();

    /**
     * Requests the context to close, shutting down the main loop
     * and making necessary cleanup operations.
     *
     * Same as calling stop(false)
     *
     * @see #stop(boolean)
     */
    public void stop();

    /**
     * Requests the context to close, shutting down the main loop
     * and making necessary cleanup operations.
     * After the application has stopped, it cannot be used anymore.
     */
    public void stop(boolean waitFor);

    /**
     * Enqueues a task/callable object to execute in the jME3
     * rendering thread.
     * <p>
     * Callables are executed right at the beginning of the main loop.
     * They are executed even if the application is currently paused
     * or out of focus.
     *
     * @param callable The callable to run in the main jME3 thread
     */
    public <V> Future<V> enqueue(Callable<V> callable);

    /**
     * Enqueues a runnable object to execute in the jME3
     * rendering thread.
     * <p>
     * Runnables are executed right at the beginning of the main loop.
     * They are executed even if the application is currently paused
     * or out of focus.
     *
     * @param runnable The runnable to run in the main jME3 thread
     */
    public void enqueue(Runnable runnable);

    /**
     * @return The GUI viewport. Which is used for the on screen
     * statistics and FPS.
     */
    public ViewPort getGuiViewPort();

    public ViewPort getViewPort();
}
