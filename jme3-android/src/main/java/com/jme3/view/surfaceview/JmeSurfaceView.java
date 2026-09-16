/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
package com.jme3.view.surfaceview;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import com.jme3.app.LegacyApplication;
import com.jme3.audio.AudioRenderer;
import com.jme3.input.JoyInput;
import com.jme3.input.android.AndroidJoyInput;
import com.jme3.input.android.AndroidSensorJoyInput;
import com.jme3.system.AppSettings;
import com.jme3.system.SystemListener;
import com.jme3.system.android.JmeAndroidSystem;
import com.jme3.system.android.OGLESContext;
import com.jme3.util.AndroidNativeBufferAllocator;
import com.jme3.util.BufferAllocatorFactory;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An OpenGL Android View wrapper for embedding a jMonkeyEngine application in custom layouts.
 *
 * @author pavl_g.
 */
public class JmeSurfaceView extends RelativeLayout
        implements SystemListener, DialogInterface.OnClickListener, LifecycleEventObserver {

    private static final Logger logger = Logger.getLogger(JmeSurfaceView.class.getName());
    private static final String SAFER_BUFFER_ALLOCATOR_CLASS = "com.jme3.util.SaferBufferAllocator";

    public enum DestructionPolicy {
        DESTROY_WHEN_FINISH,
        KEEP_WHEN_FINISH
    }

    protected static final class GameState {
        private static LegacyApplication legacyApplication;
        private static boolean firstUpdatePassed = false;

        private GameState() {
        }

        protected static LegacyApplication getLegacyApplication() {
            return legacyApplication;
        }

        protected static void setLegacyApplication(LegacyApplication legacyApplication) {
            GameState.legacyApplication = legacyApplication;
        }

        protected static boolean isFirstUpdatePassed() {
            return firstUpdatePassed;
        }

        protected static void setFirstUpdatePassed(boolean firstUpdatePassed) {
            GameState.firstUpdatePassed = firstUpdatePassed;
        }
    }

    protected String audioRendererType = AppSettings.ANDROID_OPENAL_SOFT;
    private LegacyApplication legacyApplication;
    private AppSettings appSettings;
    private int eglBitsPerPixel = 24;
    private int eglAlphaBits = 0;
    private int eglDepthBits = 16;
    private int eglSamples = 0;
    private int eglStencilBits = 0;
    private int frameRate = -1;
    private boolean emulateKeyBoard = true;
    private boolean emulateMouse = true;
    private boolean useJoyStickEvents = true;
    private boolean isGLThreadPaused = true;
    private boolean showErrorDialog = true;
    private boolean bindAppState = true;
    private boolean showEscExitPrompt = true;
    private boolean exitOnEscPressed = true;
    private String crashLog = "";
    private String glEsVersion = "";

    private GLSurfaceView glSurfaceView;
    private OGLESContext oglesContext;
    private DestructionPolicy destructionPolicy = DestructionPolicy.DESTROY_WHEN_FINISH;
    private Handler handler = new Handler(Looper.getMainLooper());

    private OnRendererStarted onRendererStarted;
    private OnRendererCompleted onRendererCompleted;
    private OnLayoutDrawn onLayoutDrawn;
    private OnExceptionThrown onExceptionThrown;

    public JmeSurfaceView(@NonNull Context context) {
        super(context);
    }

    public JmeSurfaceView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public JmeSurfaceView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public JmeSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void startRenderer(int delayMillis) {
        delayMillis = Math.max(0, delayMillis);

        try {
            ConfigurationInfo configurationInfo = ((ActivityManager) getContext()
                    .getSystemService(Context.ACTIVITY_SERVICE)).getDeviceConfigurationInfo();
            glEsVersion = "GL_ES Version : " + configurationInfo.getGlEsVersion();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to retrieve GL ES version", e);
        }

        if (legacyApplication == null) {
            throw new IllegalStateException("Cannot build a SurfaceView for a null app, make sure to use setLegacyApplication() to pass in your app!");
        }

        if (System.getProperty(BufferAllocatorFactory.PROPERTY_BUFFER_ALLOCATOR_IMPLEMENTATION) == null) {
            String allocator = isClassPresent(SAFER_BUFFER_ALLOCATOR_CLASS)
                    ? SAFER_BUFFER_ALLOCATOR_CLASS
                    : AndroidNativeBufferAllocator.class.getName();
            System.setProperty(BufferAllocatorFactory.PROPERTY_BUFFER_ALLOCATOR_IMPLEMENTATION, allocator);
        }

        if (appSettings == null) {
            appSettings = new AppSettings(true);
        }
        appSettings.setAudioRenderer(audioRendererType);
        if (getLayoutParams() != null && getLayoutParams().width > 0 && getLayoutParams().height > 0) {
            appSettings.setResolution(getLayoutParams().width, getLayoutParams().height);
        }
        appSettings.setAlphaBits(eglAlphaBits);
        appSettings.setDepthBits(eglDepthBits);
        appSettings.setSamples(eglSamples);
        appSettings.setStencilBits(eglStencilBits);
        appSettings.setBitsPerPixel(eglBitsPerPixel);
        appSettings.setFrameRate(frameRate);
        appSettings.setEmulateKeyboard(emulateKeyBoard);
        appSettings.setEmulateMouse(emulateMouse);
        appSettings.setUseJoysticks(useJoyStickEvents);

        if (GameState.getLegacyApplication() != null) {
            this.legacyApplication = GameState.getLegacyApplication();
            logger.log(Level.INFO, "Reusing existing GameState application");
        } else {
            legacyApplication.setSettings(appSettings);
            logger.log(Level.INFO, "Starting a new GameState application");
            legacyApplication.start();

            if (onRendererStarted != null && getContext() instanceof Activity) {
                ((Activity) getContext()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onRendererStarted.onRenderStart(legacyApplication, JmeSurfaceView.this);
                    }
                });
            }
        }

        oglesContext = (OGLESContext) legacyApplication.getContext();
        oglesContext.setSystemListener(this);
        glSurfaceView = oglesContext.createView(getContext());
        JmeAndroidSystem.setView(this);

        glSurfaceView.setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        if (GameState.getLegacyApplication() != null || delayMillis == 0) {
            addGlSurfaceView();
        } else {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    addGlSurfaceView();
                }
            }, delayMillis);
        }
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, JmeSurfaceView.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    protected void addGlSurfaceView() {
        if (glSurfaceView == null) {
            return;
        }
        post(new Runnable() {
            @Override
            public void run() {
                if (glSurfaceView.getParent() instanceof ViewGroup) {
                    ((ViewGroup) glSurfaceView.getParent()).removeView(glSurfaceView);
                }
                addView(glSurfaceView);
                if (onLayoutDrawn != null && getContext() instanceof Activity) {
                    ((Activity) getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onLayoutDrawn.onLayoutDrawn(legacyApplication, JmeSurfaceView.this);
                        }
                    });
                }
            }
        });
    }

    private void removeGLSurfaceView() {
        if (glSurfaceView != null) {
            post(new Runnable() {
                @Override
                public void run() {
                    removeView(glSurfaceView);
                }
            });
        }
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        if (!bindAppState) {
            return;
        }
        if (event == Lifecycle.Event.ON_RESUME) {
            gainFocus();
        } else if (event == Lifecycle.Event.ON_PAUSE) {
            loseFocus();
        } else if (event == Lifecycle.Event.ON_DESTROY) {
            if (destructionPolicy == DestructionPolicy.DESTROY_WHEN_FINISH) {
                destroy();
            }
        }
    }

    @Override
    public void initialize() {
        if (legacyApplication != null) {
            legacyApplication.initialize();
        }
    }

    @Override
    public void reshape(int width, int height) {
        if (legacyApplication != null) {
            legacyApplication.reshape(width, height);
        }
    }

    @Override
    public void reshape(int logicalWidth, int logicalHeight, int framebufferWidth, int framebufferHeight) {
        if (legacyApplication != null) {
            legacyApplication.reshape(logicalWidth, logicalHeight, framebufferWidth, framebufferHeight);
        }
    }

    @Override
    public void rescale(float x, float y) {
        if (legacyApplication != null) {
            legacyApplication.rescale(x, y);
        }
    }

    @Override
    public void update() {
        if (legacyApplication != null) {
            legacyApplication.update();
        }
        if (!GameState.isFirstUpdatePassed()) {
            GameState.setFirstUpdatePassed(true);
            if (onRendererCompleted != null && getContext() instanceof Activity) {
                ((Activity) getContext()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onRendererCompleted.onRenderCompletion(legacyApplication, appSettings);
                    }
                });
            }
        }
    }

    @Override
    public void requestClose(boolean esc) {
        if (legacyApplication != null) {
            legacyApplication.requestClose(esc);
        }
        if (esc && showEscExitPrompt && getContext() instanceof Activity) {
            ((Activity) getContext()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Do you want to exit?")
                            .setMessage("Use your home key to bring this app into the background or exit to terminate it.")
                            .setPositiveButton("Yes", JmeSurfaceView.this)
                            .setNegativeButton("No", JmeSurfaceView.this)
                            .create()
                            .show();
                }
            });
        }
    }

    @Override
    public void gainFocus() {
        logger.fine("gainFocus");
        if (glSurfaceView != null) {
            glSurfaceView.onResume();
        }

        if (legacyApplication != null) {
            AudioRenderer audioRenderer = legacyApplication.getAudioRenderer();
            if (audioRenderer != null) {
                audioRenderer.resumeAll();
            }

            JoyInput joyInput = legacyApplication.getContext() != null
                    ? legacyApplication.getContext().getJoyInput() : null;
            if (joyInput instanceof AndroidJoyInput) {
                ((AndroidJoyInput) joyInput).resumeJoysticks();
            } else if (joyInput instanceof AndroidSensorJoyInput) {
                ((AndroidSensorJoyInput) joyInput).resumeSensors();
            }

            legacyApplication.gainFocus();
        }
        isGLThreadPaused = false;
    }

    @Override
    public void loseFocus() {
        logger.fine("loseFocus");
        if (legacyApplication != null) {
            legacyApplication.loseFocus();
        }

        if (glSurfaceView != null) {
            glSurfaceView.onPause();
        }

        if (legacyApplication != null) {
            AudioRenderer audioRenderer = legacyApplication.getAudioRenderer();
            if (audioRenderer != null) {
                audioRenderer.pauseAll();
            }

            JoyInput joyInput = legacyApplication.getContext() != null
                    ? legacyApplication.getContext().getJoyInput() : null;
            if (joyInput instanceof AndroidJoyInput) {
                ((AndroidJoyInput) joyInput).pauseJoysticks();
            } else if (joyInput instanceof AndroidSensorJoyInput) {
                ((AndroidSensorJoyInput) joyInput).pauseSensors();
            }
        }
        isGLThreadPaused = true;
    }

    @Override
    public void destroy() {
        logger.fine("destroy");
        if (legacyApplication != null) {
            legacyApplication.stop(false);
        }

        removeGLSurfaceView();

        if (destructionPolicy == DestructionPolicy.KEEP_WHEN_FINISH) {
            GameState.setLegacyApplication(legacyApplication);
        } else {
            GameState.setLegacyApplication(null);
            GameState.setFirstUpdatePassed(false);
        }

        legacyApplication = null;
        glSurfaceView = null;
        oglesContext = null;
        JmeAndroidSystem.setView(null);
    }

    @Override
    public void handleError(final String errorMsg, final Throwable throwable) {
        String stackTrace = "";
        String title = "Error";

        if (throwable != null) {
            StringWriter writer = new StringWriter(100);
            throwable.printStackTrace(new PrintWriter(writer));
            stackTrace = writer.toString();
            title = throwable.toString();
        }

        final String finalTitle = title;
        final String finalMessage = (errorMsg != null ? errorMsg : "Uncaught Exception")
                + "\n" + stackTrace;
        this.crashLog = finalMessage;

        logger.log(Level.SEVERE, finalMessage);

        if (onExceptionThrown != null) {
            onExceptionThrown.onExceptionThrown(throwable);
        }

        if (showErrorDialog && getContext() instanceof Activity) {
            ((Activity) getContext()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new AlertDialog.Builder(getContext())
                            .setTitle(finalTitle)
                            .setMessage(finalMessage)
                            .setCancelable(true)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    destroy();
                                    if (getContext() instanceof Activity) {
                                        ((Activity) getContext()).finish();
                                    }
                                }
                            })
                            .setNegativeButton("Kill", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    android.os.Process.killProcess(android.os.Process.myPid());
                                }
                            })
                            .create()
                            .show();
                }
            });
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (exitOnEscPressed) {
                destroy();
                if (getContext() instanceof Activity) {
                    ((Activity) getContext()).finish();
                }
            }
        }
    }

    public void bindAppStateToActivityLifeCycle(final boolean condition) {
        this.bindAppState = condition;
    }

    public DestructionPolicy getDestructionPolicy() {
        return destructionPolicy;
    }

    public void setDestructionPolicy(DestructionPolicy destructionPolicy) {
        this.destructionPolicy = destructionPolicy;
    }

    public boolean isAppStateBoundToActivityLifeCycle() {
        return bindAppState;
    }

    public boolean isShowEscExitPrompt() {
        return showEscExitPrompt;
    }

    public void setShowEscExitPrompt(boolean showEscExitPrompt) {
        this.showEscExitPrompt = showEscExitPrompt;
    }

    public boolean isExitOnEscPressed() {
        return exitOnEscPressed;
    }

    public void setExitOnEscPressed(boolean exitOnEscPressed) {
        this.exitOnEscPressed = exitOnEscPressed;
    }

    public LegacyApplication getLegacyApplication() {
        return legacyApplication;
    }

    public void setLegacyApplication(@NonNull LegacyApplication legacyApplication) {
        this.legacyApplication = legacyApplication;
    }

    public AppSettings getAppSettings() {
        return appSettings;
    }

    public void setAppSettings(@NonNull AppSettings appSettings) {
        this.appSettings = appSettings;
    }

    public int getEglBitsPerPixel() {
        return eglBitsPerPixel;
    }

    public void setEglBitsPerPixel(int eglBitsPerPixel) {
        this.eglBitsPerPixel = eglBitsPerPixel;
    }

    public int getEglAlphaBits() {
        return eglAlphaBits;
    }

    public void setEglAlphaBits(int eglAlphaBits) {
        this.eglAlphaBits = eglAlphaBits;
    }

    public int getEglDepthBits() {
        return eglDepthBits;
    }

    public void setEglDepthBits(int eglDepthBits) {
        this.eglDepthBits = eglDepthBits;
    }

    public int getEglSamples() {
        return eglSamples;
    }

    public void setEglSamples(int eglSamples) {
        this.eglSamples = eglSamples;
    }

    public int getEglStencilBits() {
        return eglStencilBits;
    }

    public void setEglStencilBits(int eglStencilBits) {
        this.eglStencilBits = eglStencilBits;
    }

    public int getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }

    public String getAudioRendererType() {
        return audioRendererType;
    }

    public void setAudioRendererType(String audioRendererType) {
        this.audioRendererType = audioRendererType;
    }

    public boolean isEmulateKeyBoard() {
        return emulateKeyBoard;
    }

    public void setEmulateKeyBoard(boolean emulateKeyBoard) {
        this.emulateKeyBoard = emulateKeyBoard;
    }

    public boolean isEmulateMouse() {
        return emulateMouse;
    }

    public void setEmulateMouse(boolean emulateMouse) {
        this.emulateMouse = emulateMouse;
    }

    public boolean isUseJoyStickEvents() {
        return useJoyStickEvents;
    }

    public void setUseJoyStickEvents(boolean useJoyStickEvents) {
        this.useJoyStickEvents = useJoyStickEvents;
    }

    public boolean isGLThreadPaused() {
        return isGLThreadPaused;
    }

    protected void setGLThreadPaused(boolean glThreadPaused) {
        this.isGLThreadPaused = glThreadPaused;
    }

    public void setOnRendererCompleted(OnRendererCompleted onRendererCompleted) {
        this.onRendererCompleted = onRendererCompleted;
    }

    public void setOnExceptionThrown(OnExceptionThrown onExceptionThrown) {
        this.onExceptionThrown = onExceptionThrown;
    }

    public void setOnRendererStarted(OnRendererStarted onRendererStarted) {
        this.onRendererStarted = onRendererStarted;
    }

    public void setOnLayoutDrawn(OnLayoutDrawn onLayoutDrawn) {
        this.onLayoutDrawn = onLayoutDrawn;
    }

    public String getGlEsVersion() {
        return glEsVersion;
    }

    public boolean isShowErrorDialog() {
        return showErrorDialog;
    }

    public void setShowErrorDialog(boolean showErrorDialog) {
        this.showErrorDialog = showErrorDialog;
    }
}
