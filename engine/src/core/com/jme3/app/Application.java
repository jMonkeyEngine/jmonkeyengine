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
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioContext;
import com.jme3.audio.AudioRenderer;
import com.jme3.audio.Listener;
import com.jme3.input.*;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.system.JmeContext.Type;
import com.jme3.system.*;
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
    protected Timer timer = new NanoTimer();
    protected Camera cam;
    protected Listener listener;

    protected boolean inputEnabled = true;
    protected boolean pauseOnFocus = true;
    protected float speed = 1f;
    protected boolean paused = false;
    protected MouseInput mouseInput;
    protected KeyInput keyInput;
    protected JoyInput joyInput;
    protected TouchInput touchInput;
    protected InputManager inputManager;
    protected AppStateManager stateManager;

    private final ConcurrentLinkedQueue<AppTask<?>> taskQueue = new ConcurrentLinkedQueue<AppTask<?>>();

    /**
     * Create a new instance of <code>Application</code>.
     */
    public Application(){
        initStateManager();
    }

    /**
     * Returns true if pause on lost focus is enabled, false otherwise.
     * 
     * @return true if pause on lost focus is enabled
     * 
     * @see #setPauseOnLostFocus(boolean) 
     */
    public boolean isPauseOnLostFocus() {
        return pauseOnFocus;
    }

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
     */
    public void setPauseOnLostFocus(boolean pauseOnLostFocus) {
        this.pauseOnFocus = pauseOnLostFocus;
    }

    @Deprecated
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
                    url = Application.class.getClassLoader().getResource(assetCfg);
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
     * Set the display settings to define the display created.
     * <p>
     * Examples of display parameters include display pixel width and height,
     * color bit depth, z-buffer bits, anti-aliasing samples, and update frequency.
     * If this method is called while the application is already running, then
     * {@link #restart() } must be called to apply the settings to the display.
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

    /**
     * Sets the Timer implementation that will be used for calculating
     * frame times.  By default, Application will use the Timer as returned
     * by the current JmeContext implementation.
     */
    public void setTimer(Timer timer){
        this.timer = timer;
        
        if (timer != null) {
            timer.reset();
        }
        
        if (renderManager != null) {
            renderManager.setTimer(timer);
        }
    }
    
    public Timer getTimer(){
        return timer;
    } 

    private void initDisplay(){
        // aquire important objects
        // from the context
        settings = context.getSettings();
 
        // Only reset the timer if a user has not already provided one       
        if (timer == null) {
            timer = context.getTimer();
        }
       
        renderer = context.getRenderer();
    }

    private void initAudio(){
        if (settings.getAudioRenderer() != null && context.getType() != Type.Headless){
            audioRenderer = JmeSystem.newAudioRenderer(settings);
            audioRenderer.initialize();
            AudioContext.setAudioRenderer(audioRenderer);

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
        
        touchInput = context.getTouchInput();
        if (touchInput != null)
            touchInput.initialize();

        if (!settings.getBoolean("DisableJoysticks")){
            joyInput = context.getJoyInput();
            if (joyInput != null)
                joyInput.initialize();
        }

        inputManager = new InputManager(mouseInput, keyInput, joyInput, touchInput);
    }

    private void initStateManager(){
        stateManager = new AppStateManager(this);
        
        // Always register a ResetStatsState to make sure
        // that the stats are cleared every frame
        stateManager.attach(new ResetStatsState());
    }

    /**
     * @return The {@link AssetManager asset manager} for this application.
     */
    public AssetManager getAssetManager(){
        return assetManager;
    }

    /**
     * @return the {@link InputManager input manager}.
     */
    public InputManager getInputManager(){
        return inputManager;
    }

    /**
     * @return the {@link AppStateManager app state manager}
     */
    public AppStateManager getStateManager() {
        return stateManager;
    }

    /**
     * @return the {@link RenderManager render manager}
     */
    public RenderManager getRenderManager() {
        return renderManager;
    }

    /**
     * @return The {@link Renderer renderer} for the application
     */
    public Renderer getRenderer(){
        return renderer;
    }

    /**
     * @return The {@link AudioRenderer audio renderer} for the application
     */
    public AudioRenderer getAudioRenderer() {
        return audioRenderer;
    }

    /**
     * @return The {@link Listener listener} object for audio
     */
    public Listener getListener() {
        return listener;
    }

    /**
     * @return The {@link JmeContext display context} for the application
     */
    public JmeContext getContext(){
        return context;
    }

    /**
     * @return The {@link Camera camera} for the application
     */
    public Camera getCamera(){
        return cam;
    }

    /**
     * Starts the application in {@link Type#Display display} mode.
     * 
     * @see #start(com.jme3.system.JmeContext.Type) 
     */
    public void start(){
        start(JmeContext.Type.Display);
    }

    /**
     * Starts the application. 
     * Creating a rendering context and executing
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

    /**
     * Initializes the application's canvas for use.
     * <p>
     * After calling this method, cast the {@link #getContext() context} to 
     * {@link JmeCanvasContext},
     * then acquire the canvas with {@link JmeCanvasContext#getCanvas() }
     * and attach it to an AWT/Swing Frame.
     * The rendering thread will start when the canvas becomes visible on
     * screen, however if you wish to start the context immediately you
     * may call {@link #startCanvas() } to force the rendering thread
     * to start. 
     * 
     * @see JmeCanvasContext
     * @see Type#Canvas
     */
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

    /**
     * Starts the rendering thread after createCanvas() has been called.
     * <p>
     * Same as calling startCanvas(false)
     * 
     * @see #startCanvas(boolean) 
     */
    public void startCanvas(){
        startCanvas(false);
    }

    /**
     * Starts the rendering thread after createCanvas() has been called.
     * <p>
     * Calling this method is optional, the canvas will start automatically
     * when it becomes visible.
     * 
     * @param waitFor If true, the current thread will block until the 
     * rendering thread is running
     */
    public void startCanvas(boolean waitFor){
        context.create(waitFor);
    }

    /**
     * Internal use only. 
     */
    public void reshape(int w, int h){
        renderManager.notifyReshape(w, h);
    }

    /**
     * Restarts the context, applying any changed settings.
     * <p>
     * Changes to the {@link AppSettings} of this Application are not 
     * applied immediately; calling this method forces the context
     * to restart, applying the new settings.
     */
    public void restart(){
        context.setSettings(settings);
        context.restart();
    }

    /**
     * Requests the context to close, shutting down the main loop
     * and making necessary cleanup operations.
     * 
     * Same as calling stop(false)
     * 
     * @see #stop(boolean) 
     */
    public void stop(){
        stop(false);
    }

    /**
     * Requests the context to close, shutting down the main loop
     * and making necessary cleanup operations. 
     * After the application has stopped, it cannot be used anymore.
     */
    public void stop(boolean waitFor){
        logger.log(Level.FINE, "Closing application: {0}", getClass().getName());
        context.destroy(waitFor);
    }

    /**
     * Do not call manually.
     * Callback from ContextListener.
     * <p>
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

        // update timer so that the next delta is not too large
//        timer.update();
        timer.reset();

        // user code here..
    }

    /**
     * Internal use only.
     */
    public void handleError(String errMsg, Throwable t){
        logger.log(Level.SEVERE, errMsg, t);
        // user should add additional code to handle the error.
        stop(); // stop the application
    }

    /**
     * Internal use only.
     */
    public void gainFocus(){
        if (pauseOnFocus) {
            paused = false;
            context.setAutoFlushFrames(true);
            if (inputManager != null) {
                inputManager.reset();
            }
        }
    }

    /**
     * Internal use only.
     */
    public void loseFocus(){
        if (pauseOnFocus){
            paused = true;
            context.setAutoFlushFrames(false);
        }
    }

    /**
     * Internal use only.
     */
    public void requestClose(boolean esc){
        context.destroy(false);
    }

    /**
     * Enqueues a task/callable object to execute in the jME3
     * rendering thread. 
     * <p>
     * Callables are executed right at the beginning of the main loop.
     * They are executed even if the application is currently paused
     * or out of focus.
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
        // Make sure the audio renderer is available to callables
        AudioContext.setAudioRenderer(audioRenderer);
        
        AppTask<?> task = taskQueue.poll();
        toploop: do {
            if (task == null) break;
            while (task.isCancelled()) {
                task = taskQueue.poll();
                if (task == null) break toploop;
            }
            task.invoke();
        } while (((task = taskQueue.poll()) != null));
        
        /* I think the above is really just doing this:
        AppTask<?> task;
        while( (task = taskQueue.poll()) != null ) {
            if (!task.isCancelled()) {
                task.invoke();
            }
        }
        //...but it's hard to say for sure.  It's so twisted
        //up that I don't trust my eyes.  -pspeed
        */ 
    
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
        
        if (touchInput != null)
            touchInput.destroy();        

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

    /**
     * @return The GUI viewport. Which is used for the on screen
     * statistics and FPS.
     */
    public ViewPort getGuiViewPort() {
        return guiViewPort;
    }

    public ViewPort getViewPort() {
        return viewPort;
    }

}
