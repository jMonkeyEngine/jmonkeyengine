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

package com.jme3.app.state;

import com.jme3.app.Application;
import com.jme3.renderer.RenderManager;

/**
 * AppState represents a continously executing code inside the main loop.
 * An <code>AppState</code> can track when it is attached to the 
 * {@link AppStateManager} or when it is detached. <br/><code>AppState</code>s
 * are initialized in the render thread, upon a call to {@link AppState#initialize(com.jme3.app.state.AppStateManager, com.jme3.app.Application) }
 * and are de-initialized upon a call to {@link AppState#cleanup()}. 
 * Implementations should return the correct value with a call to 
 * {@link AppState#isInitialized() } as specified above.<br/>
 * 
 *
 * @author Kirill Vainer
 */
public interface AppState {

    /**
     * Called to initialize the AppState.
     *
     * @param stateManager The state manager
     * @param app
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
     * Called when the state was attached.
     *
     * @param stateManager State manager to which the state was attached to.
     */
    public void stateAttached(AppStateManager stateManager);

   /**
    * Called when the state was detached.
    *
    * @param stateManager The state manager from which the state was detached from.
    */
    public void stateDetached(AppStateManager stateManager);

    /**
     * Called to update the state.
     *
     * @param tpf Time per frame.
     */
    public void update(float tpf);

    /**
     * Render the state.
     *
     * @param rm RenderManager
     */
    public void render(RenderManager rm);

    /**
     * Called after all rendering commands are flushed.
     */
    public void postRender();

    /**
     * Cleanup the game state. 
     */
    public void cleanup();

}
