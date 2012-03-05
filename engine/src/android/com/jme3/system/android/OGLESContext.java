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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.opengl.GLSurfaceView;
import android.text.InputType;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.FrameLayout;
import com.jme3.app.AndroidHarness;
import com.jme3.app.Application;
import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.SoftTextDialogInput;
import com.jme3.input.TouchInput;
import com.jme3.input.android.AndroidInput;
import com.jme3.input.controls.SoftTextDialogInputListener;
import com.jme3.input.controls.TouchTrigger;
import com.jme3.input.dummy.DummyKeyInput;
import com.jme3.input.dummy.DummyMouseInput;
import com.jme3.renderer.android.OGLESShaderRenderer;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeSystem;
import com.jme3.system.SystemListener;
import com.jme3.system.Timer;
import com.jme3.system.android.AndroidConfigChooser.ConfigType;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

public class OGLESContext implements JmeContext, GLSurfaceView.Renderer, SoftTextDialogInput {

    private static final Logger logger = Logger.getLogger(OGLESContext.class.getName());
    protected final AtomicBoolean created = new AtomicBoolean(false);
    protected final AtomicBoolean renderable = new AtomicBoolean(false);
    protected final AtomicBoolean needClose = new AtomicBoolean(false);
    protected final AppSettings settings = new AppSettings(true);

    /*
     * >= OpenGL ES 2.0 (Android 2.2+)
     */
    protected OGLESShaderRenderer renderer;
    protected Timer timer;
    protected SystemListener listener;
    protected boolean autoFlush = true;
    protected AndroidInput view;
    private boolean firstDrawFrame = true;
    //protected int minFrameDuration = 1000 / frameRate;  // Set a max FPS of 33
    protected int minFrameDuration = 0;                   // No FPS cap
    /**
     * EGL_RENDERABLE_TYPE: EGL_OPENGL_ES_BIT = OpenGL ES 1.0 |
     * EGL_OPENGL_ES2_BIT = OpenGL ES 2.0
     */
    protected int clientOpenGLESVersion = 1;
    protected boolean verboseLogging = false;
    final private String ESCAPE_EVENT = "TouchEscape";

    public OGLESContext() {
    }

    @Override
    public Type getType() {
        return Type.Display;
    }

    /**
     * <code>createView</code>
     *
     * @param activity The Android activity which is parent for the
     * GLSurfaceView
     * @return GLSurfaceView The newly created view
     */
    public GLSurfaceView createView(Activity activity) {
        return createView(new AndroidInput(activity));
    }

    /**
     * <code>createView</code>
     *
     * @param view The Android input which will be used as the GLSurfaceView for
     * this context
     * @return GLSurfaceView The newly created view
     */
    public GLSurfaceView createView(AndroidInput view) {
        return createView(view, ConfigType.FASTEST, false);
    }

    /**
     * <code>createView</code> initializes the GLSurfaceView
     *
     * @param view The Android input which will be used as the GLSurfaceView for
     * this context
     * @param configType ConfigType.FASTEST (Default) | ConfigType.LEGACY |
     * ConfigType.BEST
     * @param eglConfigVerboseLogging if true show all found configs
     * @return GLSurfaceView The newly created view
     */
    public GLSurfaceView createView(AndroidInput view, ConfigType configType, boolean eglConfigVerboseLogging) {
        // Start to set up the view
        this.view = view;
        verboseLogging = eglConfigVerboseLogging;

        if (configType == ConfigType.LEGACY) {
            // Hardcoded egl setup
            clientOpenGLESVersion = 2;
            view.setEGLContextClientVersion(2);
            //RGB565, Depth16
            view.setEGLConfigChooser(5, 6, 5, 0, 16, 0);
            logger.info("ConfigType.LEGACY using RGB565");
        } else {
            EGL10 egl = (EGL10) EGLContext.getEGL();
            EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

            int[] version = new int[2];
            if (egl.eglInitialize(display, version) == true) {
                logger.log(Level.INFO, "Display EGL Version: {0}.{1}", new Object[]{version[0], version[1]});
            }

            try {
                // Create a config chooser
                AndroidConfigChooser configChooser = new AndroidConfigChooser(configType);
                // Init chooser
                if (!configChooser.findConfig(egl, display)) {
                    listener.handleError("Unable to find suitable EGL config", null);
                    return null;
                }

                clientOpenGLESVersion = configChooser.getClientOpenGLESVersion();
                if (clientOpenGLESVersion < 2) {
                    listener.handleError("OpenGL ES 2.0 is not supported on this device", null);
                    return null;
                }

                // Requesting client version from GLSurfaceView which is extended by
                // AndroidInput.
                view.setEGLContextClientVersion(clientOpenGLESVersion);
                view.setEGLConfigChooser(configChooser);
                view.getHolder().setFormat(configChooser.getPixelFormat());
            } finally {
                if (display != null) {
                    egl.eglTerminate(display);
                }
            }
        }

        view.setFocusableInTouchMode(true);
        view.setFocusable(true);
        view.getHolder().setType(SurfaceHolder.SURFACE_TYPE_GPU);
        view.setRenderer(this);

        return view;
    }

    // renderer:initialize
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig cfg) {

        if (created.get() && renderer != null) {
            renderer.resetGLObjects();
        } else {
            if (!created.get()) {
                logger.info("GL Surface created, doing JME3 init");
                initInThread();
            } else {
                logger.warning("GL Surface already created");
            }
        }
    }

    protected void initInThread() {
        created.set(true);

        logger.info("OGLESContext create");
        logger.info("Running on thread: " + Thread.currentThread().getName());

        final Context ctx = this.view.getContext();

        // Setup unhandled Exception Handler
        if (ctx instanceof AndroidHarness) {
            Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

                public void uncaughtException(Thread thread, Throwable thrown) {
                    ((AndroidHarness) ctx).handleError("Exception thrown in " + thread.toString(), thrown);
                }
            });
        } else {
            Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

                public void uncaughtException(Thread thread, Throwable thrown) {
                    listener.handleError("Exception thrown in " + thread.toString(), thrown);
                }
            });
        }

        if (clientOpenGLESVersion < 2) {
            throw new UnsupportedOperationException("OpenGL ES 2.0 is not supported on this device");
        }

        timer = new AndroidTimer();
        renderer = new OGLESShaderRenderer();

        renderer.setUseVA(true);
        renderer.setVerboseLogging(verboseLogging);

        renderer.initialize();
        listener.initialize();

        // Setup exit hook
        if (ctx instanceof AndroidHarness) {
            Application app = ((AndroidHarness) ctx).getJmeApplication();
            if (app.getInputManager() != null) {
                app.getInputManager().addMapping(ESCAPE_EVENT, new TouchTrigger(TouchInput.KEYCODE_BACK));
                app.getInputManager().addListener((AndroidHarness) ctx, new String[]{ESCAPE_EVENT});
            }
        }

        JmeSystem.setSoftTextDialogInput(this);

        needClose.set(false);
        renderable.set(true);
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
            logger.info("Display destroyed.");

            renderable.set(false);
            final Context ctx = this.view.getContext();
            if (ctx instanceof AndroidHarness) {
                AndroidHarness harness = (AndroidHarness) ctx;
                if (harness.isFinishOnAppStop()) {
                    harness.finish();
                }
            }
        }
    }

    protected void applySettingsToRenderer(OGLESShaderRenderer renderer, AppSettings settings) {
        logger.warning("setSettings.USE_VA: [" + settings.getBoolean("USE_VA") + "]");
        logger.warning("setSettings.VERBOSE_LOGGING: [" + settings.getBoolean("VERBOSE_LOGGING") + "]");
        renderer.setUseVA(settings.getBoolean("USE_VA"));
        renderer.setVerboseLogging(settings.getBoolean("VERBOSE_LOGGING"));
    }

    protected void applySettings(AppSettings settings) {
        setSettings(settings);
        if (renderer != null) {
            applySettingsToRenderer(renderer, this.settings);
        }
    }

    @Override
    public void setSettings(AppSettings settings) {
        this.settings.copyFrom(settings);
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
        return null;
    }

    @Override
    public TouchInput getTouchInput() {
        return view;
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
        logger.info("GL Surface changed, width: " + width + " height: " + height);
        settings.setResolution(width, height);
        listener.reshape(width, height);
    }

    // SystemListener:update
    @Override
    public void onDrawFrame(GL10 gl) {
        if (needClose.get()) {
            deinitInThread();
            return;
        }

        if (renderable.get()) {
            if (!created.get()) {
                throw new IllegalStateException("onDrawFrame without create");
            }

            long milliStart = System.currentTimeMillis();

            listener.update();

            // call to AndroidHarness to remove the splash screen, if present.
            // call after listener.update() to make sure no gap between
            //   splash screen going away and app display being shown.
            if (firstDrawFrame) {
                final Context ctx = this.view.getContext();
                if (ctx instanceof AndroidHarness) {
                    ((AndroidHarness) ctx).removeSplashScreen();
                }
                firstDrawFrame = false;
            }

            if (autoFlush) {
                renderer.onFrame();
            }

            long milliDelta = System.currentTimeMillis() - milliStart;

            // Enforce a FPS cap
            if (milliDelta < minFrameDuration) {
                //logger.log(Level.INFO, "Time per frame {0}", milliDelta);
                try {
                    Thread.sleep(minFrameDuration - milliDelta);
                } catch (InterruptedException e) {
                }
            }

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

    public int getClientOpenGLESVersion() {
        return clientOpenGLESVersion;
    }

    public void requestDialog(final int id, final String title, final String initialValue, final SoftTextDialogInputListener listener) {
        logger.log(Level.INFO, "requestDialog: title: {0}, initialValue: {1}",
                new Object[]{title, initialValue});

        JmeAndroidSystem.getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {

                final FrameLayout layoutTextDialogInput = new FrameLayout(JmeAndroidSystem.getActivity());
                final EditText editTextDialogInput = new EditText(JmeAndroidSystem.getActivity());
                editTextDialogInput.setWidth(LayoutParams.FILL_PARENT);
                editTextDialogInput.setHeight(LayoutParams.FILL_PARENT);
                editTextDialogInput.setPadding(20, 20, 20, 20);
                editTextDialogInput.setGravity(Gravity.FILL_HORIZONTAL);

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

                AlertDialog dialogTextInput = new AlertDialog.Builder(JmeAndroidSystem.getActivity()).setTitle(title).setView(layoutTextDialogInput).setPositiveButton("OK",
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
}
