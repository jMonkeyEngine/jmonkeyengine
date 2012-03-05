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
import com.jme3.util.SafeArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The <code>AppStateManager</code> holds a list of {@link AppState}s which
 * it will update and render.<br/>
 * When an {@link AppState} is attached or detached, the
 * {@link AppState#stateAttached(com.jme3.app.state.AppStateManager) } and
 * {@link AppState#stateDetached(com.jme3.app.state.AppStateManager) } methods
 * will be called respectively.
 *
 * <p>The lifecycle for an attached AppState is as follows:</p>
 * <ul>
 * <li>stateAttached() : called when the state is attached on the thread on which
 *                       the state was attached.
 * <li>initialize() : called ONCE on the render thread at the beginning of the next
 *                    AppStateManager.update().
 * <li>stateDetached() : called when the state is attached on the thread on which
 *                       the state was detached.  This is not necessarily on the
 *                       render thread and it is not necessarily safe to modify
 *                       the scene graph, etc..
 * <li>cleanup() : called ONCE on the render thread at the beginning of the next update
 *                 after the state has been detached or when the application is 
 *                 terminating.  
 * </ul> 
 *
 * @author Kirill Vainer, Paul Speed
 */
public class AppStateManager {

    /**
     *  List holding the attached app states that are pending
     *  initialization.  Once initialized they will be added to
     *  the running app states.  
     */
    private final SafeArrayList<AppState> initializing = new SafeArrayList<AppState>(AppState.class);
    
    /**
     *  Holds the active states once they are initialized.  
     */
    private final SafeArrayList<AppState> states = new SafeArrayList<AppState>(AppState.class);
    
    /**
     *  List holding the detached app states that are pending
     *  cleanup.  
     */
    private final SafeArrayList<AppState> terminating = new SafeArrayList<AppState>(AppState.class);
 
    // All of the above lists need to be thread safe but access will be
    // synchronized separately.... but always on the states list.  This
    // is to avoid deadlocking that may occur and the most common use case
    // is that they are all modified from the same thread anyway.
    
    private final Application app;
    private AppState[] stateArray;

    public AppStateManager(Application app){
        this.app = app;
    }

    protected AppState[] getInitializing() { 
        synchronized (states){
            return initializing.getArray();
        }
    } 

    protected AppState[] getTerminating() { 
        synchronized (states){
            return terminating.getArray();
        }
    } 

    protected AppState[] getStates(){
        synchronized (states){
            return states.getArray();
        }
    }

    /**
     * Attach a state to the AppStateManager, the same state cannot be attached
     * twice.
     *
     * @param state The state to attach
     * @return True if the state was successfully attached, false if the state
     * was already attached.
     */
    public boolean attach(AppState state){
        synchronized (states){
            if (!states.contains(state) && !initializing.contains(state)){
                state.stateAttached(this);
                initializing.add(state);
                return true;
            }else{
                return false;
            }
        }
    }

    /**
     * Detaches the state from the AppStateManager. 
     *
     * @param state The state to detach
     * @return True if the state was detached successfully, false
     * if the state was not attached in the first place.
     */
    public boolean detach(AppState state){
        synchronized (states){
            if (states.contains(state)){
                state.stateDetached(this);
                states.remove(state);
                terminating.add(state);
                return true;
            } else if(initializing.contains(state)){
                state.stateDetached(this);
                initializing.remove(state);
                return true;
            }else{
                return false;
            }
        }
    }

    /**
     * Check if a state is attached or not.
     *
     * @param state The state to check
     * @return True if the state is currently attached to this AppStateManager.
     * 
     * @see AppStateManager#attach(com.jme3.app.state.AppState)
     */
    public boolean hasState(AppState state){
        synchronized (states){
            return states.contains(state) || initializing.contains(state);
        }
    }

    /**
     * Returns the first state that is an instance of subclass of the specified class.
     * @param <T>
     * @param stateClass
     * @return First attached state that is an instance of stateClass
     */
    public <T extends AppState> T getState(Class<T> stateClass){
        synchronized (states){
            AppState[] array = getStates();
            for (AppState state : array) {
                if (stateClass.isAssignableFrom(state.getClass())){
                    return (T) state;
                }
            }
            
            // This may be more trouble than its worth but I think
            // it's necessary for proper decoupling of states and provides
            // similar behavior to before where a state could be looked
            // up even if it wasn't initialized. -pspeed
            array = getInitializing();
            for (AppState state : array) {
                if (stateClass.isAssignableFrom(state.getClass())){
                    return (T) state;
                }
            }
        }
        return null;
    }

    protected void initializePending(){
        AppState[] array = getInitializing();
        if (array.length == 0)
            return;
            
        synchronized( states ) {
            // Move the states that will be initialized
            // into the active array.  In all but one case the
            // order doesn't matter but if we do this here then
            // a state can detach itself in initialize().  If we
            // did it after then it couldn't.
            List<AppState> transfer = Arrays.asList(array);         
            states.addAll(transfer);
            initializing.removeAll(transfer);
        }        
        for (AppState state : array) {
            state.initialize(this, app);
        }
    }
    
    protected void terminatePending(){
        AppState[] array = getTerminating();
        if (array.length == 0)
            return;
            
        for (AppState state : array) {
            state.cleanup();
        }        
        synchronized( states ) {
            // Remove just the states that were terminated...
            // which might now be a subset of the total terminating
            // list.
            terminating.removeAll(Arrays.asList(array));         
        }
    }    

    /**
     * Calls update for attached states, do not call directly.
     * @param tpf Time per frame.
     */
    public void update(float tpf){
    
        // Cleanup any states pending
        terminatePending();

        // Initialize any states pending
        initializePending();

        // Update enabled states    
        AppState[] array = getStates();
        for (AppState state : array){
            if (state.isEnabled()) {
                state.update(tpf);
            }
        }
    }

    /**
     * Calls render for all attached and initialized states, do not call directly.
     * @param rm The RenderManager
     */
    public void render(RenderManager rm){
        AppState[] array = getStates();
        for (AppState state : array){
            if (state.isEnabled()) {
                state.render(rm);
            }
        }
    }

    /**
     * Calls render for all attached and initialized states, do not call directly.
     */
    public void postRender(){
        AppState[] array = getStates();
        for (AppState state : array){
            if (state.isEnabled()) {
                state.postRender();
            }
        }
    }

    /**
     * Calls cleanup on attached states, do not call directly.
     */
    public void cleanup(){
        AppState[] array = getStates();
        for (AppState state : array){
            state.cleanup();
        }
    }    
}
