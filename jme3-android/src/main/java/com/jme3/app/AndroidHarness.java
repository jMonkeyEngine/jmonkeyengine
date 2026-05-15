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
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.fragment.app.FragmentActivity;
import com.jme3.input.TouchInput;
import com.jme3.input.controls.TouchListener;
import com.jme3.input.controls.TouchTrigger;
import com.jme3.input.event.TouchEvent;
import com.jme3.system.AppSettings;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Legacy Activity wrapper for running a jME application on Android.
 *
 * @deprecated Use {@link AndroidHarnessFragment} from an AndroidX
 * {@link FragmentActivity} instead.
 */
@Deprecated
public class AndroidHarness extends FragmentActivity {

    protected static final Logger logger = Logger.getLogger(AndroidHarness.class.getName());

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

    private HarnessFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();

        fragment = new HarnessFragment();
        fragment.setFinishOnAppStop(finishOnAppStop);
        attachFragment(fragment);
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

    private void attachFragment(AndroidHarnessFragment fragment) {
        try {
            Method getSupportFragmentManager = getClass().getMethod("getSupportFragmentManager");
            Object fragmentManager = getSupportFragmentManager.invoke(this);
            Object transaction = fragmentManager.getClass().getMethod("beginTransaction").invoke(fragmentManager);
            transaction = transaction.getClass()
                    .getMethod("replace", int.class, androidx.fragment.app.Fragment.class)
                    .invoke(transaction, android.R.id.content, fragment);
            transaction.getClass().getMethod("commit").invoke(transaction);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to attach AndroidHarnessFragment", exception);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (app != null) {
            app.restart();
        }
    }

    public static class HarnessFragment extends AndroidHarnessFragment
            implements TouchListener, DialogInterface.OnClickListener {

        private static final String ESCAPE_EVENT = "TouchEscape";

        private FrameLayout frameLayout;
        private ImageView splashImageView;
        private boolean firstDrawFrame = true;

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
            if (harness.splashPicID == 0 || harness.app == null) {
                return jmeView;
            }

            frameLayout = new FrameLayout(harness);
            frameLayout.addView(jmeView);

            splashImageView = new ImageView(harness);
            Drawable drawable = getResources().getDrawable(harness.splashPicID);
            if (drawable instanceof NinePatchDrawable) {
                splashImageView.setBackgroundDrawable(drawable);
            } else {
                splashImageView.setImageResource(harness.splashPicID);
            }

            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER);
            frameLayout.addView(splashImageView, layoutParams);
            return frameLayout;
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
            super.onDestroyView();
        }

        @Override
        public void initialize() {
            super.initialize();
            AndroidHarness harness = harness();
            if (harness.handleExitHook) {
                if (harness.app.getInputManager().hasMapping(SimpleApplication.INPUT_MAPPING_EXIT)) {
                    harness.app.getInputManager().deleteMapping(SimpleApplication.INPUT_MAPPING_EXIT);
                }
                harness.app.getInputManager().addMapping(ESCAPE_EVENT, new TouchTrigger(TouchInput.KEYCODE_BACK));
                harness.app.getInputManager().addListener(this, new String[]{ESCAPE_EVENT});
            }
        }

        @Override
        public void update() {
            super.update();
            if (firstDrawFrame) {
                removeSplashScreen();
                firstDrawFrame = false;
            }
        }

        @Override
        public void onTouch(String name, TouchEvent event, float tpf) {
            if (ESCAPE_EVENT.equals(name) && event.getType() == TouchEvent.Type.KEY_UP) {
                harness().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AndroidHarness harness = harness();
                        new AlertDialog.Builder(harness)
                                .setTitle(harness.exitDialogTitle)
                                .setMessage(harness.exitDialogMessage)
                                .setPositiveButton("Yes", HarnessFragment.this)
                                .setNegativeButton("No", HarnessFragment.this)
                                .create()
                                .show();
                    }
                });
            }
        }

        @Override
        public void onClick(DialogInterface dialog, int whichButton) {
            if (whichButton != DialogInterface.BUTTON_NEGATIVE) {
                AndroidHarness harness = harness();
                if (harness.app != null) {
                    harness.app.stop(true);
                }
                harness.app = null;
                harness.finish();
            }
        }

        private void removeSplashScreen() {
            if (splashImageView != null && frameLayout != null) {
                harness().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        splashImageView.setVisibility(View.INVISIBLE);
                        frameLayout.removeView(splashImageView);
                    }
                });
            }
        }
    }
}
