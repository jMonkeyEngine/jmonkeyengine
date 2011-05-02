/*
 * Copyright (c) 2003-2009 jMonkeyEngine
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

package com.jme3.system.android;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.view.SurfaceHolder;
import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.android.AndroidInput;
//import com.jme3.renderer.android.OGLESRenderer;
import com.jme3.renderer.android.OGLESShaderRenderer;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.SystemListener;
import com.jme3.system.Timer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class OGLESContext implements JmeContext, GLSurfaceView.Renderer {

    private static final Logger logger = Logger.getLogger(OGLESContext.class.getName());

    protected AtomicBoolean created = new AtomicBoolean(false);
    protected AppSettings settings = new AppSettings(true);

	/* < OpenGL ES 2.0 * */
	//protected OGLESRenderer renderer;
	/* >= OpenGL ES 2.0 (Android 2.2+) */
	protected OGLESShaderRenderer renderer;

    protected Timer timer;
    protected SystemListener listener;

    protected AtomicBoolean needClose = new AtomicBoolean(false);
    protected boolean wasActive = false;
    protected int frameRate = 0;
    protected boolean autoFlush = true;

    protected AndroidInput view;

    public OGLESContext(){
    }

    public Type getType() {
        return Type.Display;
    }

    public GLSurfaceView createView(Activity activity){
        view = new AndroidInput(activity);

	/*
	 * Requesting client version from GLSurfaceView which is extended by
	 * AndroidInput.
	 * This is required to get OpenGL ES 2.0
	 */

	logger.info("setEGLContextClientVersion(2)");
	view.setEGLContextClientVersion(2);
	logger.info("setEGLContextClientVersion(2) ... done.");

        //RGB565, Depth16
        view.setEGLConfigChooser(5, 6, 5, 0, 16, 0);
        view.setFocusableInTouchMode(true);
        view.setFocusable(true);
        view.getHolder().setType(SurfaceHolder.SURFACE_TYPE_GPU);
//        view.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR);
//                         | GLSurfaceView.DEBUG_LOG_GL_CALLS);
   		view.setRenderer(this);
        return view;

    }

    protected void applySettings(AppSettings setting){
    }

    protected void initInThread(GL10 gl){
        logger.info("Display created.");
        logger.fine("Running on thread: "+Thread.currentThread().getName());

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread thread, Throwable thrown) {
                listener.handleError("Uncaught exception thrown in "+thread.toString(), thrown);
            }
        });

        created.set(true);

        timer = new AndroidTimer();

        renderer = new OGLESShaderRenderer(gl);
	applySettingsToRenderer(renderer, settings);

        renderer.initialize();
        listener.initialize();

	// OGLESShaderRenderer does not support guiView yet
	// forcefully remove all gui nodes

	if (listener instanceof com.jme3.app.SimpleApplication) {
		((com.jme3.app.SimpleApplication) listener).getGuiNode().detachAllChildren();
	}
    }

    /**
     * De-initialize in the OpenGL thread.
     */
    protected void deinitInThread(){
        listener.destroy();
	if (renderer != null) {
		renderer.cleanup();
		// do android specific cleaning here

		logger.info("Display destroyed.");
		created.set(false);
		renderer = null;
		timer = null;
	}
    }


	protected void  applySettingsToRenderer(OGLESShaderRenderer renderer, AppSettings settings) {
		logger.warning("setSettings.USE_VA: [" + settings.getBoolean("USE_VA") + "]");
		logger.warning("setSettings.VERBOSE_LOGGING: [" + settings.getBoolean("VERBOSE_LOGGING") + "]");
		renderer.setUseVA(settings.getBoolean("USE_VA"));
		renderer.setVerboseLogging(settings.getBoolean("VERBOSE_LOGGING"));
	}

	@Override
    public void setSettings(AppSettings settings) {
		this.settings.copyFrom(settings);

		// XXX This code should be somewhere else
		if (renderer != null)
			applySettingsToRenderer(renderer, this.settings);
    }

    public void setSystemListener(SystemListener listener){
        this.listener = listener;
    }

    public AppSettings getSettings() {
        return settings;
    }

    public com.jme3.renderer.Renderer getRenderer() {
        return renderer;
    }

    public MouseInput getMouseInput() {
        return view;
    }

    public KeyInput getKeyInput() {
        return view;
    }

    public JoyInput getJoyInput() {
        return null;
    }

    public Timer getTimer() {
        return timer;
    }

    public void setTitle(String title) {
    }

    public boolean isCreated(){
        return created.get();
    }

    public void setAutoFlushFrames(boolean enabled){
        this.autoFlush = enabled;
    }

    // renderer:initialize
    public void onSurfaceCreated(GL10 gl, EGLConfig cfg) {
        logger.info("Using Android");
        initInThread(gl);
    }

    // SystemListener:reshape
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        settings.setResolution(width, height);
        listener.reshape(width, height);
    }

    // SystemListener:update
    public void onDrawFrame(GL10 gl) {
        if (needClose.get()){
            deinitInThread(); // ???
            return;
        }

//        if (wasActive != Display.isActive()){
//            if (!wasActive){
//                listener.gainFocus();
//                wasActive = true;
//            }else{
//                listener.loseFocus();
//                wasActive = false;
//            }
//        }

		if (!created.get())
            throw new IllegalStateException();

        listener.update();

        // swap buffers
        
        if (frameRate > 0){
//            Display.sync(frameRate);
            // synchronzie to framerate
        }

        if (autoFlush)
            renderer.onFrame();
    }

    /**
     * TODO: get these methods to follow the spec
     * @param waitFor
     */
    public void create(boolean waitFor) {
        if (created.get()){
            logger.warning("create() called when display is already created!");
            return;
        }
    }

    public void create(){
        create(false);
    }

    public void restart() {
    }

    public boolean isRenderable() {
       // TODO isRenderable
        return true;
    }
    
    /**
     * TODO: get these methods to follow the spec
     * @param waitFor
     */
    public void destroy(boolean waitFor) {
        needClose.set(true);
    }

    public void destroy(){
        destroy(false);
    }

}
