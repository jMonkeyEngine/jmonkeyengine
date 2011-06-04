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
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.SurfaceHolder;

import com.jme3.app.AndroidHarness;
import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.TouchInput;
import com.jme3.input.android.AndroidInput;
import com.jme3.input.dummy.DummyKeyInput;
import com.jme3.input.dummy.DummyMouseInput;
import com.jme3.renderer.android.OGLESShaderRenderer;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.SystemListener;
import com.jme3.system.Timer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class OGLESContext implements JmeContext, GLSurfaceView.Renderer 
{

    private static final Logger logger = Logger.getLogger(OGLESContext.class.getName());

    protected final AtomicBoolean created = new AtomicBoolean(false);
    protected final AtomicBoolean renderable = new AtomicBoolean(false);
    protected final AtomicBoolean needClose = new AtomicBoolean(false);
    protected final Object createdLock = new Object();
    protected final AppSettings settings = new AppSettings(true);

	/* >= OpenGL ES 2.0 (Android 2.2+) */
	protected OGLESShaderRenderer renderer;

    protected Timer timer;
    protected SystemListener listener;

    
    protected boolean wasActive = false;
    protected boolean autoFlush = true;

    protected AndroidInput view;
    
    private long milliStart;
    private long milliDelta;
    protected int frameRate = 33;
    //protected int minFrameDuration = 1000 / frameRate;  // Set a max FPS of 33
    protected int minFrameDuration = 0;                   // No FPS cap

    public OGLESContext() { }

    @Override
    public Type getType() 
    {
        return Type.Display;
    }
    
    /**
     * <code>createView</code> 
     * @param activity The Android activity which is parent for the GLSurfaceView  
     * @return GLSurfaceView The newly created view
     */
    public GLSurfaceView createView(Activity activity)
    {
        return createView(new AndroidInput(activity));        
    }
    /**
     * <code>createView</code> 
     * @param AndroidInput The Android input which must be bound to an activity  
     * @return GLSurfaceView The newly created view
     */
    public GLSurfaceView createView(AndroidInput view)
    {
        return createView(view, 0);
    }
    
    /**
     * <code>createView</code> 
     * @param AndroidInput The Android input which must be bound to an activity  
     * @param debugflags 0, GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS
     * @return GLSurfaceView The newly created view
     */    
    public GLSurfaceView createView(AndroidInput view, int debugflags)
    {
        this.view = view;    

    	/*
    	 * Requesting client version from GLSurfaceView which is extended by
    	 * AndroidInput.
    	 * This is required to get OpenGL ES 2.0
    	 */    	
    	view.setEGLContextClientVersion(2);
    	
        //RGB565, Depth16
        view.setEGLConfigChooser(5, 6, 5, 0, 16, 0);
        view.setFocusableInTouchMode(true);
        view.setFocusable(true);
        view.getHolder().setType(SurfaceHolder.SURFACE_TYPE_GPU);
        view.setDebugFlags(debugflags);
   		view.setRenderer(this);
        return view;
    }
    

    protected void initInThread()
    {
        logger.info("OGLESContext create");
        logger.info("Running on thread: "+Thread.currentThread().getName());

        final Context ctx = this.view.getContext();
        
        // Setup unhandled Exception Handler
        if (ctx instanceof AndroidHarness)
        {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                public void uncaughtException(Thread thread, Throwable thrown) {
                    ((AndroidHarness)ctx).handleError("Uncaught exception thrown in "+thread.toString(), thrown);
                }
            });
        }
        else
        {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                public void uncaughtException(Thread thread, Throwable thrown) {
                    listener.handleError("Uncaught exception thrown in "+thread.toString(), thrown);
                }
            });
        }
        
        timer = new AndroidTimer();

        renderer = new OGLESShaderRenderer();
    
        renderer.setUseVA(true);
        renderer.setVerboseLogging(false);
        
        renderer.initialize();
        listener.initialize();                
        created.set(true);
        
        needClose.set(false);
    }

    /**
     * De-initialize in the OpenGL thread.
     */
    protected void deinitInThread()
    {        
        if (renderer != null) 
            renderer.cleanup();
            
        listener.destroy();
        
        // do android specific cleaning here
		logger.info("Display destroyed.");		
		renderable.set(false);
		created.set(false);
		renderer = null;
		timer = null;
    }
    
    protected void  applySettingsToRenderer(OGLESShaderRenderer renderer, AppSettings settings) 
    {
        logger.warning("setSettings.USE_VA: [" + settings.getBoolean("USE_VA") + "]");
        logger.warning("setSettings.VERBOSE_LOGGING: [" + settings.getBoolean("VERBOSE_LOGGING") + "]");
        renderer.setUseVA(settings.getBoolean("USE_VA"));
        renderer.setVerboseLogging(settings.getBoolean("VERBOSE_LOGGING"));
    }
    
    protected void applySettings(AppSettings setting)
    {
        if (renderer != null)
            applySettingsToRenderer(renderer, settings);        
    }

    @Override
    public void setSettings(AppSettings settings) 
    {
        this.settings.copyFrom(settings);
    }

    @Override
    public void setSystemListener(SystemListener listener){
        this.listener = listener;
    }

    @Override
    public AppSettings getSettings() {
        return settings;
    }

    @Override
    public com.jme3.renderer.Renderer getRenderer() {
        return renderer;
    }

    @Override
    public MouseInput getMouseInput() {
        return new DummyMouseInput();
    }

    @Override
    public KeyInput getKeyInput() {
        return new DummyKeyInput();
    }
    
    @Override
    public JoyInput getJoyInput() {
        return null;
    }

    @Override
    public TouchInput getTouchInput() {
        return view;
    }
    
    @Override
    public Timer getTimer() 
    {
        return timer;
    }

    @Override
    public void setTitle(String title) 
    {
    }
    
    @Override
    public boolean isCreated()
    {
        return created.get();
    }
    @Override
    public void setAutoFlushFrames(boolean enabled)
    {
        this.autoFlush = enabled;
    }

    // renderer:initialize
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig cfg) 
    {
        if (created.get() && renderer != null)
        {
            renderer.resetGLObjects();
        }
        else
        {
            logger.info("GL Surface created");
            initInThread();
            renderable.set(true);
        }
    }

    // SystemListener:reshape
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) 
    {
        settings.setResolution(width, height);
        listener.reshape(width, height);
    }

    // SystemListener:update
    @Override
    public void onDrawFrame(GL10 gl) 
    {
        
        
        if (needClose.get())
        {
            deinitInThread();
            return;
        }
        
        if (renderable.get())
        {

            if (!created.get())
                throw new IllegalStateException("onDrawFrame without create");

            milliStart = System.currentTimeMillis();
                    

    
            listener.update();
            
            
            if (autoFlush)
            {
                renderer.onFrame();
            }
            
            milliDelta = System.currentTimeMillis() - milliStart;
            
            // Enforce a FPS cap
            if (milliDelta < minFrameDuration) 
            {
                //logger.log(Level.INFO, "Time per frame {0}", milliDelta);
                try {
                    Thread.sleep(minFrameDuration - milliDelta);
                } catch (InterruptedException e) {
                }
            }
            
        }
        
        
    }
    
    @Override
    public boolean isRenderable()
    {
        return renderable.get();
    }
    
    @Override
    public void create(boolean waitFor)
    {
        if (waitFor)
            waitFor(true);
    }
    
    public void create()
    {
        create(false);
    }
    
    @Override
    public void restart() 
    {
        
    }
    
    @Override
    public void destroy(boolean waitFor) 
    {
        needClose.set(true);
        if (waitFor)
            waitFor(false);
    }
           
    public void destroy()
    {
        destroy(true);
    }
    
    protected void waitFor(boolean createdVal)
    {
        synchronized (createdLock){
            while (created.get() != createdVal){
                try {
                    createdLock.wait();
                } catch (InterruptedException ex) {
                }
            }
        }
    }


}
