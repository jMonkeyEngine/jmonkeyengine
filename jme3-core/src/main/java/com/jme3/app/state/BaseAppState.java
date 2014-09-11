/*
 * Copyright (c) 2014 jMonkeyEngine
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
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 *  A base app state implementation the provides more built-in
 *  management convenience than AbstractAppState, including methods
 *  for enable/disable/initialize state management.
 *  The abstract enable() and disable() methods are called
 *  appropriately during initialize(), terminate(), or setEnabled()
 *  depending on the mutual state of "initialized" and "enabled".
 *  
 *  <p>initialize() and terminate() can be used by subclasses to
 *  manage resources that should exist the entire time that the 
 *  app state is attached.  This is useful for resources that might
 *  be expensive to create or load.</p>
 *
 *  <p>enable()/disable() can be used for managing things that
 *  should only exist while the state is enabled.  Prime examples
 *  would be scene graph attachment or input listener attachment.</p>
 *
 *  <p>The base class logic is such that disable() will always be called
 *  before cleanup() if the state is enabled.  Likewise, enable()
 *  will always be called after initialize() if the state is enable().
 *  enable()/disable() are also called appropriate when setEnabled()
 *  is called that changes the enabled state AND if the state is attached.
 *  In other words, enable()/disable() are only ever called on an already 
 *  attached state.</p>
 *
 *  <p>It is technically safe to do all initialization and cleanup in
 *  the enable()/disable() methods.  Choosing to use initialize()
 *  and cleanup() for this is a matter of performance specifics for the
 *  implementor.</p>
 *
 *  @author    Paul Speed
 */
public abstract class BaseAppState implements AppState {

    static final Logger log = Logger.getLogger(BaseAppState.class.getName());

    private Application app;
    private boolean initialized;
    private boolean enabled = true;

    /**
     *  Called during initialization once the app state is
     *  attached and before enable() is called.
     */
    protected abstract void initialize( Application app );
    
    /**
     *  Called after the app state is detached or during
     *  application shutdown if the state is still attached.
     *  disable() is called before this cleanup() method if
     *  the state is enabled at the time of cleanup.
     */
    protected abstract void cleanup( Application app );
    
    /**
     *  Called when the state is fully enabled, ie: is attached
     *  and isEnabled() is true or when the setEnabled() status
     *  changes after the state is attached.
     */
    protected abstract void enable();
    
    /**
     *  Called when the state was previously enabled but is
     *  now disabled either because setEnabled(false) was called
     *  or the state is being cleaned up.
     */
    protected abstract void disable();

    /**
     *  Do not call directly: Called by the state manager to initialize this 
     *  state post-attachment.
     *  This implementation calls initialize(app) and then enable() if the
     *  state is enabled.
     */
    public final void initialize( AppStateManager stateManager, Application app ) {
        log.log(Level.FINEST, "initialize():{0}", this);

        this.app = app;
        initialized = true;
        initialize(app);
        if( isEnabled() ) {
            log.log(Level.FINEST, "enable():{0}", this);
            enable();
        }
    }

    public final boolean isInitialized() {
        return initialized;
    }

    public final Application getApplication() {
        return app;
    }

    public final AppStateManager getStateManager() {
        return app.getStateManager();
    }

    public final <T extends AppState> T getState( Class<T> type ) {
        return getStateManager().getState(type);
    }

    public final void setEnabled( boolean enabled )
    {
        if( this.enabled == enabled )
            return;
        this.enabled = enabled;
        if( !isInitialized() )
            return;
        if( enabled ) {
            log.log(Level.FINEST, "enable():{0}", this);
            enable();
        } else {
            log.log(Level.FINEST, "disable():{0}", this);
            disable();
        }
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public void stateAttached( AppStateManager stateManager ) {
    }

    public void stateDetached( AppStateManager stateManager ) {
    }

    public void update( float tpf ) {
    }

    public void render( RenderManager rm ) {
    }

    public void postRender() {
    }

    /**
     *  Do not call directly: Called by the state manager to terminate this 
     *  state post-detachment or during state manager termination.
     *  This implementation calls disable() if the state is enabled and
     *  then cleanup(app).
     */
    public final void cleanup() {
        log.log(Level.FINEST, "cleanup():{0}", this);

        if( isEnabled() ) {
            log.log(Level.FINEST, "disable():{0}", this);
            disable();
        }
        cleanup(app);
        initialized = false;
    }
}
