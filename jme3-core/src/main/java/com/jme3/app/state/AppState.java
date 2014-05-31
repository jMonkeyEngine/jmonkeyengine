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
package com.jme3.app.state;

import com.jme3.app.Application;
import com.jme3.renderer.RenderManager;

/**
 * AppState represents continously executing code inside the main loop.
 * 
 * An <code>AppState</code> can track when it is attached to the 
 * {@link AppStateManager} or when it is detached. 
 * 
 * <br/><code>AppState</code>s are initialized in the render thread, upon a call to 
 * {@link AppState#initialize(com.jme3.app.state.AppStateManager, com.jme3.app.Application) }
 * and are de-initialized upon a call to {@link AppState#cleanup()}. 
 * Implementations should return the correct value with a call to 
 * {@link AppState#isInitialized() } as specified above.<br/>
 * 
 * <ul>
 * <li>If a detached AppState is attached then <code>initialize()</code> will be called
 * on the following render pass.
 * </li>
 * <li>If an attached AppState is detached then <code>cleanup()</code> will be called
 * on the following render pass.
 * </li>
 * <li>If you attach an already-attached <code>AppState</code> then the second attach
 * is a no-op and will return false.
 * </li>
 * <li>If you both attach and detach an <code>AppState</code> within one frame then
 * neither <code>initialize()</code> or <code>cleanup()</code> will be called,
 * although if either is called both will be.
 * </li>
 * <li>If you both detach and then re-attach an <code>AppState</code> within one frame
 * then on the next update pass its <code>cleanup()</code> and <code>initialize()</code>
 * methods will be called in that order.
 * </li>
 * </ul>
 * @author Kirill Vainer
 */
public interface AppState {

    /**
     * Called by {@link AppStateManager} when transitioning this {@code AppState}
     * from <i>initializing</i> to <i>running</i>.<br>
     * This will happen on the next iteration through the update loop after
     * {@link AppStateManager#attach(com.jme3.app.state.AppState) } was called.
     * <p>
     * <code>AppStateManager</code> will call this only from the update loop
     * inside the rendering thread. This means is it safe to modify the scene 
     * graph from this method.
     *
     * @param stateManager The state manager
     * @param app The application
     */
    public void initialize(AppStateManager stateManager, Application app);

    /**
     * @return True if <code>initialize()</code> was called on the state,
     * false otherwise.
     */
    public boolean isInitialized();

    /**
     * Enable or disable the functionality of the <code>AppState</code>.
     * The effect of this call depends on implementation. An 
     * <code>AppState</code> starts as being enabled by default.
     * A disabled <code>AppState</code>s does not get calls to
     * {@link #update(float)}, {@link #render(RenderManager)}, or
     * {@link #postRender()} from its {@link AppStateManager}.
     * 
     * @param active activate the AppState or not.
     */
    public void setEnabled(boolean active);
    
    /**
     * @return True if the <code>AppState</code> is enabled, false otherwise.
     * 
     * @see AppState#setEnabled(boolean)
     */
    public boolean isEnabled();

    /**
     * Called by {@link AppStateManager#attach(com.jme3.app.state.AppState) }
     * when transitioning this
     * <code>AppState</code> from <i>detached</i> to <i>initializing</i>.
     * <p>
     * There is no assumption about the thread from which this function is
     * called, therefore it is <b>unsafe</b> to modify the scene graph
     * from this method. Please use 
     * {@link #initialize(com.jme3.app.state.AppStateManager, com.jme3.app.Application) }
     * instead.
     *
     * @param stateManager State manager to which the state was attached to.
     */
    public void stateAttached(AppStateManager stateManager);

   /**
    * Called by {@link AppStateManager#detach(com.jme3.app.state.AppState) } 
    * when transitioning this
    * <code>AppState</code> from <i>running</i> to <i>terminating</i>.
    * <p>
    * There is no assumption about the thread from which this function is
    * called, therefore it is <b>unsafe</b> to modify the scene graph
    * from this method. Please use 
    * {@link #cleanup() }
    * instead.
    * 
    * @param stateManager The state manager from which the state was detached from.
    */
    public void stateDetached(AppStateManager stateManager);

    /**
     * Called to update the <code>AppState</code>. This method will be called 
     * every render pass if the <code>AppState</code> is both attached and enabled.
     *
     * @param tpf Time since the last call to update(), in seconds.
     */
    public void update(float tpf);

    /**
     * Render the state. This method will be called 
     * every render pass if the <code>AppState</code> is both attached and enabled.
     *
     * @param rm RenderManager
     */
    public void render(RenderManager rm);

    /**
     * Called after all rendering commands are flushed. This method will be called 
     * every render pass if the <code>AppState</code> is both attached and enabled.
     */
    public void postRender();

    /**
     * Called by {@link AppStateManager} when transitioning this
     * <code>AppState</code> from <i>terminating</i> to <i>detached</i>. This
     * method is called the following render pass after the <code>AppState</code> has 
     * been detached and is always called once and only once for each time
     * <code>initialize()</code> is called. Either when the <code>AppState</code>
     * is detached or when the application terminates (if it terminates normally).
     */
    public void cleanup();

}
