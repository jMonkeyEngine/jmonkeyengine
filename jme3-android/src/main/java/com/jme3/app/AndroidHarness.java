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
package com.jme3.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.input.JoyInput;
import com.jme3.input.TouchInput;
import com.jme3.input.android.AndroidSensorJoyInput;
import com.jme3.input.controls.TouchListener;
import com.jme3.input.controls.TouchTrigger;
import com.jme3.input.event.TouchEvent;
import com.jme3.system.AppSettings;
import com.jme3.system.SystemListener;
import com.jme3.system.android.JmeAndroidSystem;
import com.jme3.system.android.OGLESContext;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Legacy Activity wrapper for running a jME application on Android.
 *
 * @deprecated Use {@link AndroidHarnessFragment} from an AndroidX
 * {@link FragmentActivity} instead.
 */
@Deprecated
public class AndroidHarness extends FragmentActivity
        implements TouchListener, DialogInterface.OnClickListener, SystemListener {

    protected static final Logger logger = Logger.getLogger(AndroidHarness.class.getName());
    private static final String HARNESS_FRAGMENT_TAG = "com.jme3.app.AndroidHarness.fragment";
    private static final String ESCAPE_EVENT = "TouchEscape";

    /**
     * The application class to start.
     */
    protected String appClass = "jme3test.android.Test";

    /**
     * The jME application object.
     */
    protected LegacyApplication app;

    protected int eglBitsPerPixel = 24;
    protected int eglAlphaBits = 0;
    protected int eglDepthBits = 16;
    protected int eglSamples = 0;
    protected int eglStencilBits = 0;
    protected int frameRate = -1;
    protected String audioRendererType = AppSettings.ANDROID_OPENAL_SOFT;
    protected boolean joystickEventsEnabled = false;
    protected boolean keyEventsEnabled = true;
    protected boolean mouseEventsEnabled = true;
    protected boolean mouseEventsInvertX = false;
    protected boolean mouseEventsInvertY = false;
    protected boolean finishOnAppStop = true;
    protected boolean handleExitHook = true;
    protected String exitDialogTitle = "Do you want to exit?";
    protected String exitDialogMessage = "Use your home key to bring this app into the background or exit to terminate it.";
    protected boolean screenFullScreen = true;
    protected boolean screenShowTitle = true;
    protected int splashPicID = 0;

    protected OGLESContext ctx;
    protected GLSurfaceView view;
    protected boolean isGLThreadPaused = true;
    protected ImageView splashImageView;
    protected FrameLayout frameLayout;

    private boolean firstDrawFrame = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();

        HarnessFragment fragment = attachFragment();
        fragment.setFinishOnAppStop(finishOnAppStop);
    }

    private void configureWindow() {
        if (screenFullScreen) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else if (!screenShowTitle) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
    }

    public Application getJmeApplication() {
        return app;
    }

    private HarnessFragment attachFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment existingFragment = fragmentManager.findFragmentByTag(HARNESS_FRAGMENT_TAG);
        if (existingFragment instanceof HarnessFragment) {
            return (HarnessFragment) existingFragment;
        }

        HarnessFragment newFragment = new HarnessFragment();
        fragmentManager.beginTransaction()
                .replace(android.R.id.content, newFragment, HARNESS_FRAGMENT_TAG)
                .commit();
        return newFragment;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (app != null) {
            app.restart();
        }
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

        logger.log(Level.SEVERE, finalMessage);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(AndroidHarness.this)
                        .setTitle(finalTitle)
                        .setMessage(finalMessage)
                        .setPositiveButton("Close", AndroidHarness.this)
                        .create()
                        .show();
            }
        });
    }

    @Override
    public void onClick(DialogInterface dialog, int whichButton) {
        if (whichButton != DialogInterface.BUTTON_NEGATIVE) {
            if (app != null) {
                app.stop(true);
            }
            app = null;
            finish();
        }
    }

    @Override
    public void onTouch(String name, TouchEvent event, float tpf) {
        if (ESCAPE_EVENT.equals(name) && event.getType() == TouchEvent.Type.KEY_UP) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new AlertDialog.Builder(AndroidHarness.this)
                            .setTitle(exitDialogTitle)
                            .setMessage(exitDialogMessage)
                            .setPositiveButton("Yes", AndroidHarness.this)
                            .setNegativeButton("No", AndroidHarness.this)
                            .create()
                            .show();
                }
            });
        }
    }

    public void layoutDisplay() {
        logger.log(Level.FINE, "Splash Screen Picture Resource ID: {0}", splashPicID);
        frameLayout = null;
        splashImageView = null;

        if (splashPicID == 0 || view == null) {
            return;
        }

        frameLayout = new FrameLayout(this);
        frameLayout.addView(view);

        splashImageView = new ImageView(this);
        Drawable drawable = getResources().getDrawable(splashPicID);
        if (drawable instanceof NinePatchDrawable) {
            splashImageView.setBackgroundDrawable(drawable);
        } else {
            splashImageView.setImageResource(splashPicID);
        }

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER);
        frameLayout.addView(splashImageView, layoutParams);
    }

    public void removeSplashScreen() {
        if (splashImageView != null && frameLayout != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    splashImageView.setVisibility(View.INVISIBLE);
                    frameLayout.removeView(splashImageView);
                }
            });
        }
    }

    @Override
    public void initialize() {
        app.initialize();
        if (handleExitHook) {
            if (app.getInputManager().hasMapping(SimpleApplication.INPUT_MAPPING_EXIT)) {
                app.getInputManager().deleteMapping(SimpleApplication.INPUT_MAPPING_EXIT);
            }
            app.getInputManager().addMapping(ESCAPE_EVENT, new TouchTrigger(TouchInput.KEYCODE_BACK));
            app.getInputManager().addListener(this, new String[]{ESCAPE_EVENT});
        }
    }

    @Override
    public void reshape(int width, int height) {
        app.reshape(width, height);
    }

    @Override
    public void rescale(float x, float y) {
        app.rescale(x, y);
    }

    @Override
    public void update() {
        app.update();
        if (firstDrawFrame) {
            removeSplashScreen();
            firstDrawFrame = false;
        }
    }

    @Override
    public void requestClose(boolean esc) {
        app.requestClose(esc);
    }

    @Override
    public void destroy() {
        if (app != null) {
            app.destroy();
        }
        if (finishOnAppStop) {
            finish();
        }
    }

    @Override
    public void gainFocus() {
        logger.fine("gainFocus");
        if (view != null) {
            view.onResume();
        }

        if (app != null) {
            AudioRenderer audioRenderer = app.getAudioRenderer();
            if (audioRenderer != null) {
                audioRenderer.resumeAll();
            }

            JoyInput joyInput = app.getContext() != null ? app.getContext().getJoyInput() : null;
            if (joyInput instanceof AndroidSensorJoyInput) {
                ((AndroidSensorJoyInput) joyInput).resumeSensors();
            }

            app.gainFocus();
        }
        isGLThreadPaused = false;
    }

    @Override
    public void loseFocus() {
        logger.fine("loseFocus");
        if (app != null) {
            app.loseFocus();
        }

        if (view != null) {
            view.onPause();
        }

        if (app != null) {
            AudioRenderer audioRenderer = app.getAudioRenderer();
            if (audioRenderer != null) {
                audioRenderer.pauseAll();
            }

            JoyInput joyInput = app.getContext() != null ? app.getContext().getJoyInput() : null;
            if (joyInput instanceof AndroidSensorJoyInput) {
                ((AndroidSensorJoyInput) joyInput).pauseSensors();
            }
        }
        isGLThreadPaused = true;
    }

    public static class HarnessFragment extends AndroidHarnessFragment {

        private FrameLayout frameLayout;
        private ImageView splashImageView;

        private AndroidHarness harness() {
            return (AndroidHarness) requireActivity();
        }

        @Override
        protected LegacyApplication createApplication() throws Exception {
            AndroidHarness harness = harness();
            Class<?> clazz = Class.forName(harness.appClass);
            harness.app = (LegacyApplication) clazz.getDeclaredConstructor().newInstance();
            return harness.app;
        }

        @Override
        protected AppSettings createSettings() {
            AppSettings settings = super.createSettings();
            settings.setAudioRenderer(harness().audioRendererType);
            return settings;
        }

        @Override
        protected void configureSettings(AppSettings settings) {
            AndroidHarness harness = harness();
            settings.setEmulateMouse(harness.mouseEventsEnabled);
            settings.setEmulateMouseFlipAxis(harness.mouseEventsInvertX, harness.mouseEventsInvertY);
            settings.setUseJoysticks(harness.joystickEventsEnabled);
            settings.setEmulateKeyboard(harness.keyEventsEnabled);

            settings.setBitsPerPixel(harness.eglBitsPerPixel);
            settings.setAlphaBits(harness.eglAlphaBits);
            settings.setDepthBits(harness.eglDepthBits);
            settings.setSamples(harness.eglSamples);
            settings.setStencilBits(harness.eglStencilBits);
            settings.setFrameRate(harness.frameRate);
        }

        @Override
        public View onCreateView(android.view.LayoutInflater inflater,
                ViewGroup container, Bundle savedInstanceState) {
            View jmeView = super.onCreateView(inflater, container, savedInstanceState);
            AndroidHarness harness = harness();
            if (jmeView instanceof GLSurfaceView) {
                harness.view = (GLSurfaceView) jmeView;
            }
            harness.ctx = harness.app != null ? (OGLESContext) harness.app.getContext() : null;
            if (harness.app == null) {
                return jmeView;
            }

            harness.layoutDisplay();
            frameLayout = harness.frameLayout;
            splashImageView = harness.splashImageView;
            return frameLayout != null ? frameLayout : jmeView;
        }

        @Override
        public void onDestroyView() {
            if (splashImageView != null && splashImageView.getParent() instanceof ViewGroup) {
                ((ViewGroup) splashImageView.getParent()).removeView(splashImageView);
            }
            if (frameLayout != null && frameLayout.getParent() instanceof ViewGroup) {
                ((ViewGroup) frameLayout.getParent()).removeView(frameLayout);
            }
            splashImageView = null;
            frameLayout = null;
            AndroidHarness harness = harness();
            harness.frameLayout = null;
            harness.splashImageView = null;
            harness.view = null;
            JmeAndroidSystem.setView(null);
            super.onDestroyView();
        }

        @Override
        public void initialize() {
            harness().initialize();
        }

        @Override
        public void reshape(int width, int height) {
            harness().reshape(width, height);
        }

        @Override
        public void rescale(float x, float y) {
            harness().rescale(x, y);
        }

        @Override
        public void update() {
            harness().update();
        }

        @Override
        public void requestClose(boolean esc) {
            harness().requestClose(esc);
        }

        @Override
        public void destroy() {
            harness().destroy();
        }

        @Override
        public void gainFocus() {
            harness().gainFocus();
        }

        @Override
        public void loseFocus() {
            harness().loseFocus();
        }

        @Override
        public void handleError(String errorMsg, Throwable throwable) {
            harness().handleError(errorMsg, throwable);
        }
    }
}
