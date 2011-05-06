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

package com.jme3.app;

import com.jme3.app.state.AppStateManager;
import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.system.*;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.Renderer;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.audio.Listener;
import com.jme3.input.InputManager;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>Application</code> class represents an instance of a
 * real-time 3D rendering jME application.
 *
 * An <code>Application</code> provides all the tools that are commonly used in jME3
 * applications.
 *
 * jME3 applications should extend this class and call start() to begin the
 * application.
 * 
 */
public class Application implements SystemListener {

    private static final Logger logger = Logger.getLogger(Application.class.getName());

    protected AssetManager assetManager;
    
    protected AudioRenderer audioRenderer;
    protected Renderer renderer;
    protected RenderManager renderManager;
    protected ViewPort viewPort;
    protected ViewPort guiViewPort;

    protected JmeContext context;
    protected AppSettings settings;
    protected Timer timer;
    protected Camera cam;
    protected Listener listener;

    protected boolean inputEnabled = true;
    protected boolean pauseOnFocus = true;
    protected float speed = 1f;
    protected boolean paused = false;
    protected MouseInput mouseInput;
    protected KeyInput keyInput;
    protected JoyInput joyInput;
    protected InputManager inputManager;
    protected AppStateManager stateManager;

    private final ConcurrentLinkedQueue<AppTask<?>> taskQueue = new ConcurrentLinkedQueue<AppTask<?>>();

    /**
     * Create a new instance of <code>Application</code>.
     */
    public Application(){
    }

    public boolean isPauseOnLostFocus() {
        return pauseOnFocus;
    }

    public void setPauseOnLostFocus(boolean pauseOnLostFocus) {
        this.pauseOnFocus = pauseOnLostFocus;
    }

    public void setAssetManager(AssetManager assetManager){
        if (this.assetManager != null)
            throw new IllegalStateException("Can only set asset manager"
                                          + " before initialization.");

        this.assetManager = assetManager;
    }

    private void initAssetManager(){
        if (settings != null){
            String assetCfg = settings.getString("AssetConfigURL");
            if (assetCfg != null){
                URL url = null;
                try {
                    url = new URL(assetCfg);
                } catch (MalformedURLException ex) {
                }
                if (url == null) {
                    url = Application.class.getResource(assetCfg);
                    if (url == null) {
                        logger.log(Level.SEVERE, "Unable to access AssetConfigURL in asset config:{0}", assetCfg);
                        return;
                    }
                }
                assetManager = JmeSystem.newAssetManager(url);
            }
        }
        if (assetManager == null){
            assetManager = JmeSystem.newAssetManager(
                    Thread.currentThread().getContextClassLoader()
                    .getResource("com/jme3/asset/Desktop.cfg"));
        }
    }

    /**
     * Set the display settings to define the display created. Examples of
     * display parameters include display pixel width and height,
     * color bit depth, z-buffer bits, anti-aliasing samples, and update frequency.
     *
     * @param settings The settings to set.
     */
    public void setSettings(AppSettings settings){
        this.settings = settings;
        if (context != null && settings.useInput() != inputEnabled){
            // may need to create or destroy input based
            // on settings change
            inputEnabled = !inputEnabled;
            if (inputEnabled){
                initInput();
            }else{
                destroyInput();
            }
        }else{
            inputEnabled = settings.useInput();
        }
    }

    private void initDisplay(){
        // aquire important objects
        // from the context
        settings = context.getSettings();
        timer = context.getTimer();
       
        renderer = context.getRenderer();
    }

    private void initAudio(){
        if (settings.getAudioRenderer() != null){
            audioRenderer = JmeSystem.newAudioRenderer(settings);
            audioRenderer.initialize();

            listener = new Listener();
            audioRenderer.setListener(listener);
        }
    }

    /**
     * Creates the camera to use for rendering. Default values are perspective
     * projection with 45° field of view, with near and far values 1 and 1000
     * units respectively.
     */
    private void initCamera(){
        cam = new Camera(settings.getWidth(), settings.getHeight());

        cam.setFrustumPerspective(45f, (float)cam.getWidth() / cam.getHeight(), 1f, 1000f);
        cam.setLocation(new Vector3f(0f, 0f, 10f));
        cam.lookAt(new Vector3f(0f, 0f, 0f), Vector3f.UNIT_Y);

        renderManager = new RenderManager(renderer);
        //Remy - 09/14/2010 setted the timer in the renderManager
        renderManager.setTimer(timer);
        viewPort = renderManager.createMainView("Default", cam);
        viewPort.setClearFlags(true, true, true);

        // Create a new cam for the gui
        Camera guiCam = new Camera(settings.getWidth(), settings.getHeight());
        guiViewPort = renderManager.createPostView("Gui Default", guiCam);
        guiViewPort.setClearFlags(false, false, false);
    }

    /**
     * Initializes mouse and keyboard input. Also
     * initializes joystick input if joysticks are enabled in the
     * AppSettings.
     */
    private void initInput(){
        mouseInput = context.getMouseInput();
        if (mouseInput != null)
            mouseInput.initialize();

        keyInput = context.getKeyInput();
        if (keyInput != null)
            keyInput.initialize();

        if (!settings.getBoolean("DisableJoysticks")){
            joyInput = context.getJoyInput();
            if (joyInput != null)
                joyInput.initialize();
        }

        inputManager = new InputManager(mouseInput, keyInput, joyInput);
    }

    private void initStateManager(){
        stateManager = new AppStateManager(this);
    }

    /**
     * @return The asset manager for this application.
     */
    public AssetManager getAssetManager(){
        return assetManager;
    }

    /**
     * @return the input manager.
     */
    public InputManager getInputManager(){
        return inputManager;
    }

    /**
     * @return the app state manager
     */
    public AppStateManager getStateManager() {
        return stateManager;
    }

    /**
     * @return the render manager
     */
    public RenderManager getRenderManager() {
        return renderManager;
    }

    /**
     * @return The renderer for the application, or null if was not started yet.
     */
    public Renderer getRenderer(){
        return renderer;
    }

    /**
     * @return The audio renderer for the application, or null if was not started yet.
     */
    public AudioRenderer getAudioRenderer() {
        return audioRenderer;
    }

    /**
     * @return The listener object for audio
     */
    public Listener getListener() {
        return listener;
    }

    /**
     * @return The display context for the application, or null if was not
     * started yet.
     */
    public JmeContext getContext(){
        return context;
    }

    /**
     * @return The camera for the application, or null if was not started yet.
     */
    public Camera getCamera(){
        return cam;
    }

    /**
     * Starts the application as a display.
     */
    public void start(){
        start(JmeContext.Type.Display);
    }

    /**
     * Starts the application. Creating a rendering context and executing
     * the main loop in a separate thread.
     */
    public void start(JmeContext.Type contextType){
        if (context != null && context.isCreated()){
            logger.warning("start() called when application already created!");
            return;
        }

        if (settings == null){
            settings = new AppSettings(true);
        }
        
        logger.log(Level.FINE, "Starting application: {0}", getClass().getName());
        context = JmeSystem.newContext(settings, contextType);
        context.setSystemListener(this);
        context.create(false);
    }

    public void createCanvas(){
        if (context != null && context.isCreated()){
            logger.warning("createCanvas() called when application already created!");
            return;
        }

        if (settings == null){
            settings = new AppSettings(true);
        }

        logger.log(Level.FINE, "Starting application: {0}", getClass().getName());
        context = JmeSystem.newContext(settings, JmeContext.Type.Canvas);
        context.setSystemListener(this);
    }

    public void startCanvas(){
        startCanvas(false);
    }

    public void startCanvas(boolean waitFor){
        context.create(waitFor);
    }

    public void reshape(int w, int h){
        renderManager.notifyReshape(w, h);
    }

    public void restart(){
        context.setSettings(settings);
        context.restart();
    }

    public void stop(){
        stop(false);
    }

    /**
     * Requests the display to close, shutting down the main loop
     * and making neccessary cleanup operations.
     */
    public void stop(boolean waitFor){
        logger.log(Level.FINE, "Closing application: {0}", getClass().getName());
        context.destroy(waitFor);
    }

    /**
     * Do not call manually.
     * Callback from ContextListener.
     *
     * Initializes the <code>Application</code>, by creating a display and
     * default camera. If display settings are not specified, a default
     * 640x480 display is created. Default values are used for the camera;
     * perspective projection with 45° field of view, with near
     * and far values 1 and 1000 units respectively.
     */
    public void initialize(){
        if (assetManager == null){
            initAssetManager();
        }

        initDisplay();
        initCamera();
        
        if (inputEnabled){
            initInput();
        }
        initAudio();
        initStateManager();

        // update timer so that the next delta is not too large
//        timer.update();
        timer.reset();

        // user code here..
    }

    public void handleError(String errMsg, Throwable t){
        logger.log(Level.SEVERE, errMsg, t);
        // user should add additional code to handle the error.
        stop(); // stop the application
    }

    public void gainFocus(){
        if (pauseOnFocus){
            paused = false;
            context.setAutoFlushFrames(true);
            if (inputManager != null)
                inputManager.reset();
        }
    }

    public void loseFocus(){
        if (pauseOnFocus){
            paused = true;
            context.setAutoFlushFrames(false);
        }
    }

    public void requestClose(boolean esc){
        context.destroy(false);
    }

    /**
     * Enqueues a task/callable object to execute in the jME3
     * rendering thread.
     */
    public <V> Future<V> enqueue(Callable<V> callable) {
        AppTask<V> task = new AppTask<V>(callable);
        taskQueue.add(task);
        return task;
    }

    /**
     * Do not call manually.
     * Callback from ContextListener.
     */
    public void update(){
        AppTask<?> task = taskQueue.poll();
        toploop: do {
            if (task == null) break;
            while (task.isCancelled()) {
                task = taskQueue.poll();
                if (task == null) break toploop;
            }
            task.invoke();
        } while (((task = taskQueue.poll()) != null));
    
        if (speed == 0 || paused)
            return;

        timer.update();

        if (inputEnabled){
            inputManager.update(timer.getTimePerFrame());
        }

        if (audioRenderer != null){
            audioRenderer.update(timer.getTimePerFrame());
        }

        // user code here..
    }

    protected void destroyInput(){
        if (mouseInput != null)
            mouseInput.destroy();

        if (keyInput != null)
            keyInput.destroy();

        if (joyInput != null)
            joyInput.destroy();

        inputManager = null;
    }

    /**
     * Do not call manually.
     * Callback from ContextListener.
     */
    public void destroy(){
        stateManager.cleanup();
        
        destroyInput();
        if (audioRenderer != null)
            audioRenderer.cleanup();
        
        timer.reset();
    }

    public ViewPort getGuiViewPort() {
        return guiViewPort;
    }

    public ViewPort getViewPort() {
        return viewPort;
    }

}
