/*
 * 
 * Copyright (c) 2014-2024 jMonkeyEngine
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package om.jme3.app.state;

import com.jme3.app.Application;
import com.jme3.util.SafeArrayList;

/**
 *  An AppState that manages a set of child app states, making sure
 *  they are attached/detached and optional enabled/disabled with the
 *  parent state.
 *
 *  @author    Paul Speed
 */
public class CompositeAppState extends BaseAppState {

    private final SafeArrayList<AppStateEntry> states = new SafeArrayList<>(AppStateEntry.class);
    private boolean childrenEnabled;
    
    /**
     *  Since we manage attachmend/detachment possibly before
     *  initialization, we need to keep track of the stateManager we
     *  were given in stateAttached() in case we have to attach another
     *  child prior to initialization (but after we're attached).
     *  It's possible that we should actually be waiting for initialize
     *  to add these but I feel like there was some reason I did it this 
     *  way originally.  Past-me did not leave any clues.
     */
    private AppStateManager stateManager;
    private boolean attached;  
    
    public CompositeAppState( AppState... states ) {
        for( AppState a : states ) {
            this.states.add(new AppStateEntry(a, false));
        }
    }

    private int indexOf( AppState state ) {
        for( int i = 0; i < states.size(); i++ ) {
            AppStateEntry e = states.get(i);
            if( e.state == state ) {
                return i;
            }
        }
        return -1;
    }

    private AppStateEntry entry( AppState state ) {
        for( AppStateEntry e : states.getArray() ) {
            if( e.state == state ) {
                return e;
            }
        }
        return null;
    }

    protected <T extends AppState> T addChild( T state ) {
        return addChild(state, false);
    }
    
    protected <T extends AppState> T addChild( T state, boolean overrideEnable ) {
        if( indexOf(state) >= 0 ) {
            return state;
        }
        states.add(new AppStateEntry(state, overrideEnable));
        if( attached ) {
            stateManager.attach(state);
        }
        return state;   
    }
 
    protected void removeChild( AppState state ) {
        int index = indexOf(state);
        if( index < 0 ) {
            return;
        }
        states.remove(index);
        if( attached ) {
            stateManager.detach(state);
        }
    }
    
    protected <T extends AppState> T getChild( Class<T> stateType ) {
        for( AppStateEntry e : states.getArray() ) {
            if( stateType.isInstance(e.state) ) {
                return stateType.cast(e.state);
            }
        }
        return null;
    }

    protected void clearChildren() {
        for( AppStateEntry e : states.getArray() ) {
            removeChild(e.state);
        }
    }
    
    @Override 
    public void stateAttached( AppStateManager stateManager ) {
        this.stateManager = stateManager;
        for( AppStateEntry e : states.getArray() ) {
            stateManager.attach(e.state);
        }
        this.attached = true;
    }
    
    @Override
    public void stateDetached( AppStateManager stateManager ) {
        // Reverse order
        for( int i = states.size() - 1; i >= 0; i-- ) {
            stateManager.detach(states.get(i).state);
        }
        this.attached = false;
        this.stateManager = null;
    }

    protected void setChildrenEnabled( boolean b ) {
        if( childrenEnabled == b ) {
            return;
        }
        childrenEnabled = b;
        for( AppStateEntry e : states.getArray() ) {
            e.setEnabled(b);
        }
    }

    /**
     *  Overrides the automatic synching of a child's enabled state.
     *  When override is true, a child will remember its old state when
     *  the parent's enabled state is false so that when the parent is
     *  re-enabled the child can resume its previous enabled state.  This
     *  is useful for the cases where a child may want to be disabled
     *  independent of the parent... and then not automatically become
     *  enabled just because the parent does.
     *  Currently, the parent's disabled state always disables the children,
     *  too.  Override is about remembering the child's state before that
     *  happened and restoring it when the 'family' is enabled again as a whole.
     */
    public void setOverrideEnabled( AppState state, boolean override ) {
        AppStateEntry e = entry(state);
        if( e == null ) {
            throw new IllegalArgumentException("State not managed:" + state);
        }
        if( override ) {
            e.override = true;
        } else {
            e.override = false;
            e.state.setEnabled(isEnabled());
        }   
    }

    @Override
    protected void initialize(Application app) {
    }

    @Override
    protected void cleanup(Application app) {
    }

    @Override
    protected void onEnable() {
        setChildrenEnabled(true);
    }

    @Override
    protected void onDisable() {
        setChildrenEnabled(false);
    }
    
    private class AppStateEntry {
        AppState state;
        boolean enabled;
        boolean override;
        
        public AppStateEntry( AppState state, boolean overrideEnable ) {
            this.state = state;
            this.override = overrideEnable;
            this.enabled = state.isEnabled();
        }
        
        public void setEnabled( boolean b ) {
 
            if( override ) {
                if( b ) {
                    // Set it to whatever its enabled state
                    // was before going disabled last time.
                    state.setEnabled(enabled);
                } else {
                    // We are going to set enabled to false
                    // but keep track of what it was before we did
                    // that
                    this.enabled = state.isEnabled();
                    state.setEnabled(false);
                }               
            } else {
                // Just synch it always
                state.setEnabled(b);
            }
        }
    }
}

