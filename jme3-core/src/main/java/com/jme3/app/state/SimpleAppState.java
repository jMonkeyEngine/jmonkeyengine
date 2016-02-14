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
import com.jme3.app.LostFocusBehavior;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.audio.Listener;
import com.jme3.input.FlyByCamera;
import com.jme3.input.InputManager;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.system.JmeContext;
import com.jme3.system.Timer;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * <code>SimpleAppState</code> provides a
 * basic implementation of the <code>AppState</code>
 * interface to allow for easy access to all
 * tools and objects available in <code>SimpleApplication</code> 
 * in addition to higher quality lifecycle management.
 * 
 * SimpleAppState is entirely thread safe with the
 * exception of the methods that are normally
 * provided with SimpleApplication. SimpleAppState
 * can also only be used with an application that
 * subclasses SimpleApplication rather than one that 
 * simply subclasses Application.
 * 
 * The usage of this kind of AppState is slightly
 * different from that of AbstractAppState. Specifically,
 * most of the methods that are normally available are
 * reserved for background "black box" usabfe in this
 * class and it's subclasses. Instead, more flexible 
 * alternatives are provided such as onInit() and onUpdate.
 * 
 * When using this appstate all of the methods that are 
 * normally available only through your SimpleApplication
 * subclass such as <code>getFlyByCam()</code> are available to be used
 * in subclasses of this AppState as well.
 * 
 * @param <ApplicationType> The type of the application that this AppState is intended to be used with. Allows the getApplication() method to return this type.
 * 
 * @author john01dav
 */
public abstract class SimpleAppState<ApplicationType extends SimpleApplication> implements AppState{
    public enum AppStateState {PRE_INIT, ENABLED, DISABLED, POST_DEINIT};
    private volatile AppStateState state = AppStateState.PRE_INIT;
    private volatile ApplicationType application;
    
    @Override
    public final void initialize(AppStateManager stateManager, Application application) throws IllegalStateException{        
        try{
            this.application = ((ApplicationType) application);
        }catch(ClassCastException e){
            throw new IllegalStateException("SimpleAppState may only be used with the class that was provided in it's generic type.", e);
        }
        
        setEnabled(true);
        
        onInit();
    }

    @Override
    public final boolean isInitialized(){
        return state == AppStateState.ENABLED || state == AppStateState.DISABLED;
    }

    @Override
    public final void setEnabled(boolean active){
        state = active ? AppStateState.ENABLED : AppStateState.DISABLED;
        
        if(active) onEnable();
        else onDisable();
    }

    @Override
    public final boolean isEnabled(){
        return state == AppStateState.ENABLED;
    }

    @Override
    public final void update(float tpf){
        onUpdate(tpf);
        if(state == AppStateState.ENABLED){
            onEnabledUpdate(tpf);
        }
    }

    @Override
    public final void cleanup(){
        setEnabled(false);
        onDeinit();
    }
    
        
    //extenders may implement these methods rather than the default appstate methods
    
    /**
     * This method is called when this AppState
     * is initialized. It will always be called
     * on the main jMonkeyEngine thread.
     */
    protected void onInit(){}
    
    /**
     * This method is called when this AppState
     * is enabled -- including when it is first
     * attached. It will always be called
     * on the main jMonkeyEngine thread.
     */
    protected void onEnable(){}
    
    /**
     * This method is called when this AppState
     * is disabled -- including when it is first
     * detached. It will always be called
     * on the main jMonkeyEngine thread.
     */
    protected void onDisable(){}
    
    /**
     * This method is called when this AppState
     * updates and is enabled at the same time.
     * This method will not be called before
     * the SimpleApplication replacement methods
     * are available. It will always be called
     * on the main jMonkeyEngine thread.
     * @param tpf The time, in seconds, that the last update took.
     */
    protected void onEnabledUpdate(float tpf){}
    
    /**
     * This method is called when this AppState
     * updates whether or not it is enabled. 
     * It will always be called on the main jMonkeyEngine thread.
     * @param tpf The time, in seconds, that the last update took.
     */
    protected void onUpdate(float tpf){}
    
    /**
     * This method is called when this AppState is deinitialized. 
     * It will always be called on the main jMonkeyEngine thread.
     */
    protected void onDeinit(){}
    
    //these default appstate methods are still available for use by subclasses
    @Override public void stateAttached(AppStateManager stateManager){}
    @Override public void stateDetached(AppStateManager stateManager){}
    @Override public void render(RenderManager rm){}
    @Override public void postRender(){}

    /**
     * Gets the current state of this AppState
     * @return the current state of this AppState
     */
    public final AppStateState getState(){
        return state;
    }
    
    public final <T extends AppState> T getState(Class<T> clazz){
        return getStateManager().getState(clazz);
    }

    public ApplicationType getApplication(){
        return application;
    }
    
    public FlyByCamera getFlyByCamera(){
        return application.getFlyByCamera();
    }

    public final Node getGuiNode() {
        return application.getGuiNode();
    }

    public final Node getRootNode() {
        return application.getRootNode();
    }

    public final boolean isShowSettings() {
        return application.isShowSettings();
    }

    public final void setShowSettings(boolean showSettings) {
        application.setShowSettings(showSettings);
    }

    public final void setDisplayFps(boolean show) {
        application.setDisplayFps(show);
    }

    public final void setDisplayStatView(boolean show) {
        application.setDisplayStatView(show);
    }

    public final LostFocusBehavior getLostFocusBehavior() {
        return application.getLostFocusBehavior();
    }

    public final boolean isPauseOnLostFocus() {
        return application.isPauseOnLostFocus();
    }

    public final void setPauseOnLostFocus(boolean pauseOnLostFocus) {
        application.setPauseOnLostFocus(pauseOnLostFocus);
    }

    public final Timer getTimer() {
        return application.getTimer();
    }

    public final AssetManager getAssetManager() {
        return application.getAssetManager();
    }

    public final InputManager getInputManager() {
        return application.getInputManager();
    }

    public final AppStateManager getStateManager() {
        return application.getStateManager();
    }

    public final RenderManager getRenderManager() {
        return application.getRenderManager();
    }

    public final Renderer getRenderer() {
        return application.getRenderer();
    }

    public final AudioRenderer getAudioRenderer() {
        return application.getAudioRenderer();
    }

    public final Listener getListener() {
        return application.getListener();
    }

    public final JmeContext getContext() {
        return application.getContext();
    }

    public final Camera getCamera() {
        return application.getCamera();
    }

    public final void setAppProfiler(AppProfiler prof) {
        application.setAppProfiler(prof);
    }

    public final AppProfiler getAppProfiler() {
        return application.getAppProfiler();
    }

    public final void reshape(int w, int h) {
        application.reshape(w, h);
    }

    public final void restart() {
        application.restart();
    }

    public final void stop() {
        application.stop();
    }

    public final void stop(boolean waitFor) {
        application.stop(waitFor);
    }

    public final <V> Future<V> enqueue(Callable<V> callable) {
        return application.enqueue(callable);
    }

    public final ViewPort getGuiViewPort() {
        return application.getGuiViewPort();
    }

    public final ViewPort getViewPort() {
        return application.getViewPort();
    }
    
    /**
     * This method performs identically to SimpleApplication's
     * enqueue method except that it accepts a Runnable rather
     * than a Callable to allow for easier and cleaner usage of 
     * Lambda expressions.
     * @param runnable 
     */
    public final void enqueue(Runnable runnable){
        enqueue(new RunnableWrapper(runnable));
    }
    
    private class RunnableWrapper implements Callable<Object>{
        private final Runnable runnable;

        public RunnableWrapper(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public Object call(){
            runnable.run();
            return null;
        }
        
    }
    
}
