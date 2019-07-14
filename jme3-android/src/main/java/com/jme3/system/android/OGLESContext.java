/*
 * Copyright (c) 2009-2018 jMonkeyEngine
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

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ConfigurationInfo;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.FrameLayout;
import com.jme3.input.*;
import com.jme3.input.android.AndroidInputHandler;
import com.jme3.input.android.AndroidInputHandler14;
import com.jme3.input.controls.SoftTextDialogInputListener;
import com.jme3.input.dummy.DummyKeyInput;
import com.jme3.input.dummy.DummyMouseInput;
import com.jme3.renderer.android.AndroidGL;
import com.jme3.renderer.opengl.GL;
import com.jme3.renderer.opengl.GLES_30;
import com.jme3.renderer.opengl.GLDebugES;
import com.jme3.renderer.opengl.GLExt;
import com.jme3.renderer.opengl.GLFbo;
import com.jme3.renderer.opengl.GLRenderer;
import com.jme3.renderer.opengl.GLTracer;
import com.jme3.system.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class OGLESContext implements JmeContext, GLSurfaceView.Renderer, SoftTextDialogInput {

    private static final Logger logger = Logger.getLogger(OGLESContext.class.getName());
    protected final AtomicBoolean created = new AtomicBoolean(false);
    protected final AtomicBoolean renderable = new AtomicBoolean(false);
    protected final AtomicBoolean needClose = new AtomicBoolean(false);
    protected AppSettings settings = new AppSettings(true);

    protected GLRenderer renderer;
    protected Timer timer;
    protected SystemListener listener;
    protected boolean autoFlush = true;
    protected AndroidInputHandler androidInput;
    protected long minFrameDuration = 0;                   // No FPS cap
    protected long lastUpdateTime = 0;

    public OGLESContext() {
    }

    @Override
    public Type getType() {
        return Type.Display;
    }

    /**
     * <code>createView</code> creates the GLSurfaceView that the renderer will
     * draw to. <p> The result GLSurfaceView will receive input events and
     * forward them to the Application. Any rendering will be done into the
     * GLSurfaceView. Only one GLSurfaceView can be created at this time. The
     * given configType specifies how to determine the display configuration.
     *
     * @return GLSurfaceView The newly created view
     */
    public GLSurfaceView createView(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = am.getDeviceConfigurationInfo();
        // NOTE: We assume all ICS devices have OpenGL ES 2.0.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            // below 4.0, check OpenGL ES 2.0 support.
            if (info.reqGlEsVersion < 0x20000) {
                throw new UnsupportedOperationException("OpenGL ES 2.0 or better is not supported on this device");
            }
        } else if (Build.VERSION.SDK_INT < 9){
            throw new UnsupportedOperationException("jME3 requires Android 2.3 or later");
        }

        // Start to set up the view
        GLSurfaceView view = new GLSurfaceView(context);
        logger.log(Level.INFO, "Android Build Version: {0}", Build.VERSION.SDK_INT);
        if (androidInput == null) {
            if (Build.VERSION.SDK_INT >= 14) {
                androidInput = new AndroidInputHandler14();
            } else if (Build.VERSION.SDK_INT >= 9){
                androidInput = new AndroidInputHandler();
            }
        }
        androidInput.setView(view);
        androidInput.loadSettings(settings);

        // setEGLContextClientVersion must be set before calling setRenderer
        // this means it cannot be set in AndroidConfigChooser (too late)
        // use proper openGL ES version
        view.setEGLContextClientVersion(info.reqGlEsVersion>>16);

        view.setFocusableInTouchMode(true);
        view.setFocusable(true);

        // setFormat must be set before AndroidConfigChooser is called by the surfaceview.
        // if setFormat is called after ConfigChooser is called, then execution
        // stops at the setFormat call without a crash.
        // We look at the user setting for alpha bits and set the surfaceview
        // PixelFormat to either Opaque, Transparent, or Translucent.
        // ConfigChooser will do its best to honor the alpha requested by the user
        // For best rendering performance, use Opaque (alpha bits = 0).
        int curAlphaBits = settings.getAlphaBits();
        logger.log(Level.FINE, "curAlphaBits: {0}", curAlphaBits);
        if (curAlphaBits >= 8) {
            logger.log(Level.FINE, "Pixel Format: TRANSLUCENT");
            view.getHolder().setFormat(PixelFormat.TRANSLUCENT);
            view.setZOrderOnTop(true);
        } else if (curAlphaBits >= 1) {
            logger.log(Level.FINE, "Pixel Format: TRANSPARENT");
            view.getHolder().setFormat(PixelFormat.TRANSPARENT);
        } else {
            logger.log(Level.FINE, "Pixel Format: OPAQUE");
            view.getHolder().setFormat(PixelFormat.OPAQUE);
        }

        AndroidConfigChooser configChooser = new AndroidConfigChooser(settings);
        view.setEGLConfigChooser(configChooser);
        view.setRenderer(this);

        // Attempt to preserve the EGL Context on app pause/resume.
        // Not destroying and recreating the EGL context
        // will help with resume time by reusing the existing context to avoid
        // reloading all the OpenGL objects.
        if (Build.VERSION.SDK_INT >= 11) {
            view.setPreserveEGLContextOnPause(true);
        }

        return view;
    }

    // renderer:initialize
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig cfg) {
        if (created.get() && renderer != null) {
            renderer.resetGLObjects();
        } else {
            if (!created.get()) {
                logger.fine("GL Surface created, initializing JME3 renderer");
                initInThread();
            } else {
                logger.warning("GL Surface already created");
            }
        }
    }

    protected void initInThread() {
        created.set(true);

        logger.fine("OGLESContext create");
        logger.log(Level.FINE, "Running on thread: {0}", Thread.currentThread().getName());

        // Setup unhandled Exception Handler
        Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread thread, Throwable thrown) {
                listener.handleError("Exception thrown in " + thread.toString(), thrown);
            }
        });

        timer = new NanoTimer();
        Object gl = new AndroidGL();
        if (settings.getBoolean("GraphicsDebug")) {
            gl = new GLDebugES((GL) gl, (GLExt) gl, (GLFbo) gl);
        }
        if (settings.getBoolean("GraphicsTrace")) {
            gl = GLTracer.createGlesTracer(gl, GL.class, GLES_30.class, GLFbo.class, GLExt.class);
        }
        renderer = new GLRenderer((GL)gl, (GLExt)gl, (GLFbo)gl);
        renderer.initialize();

        JmeSystem.setSoftTextDialogInput(this);

        needClose.set(false);
    }

    /**
     * De-initialize in the OpenGL thread.
     */
    protected void deinitInThread() {
        if (renderable.get()) {
            created.set(false);
            if (renderer != null) {
                renderer.cleanup();
            }

            listener.destroy();

            listener = null;
            renderer = null;
            timer = null;

            // do android specific cleaning here
            logger.fine("Display destroyed.");

            renderable.set(false);
        }
    }

    @Override
    public void setSettings(AppSettings settings) {
        this.settings.copyFrom(settings);
        if (androidInput != null) {
            androidInput.loadSettings(settings);
        }

        if (settings.getFrameRate() > 0) {
            minFrameDuration = (long)(1000d / (double)settings.getFrameRate()); // ms
            logger.log(Level.FINE, "Setting min tpf: {0}ms", minFrameDuration);
        } else {
            minFrameDuration = 0;
        }
    }

    @Override
    public void setSystemListener(SystemListener listener) {
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
        return androidInput.getJoyInput();
    }

    @Override
    public TouchInput getTouchInput() {
        return androidInput.getTouchInput();
    }

    @Override
    public Timer getTimer() {
        return timer;
    }

    @Override
    public void setTitle(String title) {
    }

    @Override
    public boolean isCreated() {
        return created.get();
    }

    @Override
    public void setAutoFlushFrames(boolean enabled) {
        this.autoFlush = enabled;
    }

    // SystemListener:reshape
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        logger.log(Level.FINE, "GL Surface changed, width: {0} height: {1}", new Object[]{width, height});
        // update the application settings with the new resolution
        settings.setResolution(width, height);
        // reload settings in androidInput so the correct touch event scaling can be
        // calculated in case the surface resolution is different than the view
        androidInput.loadSettings(settings);
        // if the application has already been initialized (ie renderable is set)
        // then call reshape so the app can adjust to the new resolution.
        if (renderable.get()) {
            logger.log(Level.FINE, "App already initialized, calling reshape");
            listener.reshape(width, height);
        }
    }

    // SystemListener:update
    @Override
    public void onDrawFrame(GL10 gl) {
        if (needClose.get()) {
            deinitInThread();
            return;
        }

        if (!renderable.get()) {
            if (created.get()) {
                logger.fine("GL Surface is setup, initializing application");
                listener.initialize();
                renderable.set(true);
            }
        } else {
            if (!created.get()) {
                throw new IllegalStateException("onDrawFrame without create");
            }

            listener.update();
            if (autoFlush) {
                renderer.postFrame();
            }

            long updateDelta = System.currentTimeMillis() - lastUpdateTime;

            // Enforce a FPS cap
            if (updateDelta < minFrameDuration) {
//                    logger.log(Level.INFO, "lastUpdateTime: {0}, updateDelta: {1}, minTimePerFrame: {2}",
//                            new Object[]{lastUpdateTime, updateDelta, minTimePerFrame});
                try {
                    Thread.sleep(minFrameDuration - updateDelta);
                } catch (InterruptedException e) {
                }
            }

            lastUpdateTime = System.currentTimeMillis();

        }
    }

    @Override
    public boolean isRenderable() {
        return renderable.get();
    }

    @Override
    public void create(boolean waitFor) {
        if (waitFor) {
            waitFor(true);
        }
    }

    public void create() {
        create(false);
    }

    @Override
    public void restart() {
    }

    @Override
    public void destroy(boolean waitFor) {
        needClose.set(true);
        if (waitFor) {
            waitFor(false);
        }
    }

    public void destroy() {
        destroy(true);
    }

    protected void waitFor(boolean createdVal) {
        while (renderable.get() != createdVal) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
            }
        }
    }

    public void requestDialog(final int id, final String title, final String initialValue, final SoftTextDialogInputListener listener) {
        logger.log(Level.FINE, "requestDialog: title: {0}, initialValue: {1}",
                new Object[]{title, initialValue});

        final View view = JmeAndroidSystem.getView();
        view.getHandler().post(new Runnable() {
            @Override
            public void run() {

                final FrameLayout layoutTextDialogInput = new FrameLayout(view.getContext());
                final EditText editTextDialogInput = new EditText(view.getContext());
                editTextDialogInput.setWidth(LayoutParams.FILL_PARENT);
                editTextDialogInput.setHeight(LayoutParams.FILL_PARENT);
                editTextDialogInput.setPadding(20, 20, 20, 20);
                editTextDialogInput.setGravity(Gravity.FILL_HORIZONTAL);
                //editTextDialogInput.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);

                editTextDialogInput.setText(initialValue);

                switch (id) {
                    case SoftTextDialogInput.TEXT_ENTRY_DIALOG:

                        editTextDialogInput.setInputType(InputType.TYPE_CLASS_TEXT);
                        break;

                    case SoftTextDialogInput.NUMERIC_ENTRY_DIALOG:

                        editTextDialogInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                        break;

                    case SoftTextDialogInput.NUMERIC_KEYPAD_DIALOG:

                        editTextDialogInput.setInputType(InputType.TYPE_CLASS_PHONE);
                        break;

                    default:
                        break;
                }

                layoutTextDialogInput.addView(editTextDialogInput);

                AlertDialog dialogTextInput = new AlertDialog.Builder(view.getContext()).setTitle(title).setView(layoutTextDialogInput).setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                /* User clicked OK, send COMPLETE action
                                 * and text */
                                listener.onSoftText(SoftTextDialogInputListener.COMPLETE, editTextDialogInput.getText().toString());
                            }
                        }).setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                /* User clicked CANCEL, send CANCEL action
                                 * and text */
                                listener.onSoftText(SoftTextDialogInputListener.CANCEL, editTextDialogInput.getText().toString());
                            }
                        }).create();

                dialogTextInput.show();
            }
        });
    }

    @Override
    public com.jme3.opencl.Context getOpenCLContext() {
        logger.warning("OpenCL is not yet supported on android");
        return null;
    }
}
