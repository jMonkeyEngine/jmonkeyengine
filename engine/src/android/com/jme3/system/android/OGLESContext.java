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
package com.jme3.system.android;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.FrameLayout;
import com.jme3.input.*;
import com.jme3.input.android.AndroidInput;
import com.jme3.input.android.AndroidSensorJoyInput;
import com.jme3.input.controls.SoftTextDialogInputListener;
import com.jme3.input.dummy.DummyKeyInput;
import com.jme3.input.dummy.DummyMouseInput;
import com.jme3.renderer.android.AndroidGLSurfaceView;
import com.jme3.renderer.RendererException;
import com.jme3.renderer.android.OGLESShaderRenderer;
import com.jme3.renderer.android.RendererUtil;
import com.jme3.system.*;
import com.jme3.system.android.AndroidConfigChooser.ConfigType;
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

    /*
     * >= OpenGL ES 2.0 (Android 2.2+)
     */
    protected OGLESShaderRenderer renderer;
    protected Timer timer;
    protected SystemListener listener;
    protected boolean autoFlush = true;
    protected AndroidInput androidInput;
    protected int minFrameDuration = 0;                   // No FPS cap
    protected JoyInput androidSensorJoyInput = null;
    /**
     * EGL_RENDERABLE_TYPE: EGL_OPENGL_ES_BIT = OpenGL ES 1.0 |
     * EGL_OPENGL_ES2_BIT = OpenGL ES 2.0
     */
    protected int clientOpenGLESVersion = 1;

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
     *
     * @param configType ConfigType.FASTEST (Default) | ConfigType.LEGACY |
     * ConfigType.BEST
     * @param eglConfigVerboseLogging if true show all found configs
     * @return GLSurfaceView The newly created view
     * @deprecated AndroidGLSurfaceView createView()
     * and put the configType in the appSettigs with the key AndroidConfigChoose.SETTINGS_CONFIG_TYPE
     */
    @Deprecated
    public AndroidGLSurfaceView createView(ConfigType configType, boolean eglConfigVerboseLogging) {
        settings.put(AndroidConfigChooser.SETTINGS_CONFIG_TYPE, configType);
        return this.createView();
    }
    /**
     * <code>createView</code> creates the GLSurfaceView that the renderer will
     * draw to. <p> The result GLSurfaceView will receive input events and
     * forward them to the Application. Any rendering will be done into the
     * GLSurfaceView. Only one GLSurfaceView can be created at this time. The
     * given configType specifies how to determine the display configuration.
     *
     *
     * @param eglConfigVerboseLogging if true show all found configs
     * @return GLSurfaceView The newly created view
     */
    public AndroidGLSurfaceView createView() {
        AndroidGLSurfaceView view;
        ConfigType configType = (ConfigType)settings.get(AndroidConfigChooser.SETTINGS_CONFIG_TYPE);

        // Start to set up the view
        view = new AndroidGLSurfaceView(JmeAndroidSystem.getActivity().getApplication());
        if (androidInput == null) {
            androidInput = new AndroidInput();
        }
        androidInput.setView(view);
        androidInput.loadSettings(settings);

        int rawOpenGLESVersion = getOpenGLESVersion();
        logger.log(Level.FINE, "clientOpenGLESVersion {0}.{1}",
                new Object[]{clientOpenGLESVersion>>16, clientOpenGLESVersion<<16});
        if (rawOpenGLESVersion < 0x20000) {
            throw new UnsupportedOperationException("OpenGL ES 2.0 is not supported on this device");
        } else {
            clientOpenGLESVersion = 2;
            view.setEGLContextClientVersion(clientOpenGLESVersion);
        }

        view.setFocusableInTouchMode(true);
        view.setFocusable(true);
        view.getHolder().setType(SurfaceHolder.SURFACE_TYPE_GPU);

        AndroidConfigChooser configChooser = new AndroidConfigChooser(settings, view);
        view.setEGLConfigChooser(configChooser);

        view.setRenderer(this);

        return view;
    }
    /**
     * Get the  OpenGL ES version
     * @return version returns the int value of the GLES version
     */
    public int getOpenGLESVersion() {
        ActivityManager am =
                (ActivityManager) JmeAndroidSystem.getActivity().getApplication().getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = am.getDeviceConfigurationInfo();
        logger.log(Level.FINE, "OpenGL Version {0}:", info.getGlEsVersion());
        return info.reqGlEsVersion;
//        return (info.reqGlEsVersion >= 0x20000);
    }

    // renderer:initialize
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig cfg) {
        if (created.get() && renderer != null) {
            renderer.resetGLObjects();
        } else {
            if (!created.get()) {
                logger.fine("GL Surface created, doing JME3 init");
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

        timer = new AndroidTimer();
        renderer = new OGLESShaderRenderer();

        renderer.initialize();
        listener.initialize();

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
        if (androidSensorJoyInput == null) {
            androidSensorJoyInput = new AndroidSensorJoyInput();
        }
        return androidSensorJoyInput;
    }

    @Override
    public TouchInput getTouchInput() {
        return androidInput;
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
            if (autoFlush) {
                renderer.onFrame();
            }

            long milliDelta = System.currentTimeMillis() - milliStart;

            // Enforce a FPS cap
            if (milliDelta < minFrameDuration) {
                //logger.log(Level.FINE, "Time per frame {0}", milliDelta);
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

    public void requestDialog(final int id, final String title, final String initialValue, final SoftTextDialogInputListener listener) {
        logger.log(Level.FINE, "requestDialog: title: {0}, initialValue: {1}",
                new Object[]{title, initialValue});

        final Activity activity = JmeAndroidSystem.getActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                final FrameLayout layoutTextDialogInput = new FrameLayout(activity);
                final EditText editTextDialogInput = new EditText(activity);
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

                AlertDialog dialogTextInput = new AlertDialog.Builder(activity).setTitle(title).setView(layoutTextDialogInput).setPositiveButton("OK",
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
