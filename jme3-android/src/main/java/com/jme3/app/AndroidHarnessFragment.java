/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.jme3.audio.AudioRenderer;
import com.jme3.input.JoyInput;
import com.jme3.input.TouchInput;
import com.jme3.input.android.AndroidSensorJoyInput;
import com.jme3.input.controls.TouchListener;
import com.jme3.input.controls.TouchTrigger;
import com.jme3.input.event.TouchEvent;
import static com.jme3.input.event.TouchEvent.Type.KEY_UP;
import com.jme3.system.AppSettings;
import com.jme3.system.SystemListener;
import com.jme3.system.android.JmeAndroidSystem;
import com.jme3.system.android.OGLESContext;
import com.jme3.util.AndroidLogHandler;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 *
 * @author iwgeric
 */
public class AndroidHarnessFragment extends Fragment implements
        TouchListener, DialogInterface.OnClickListener, View.OnLayoutChangeListener, SystemListener {
    private static final Logger logger = Logger.getLogger(AndroidHarnessFragment.class.getName());

    /**
     * The application class to start
     */
    protected String appClass = "jme3test.android.Test";

    /**
     * Sets the desired RGB size for the surfaceview.  16 = RGB565, 24 = RGB888.
     * (default = 24)
     */
    protected int eglBitsPerPixel = 24;

    /**
     * Sets the desired number of Alpha bits for the surfaceview.  This affects
     * how the surfaceview is able to display Android views that are located
     * under the surfaceview jME uses to render the scenegraph.
     * 0 = Opaque surfaceview background (fastest)
     * 1-&gt;7 = Transparent surfaceview background
     * 8 or higher = Translucent surfaceview background
     * (default = 0)
     */
    protected int eglAlphaBits = 0;

    /**
     * The number of depth bits specifies the precision of the depth buffer.
     * (default = 16)
     */
    protected int eglDepthBits = 16;

    /**
     * Sets the number of samples to use for multisampling.<br>
     * Leave 0 (default) to disable multisampling.<br>
     * Set to 2 or 4 to enable multisampling.
     */
    protected int eglSamples = 0;

    /**
     * Set the number of stencil bits.
     * (default = 0)
     */
    protected int eglStencilBits = 0;

    /**
     * Set the desired frame rate.  If frameRate higher than 0, the application
     * will be capped at the desired frame rate.
     * (default = -1, no frame rate cap)
     */
    protected int frameRate = -1;

    /**
     * Set the maximum resolution for the surfaceview in either the
     * width or height screen direction depending on the screen size.
     * If the surfaceview is rectangular, the longest side (width or height)
     * will have the resolution set to a maximum of maxResolutionDimension.
     * The other direction will be set to a value that maintains the aspect
     * ratio of the surfaceview. <br>
     * Any value less than 0 (default = -1) will result in the surfaceview having the
     * same resolution as the view layout (i.e. no max resolution).
     */
    protected int maxResolutionDimension = -1;

    /**
     * Sets the type of Audio Renderer to be used.
     * <p>
     * Android MediaPlayer / SoundPool can be used on all
     * supported Android platform versions (2.2+)<br>
     * OpenAL Soft uses an OpenSL backend and is only supported on Android
     * versions 2.3+.
     * <p>
     * Only use ANDROID_ static strings found in AppSettings
     *
     */
    protected String audioRendererType = AppSettings.ANDROID_OPENAL_SOFT;

    /**
     * If true Android Sensors are used as simulated Joysticks. Users can use the
     * Android sensor feedback through the RawInputListener or by registering
     * JoyAxisTriggers.
     */
    protected boolean joystickEventsEnabled = false;

    /**
     * If true KeyEvents are generated from TouchEvents
     */
    protected boolean keyEventsEnabled = true;

    /**
     * If true MouseEvents are generated from TouchEvents
     */
    protected boolean mouseEventsEnabled = true;

    /**
     * Flip X axis
     */
    protected boolean mouseEventsInvertX = false;

    /**
     * Flip Y axis
     */
    protected boolean mouseEventsInvertY = false;

    /**
     * if true finish this activity when the jme app is stopped
     */
    protected boolean finishOnAppStop = true;

    /**
     * set to false if you don't want the harness to handle the exit hook
     */
    protected boolean handleExitHook = true;

    /**
     * Title of the exit dialog, default is "Do you want to exit?"
     */
    protected String exitDialogTitle = "Do you want to exit?";

    /**
     * Message of the exit dialog, default is "Use your home key to bring this
     * app into the background or exit to terminate it."
     */
    protected String exitDialogMessage = "Use your home key to bring this app into the background or exit to terminate it.";

    /**
     * Splash Screen picture Resource ID. If a Splash Screen is desired, set
     * splashPicID to the value of the Resource ID (i.e. R.drawable.picname). If
     * splashPicID = 0, then no splash screen will be displayed.
     */
    protected int splashPicID = 0;

    protected FrameLayout frameLayout = null;
    protected GLSurfaceView view = null;
    protected ImageView splashImageView = null;
    final private String ESCAPE_EVENT = "TouchEscape";
    private boolean firstDrawFrame = true;
    private LegacyApplication app = null;
    private int viewWidth = 0;
    private int viewHeight = 0;

    // Retrieves the jME application object
    public Application getJmeApplication() {
        return app;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    /**
     * This Fragment uses setRetainInstance(true) so the onCreate method will only
     * be called once. During device configuration changes, the instance of
     * this Fragment will be reused in the new Activity.  This method should not
     * contain any View related objects.  They are created and destroyed by
     * other methods.  View related objects should not be reused, but rather
     * created and destroyed along with the Activity.
     *
     * @param savedInstanceState the saved instance state
     */
    @Override
    @SuppressWarnings("unchecked")
    public void onCreate(Bundle savedInstanceState) {
        initializeLogHandler();
        logger.fine("onCreate");
        super.onCreate(savedInstanceState);

        // Create Settings
        logger.log(Level.FINE, "Creating settings");
        AppSettings settings = new AppSettings(true);
        settings.setEmulateMouse(mouseEventsEnabled);
        settings.setEmulateMouseFlipAxis(mouseEventsInvertX, mouseEventsInvertY);
        settings.setUseJoysticks(joystickEventsEnabled);
        settings.setEmulateKeyboard(keyEventsEnabled);

        settings.setBitsPerPixel(eglBitsPerPixel);
        settings.setAlphaBits(eglAlphaBits);
        settings.setDepthBits(eglDepthBits);
        settings.setSamples(eglSamples);
        settings.setStencilBits(eglStencilBits);
        settings.setAudioRenderer(audioRendererType);

        settings.setFrameRate(frameRate);

        // Create application instance
        try {
            if (app == null) {
                Class clazz = Class.forName(appClass);
                app = (LegacyApplication) clazz.getDeclaredConstructor().newInstance();
            }

            app.setSettings(settings);
            app.start();
        } catch (Exception ex) {
            handleError("Class " + appClass + " init failed", ex);
        }

        OGLESContext ctx = (OGLESContext) app.getContext();
        // AndroidHarness wraps the app as a SystemListener.
        ctx.setSystemListener(this);

        setRetainInstance(true);
    }

    /**
     * Called by the system to create the View hierarchy associated with this
     * Fragment.  For jME, this is a FrameLayout that contains the GLSurfaceView
     * and an overlaying SplashScreen Image (if used).  The View that is returned
     * will be placed on the screen within the boundaries of the View borders defined
     * by the Activity's layout parameters for this Fragment.  For jME, we also
     * update the application reference to the new view.
     *
     * @param inflater ignored
     * @param container ignored
     * @param savedInstanceState ignored
     * @return the new view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        logger.fine("onCreateView");
        // Create the GLSurfaceView for the application
        view = ((OGLESContext) app.getContext()).createView(getActivity());
        // store the glSurfaceView in JmeAndroidSystem for future use
        JmeAndroidSystem.setView(view);
        createLayout();
        view.addOnLayoutChangeListener(this);
        return frameLayout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        logger.fine("onActivityCreated");
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        logger.fine("onStart");
        super.onStart();
    }

    /**
     * When the Fragment resumes (i.e. after app resumes or device screen turned
     * back on), call the gainFocus() in the jME application.
     */
    @Override
    public void onResume() {
        logger.fine("onResume");
        super.onResume();

        gainFocus();
    }

    /**
     * When the Fragment pauses (i.e. after home button pressed on the device
     * or device screen turned off) , call the loseFocus() in the jME application.
     */
    @Override
    public void onPause() {
        logger.fine("onPause");
        loseFocus();

        super.onPause();
    }

    @Override
    public void onStop() {
        logger.fine("onStop");
        super.onStop();
    }

    /**
     * Called by the Android system each time the Activity is destroyed or recreated.
     * For jME, we clear references to the GLSurfaceView.
     */
    @Override
    public void onDestroyView() {
        logger.fine("onDestroyView");
        if (splashImageView != null && splashImageView.getParent() != null) {
            ((ViewGroup) splashImageView.getParent()).removeView(splashImageView);
        }
        if (view.getParent() != null) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
        if (frameLayout != null && frameLayout.getParent() != null) {
            ((ViewGroup) frameLayout.getParent()).removeView(frameLayout);
        }
        view.removeOnLayoutChangeListener(this);

        splashImageView = null;
        frameLayout = null;
        view = null;
        JmeAndroidSystem.setView(null);

        super.onDestroyView();
    }

    /**
     * Called by the system when the application is being destroyed.  In this case,
     * the jME application is actually closed as well.  This method is not called
     * during device configuration changes or when the application is put in the
     * background.
     */
    @Override
    public void onDestroy() {
        logger.fine("onDestroy");
        if (app != null) {
            app.stop(false);
        }
        app = null;
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        logger.fine("onDetach");
        super.onDetach();
    }


    /**
     * Called when an error has occurred. By default, will show an error message
     * to the user and print the exception/error to the log.
     */
    @Override
    public void handleError(final String errorMsg, final Throwable t) {
        String stackTrace = "";
        String title = "Error";

        if (t != null) {
            // Convert exception to string
            StringWriter sw = new StringWriter(100);
            t.printStackTrace(new PrintWriter(sw));
            stackTrace = sw.toString();
            title = t.toString();
        }

        final String finalTitle = title;
        final String finalMsg = (errorMsg != null ? errorMsg : "Uncaught Exception")
                + "\n" + stackTrace;

        logger.log(Level.SEVERE, finalMsg);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(finalTitle);
                builder.setPositiveButton("Kill", AndroidHarnessFragment.this);
                builder.setMessage(finalMsg);

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    /**
     * Called by the android alert dialog, terminate the activity and OpenGL
     * rendering
     *
     * @param dialog ignored
     * @param whichButton the button index
     */
    @Override
    public void onClick(DialogInterface dialog, int whichButton) {
        if (whichButton != -2) {
            if (app != null) {
                app.stop(true);
            }
            app = null;
            getActivity().finish();
        }
    }

    /**
     * Gets called by the InputManager on all touch/drag/scale events
     */
    @Override
    public void onTouch(String name, TouchEvent evt, float tpf) {
        if (name.equals(ESCAPE_EVENT)) {
            switch (evt.getType()) {
                case KEY_UP:
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle(exitDialogTitle);
                            builder.setPositiveButton("Yes", AndroidHarnessFragment.this);
                            builder.setNegativeButton("No", AndroidHarnessFragment.this);
                            builder.setMessage(exitDialogMessage);

                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    });
                    break;
                default:
                    break;
            }
        }
    }

    public void createLayout() {
        logger.log(Level.FINE, "Splash Screen Picture Resource ID: {0}", splashPicID);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER);

        if (frameLayout != null && frameLayout.getParent() != null) {
            ((ViewGroup) frameLayout.getParent()).removeView(frameLayout);
        }
        frameLayout = new FrameLayout(getActivity());

        if (view.getParent() != null) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
        frameLayout.addView(view);

        if (splashPicID != 0) {
            splashImageView = new ImageView(getActivity());

            Drawable drawable = getResources().getDrawable(splashPicID);
            if (drawable instanceof NinePatchDrawable) {
                splashImageView.setBackgroundDrawable(drawable);
            } else {
                splashImageView.setImageResource(splashPicID);
            }

            if (splashImageView.getParent() != null) {
                ((ViewGroup) splashImageView.getParent()).removeView(splashImageView);
            }
            frameLayout.addView(splashImageView, lp);

           logger.fine("Splash Screen Created");
        } else {
            logger.fine("Splash Screen Skipped.");
        }
    }

    public void removeSplashScreen() {
        logger.log(Level.FINE, "Splash Screen Picture Resource ID: {0}", splashPicID);
        if (splashPicID != 0) {
            if (frameLayout != null) {
                if (splashImageView != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            splashImageView.setVisibility(View.INVISIBLE);
                            frameLayout.removeView(splashImageView);
                        }
                    });
                } else {
                    logger.fine("splashImageView is null");
                }
            } else {
                logger.fine("frameLayout is null");
            }
        }
    }

    /**
     * Removes the standard Android log handler due to an issue with not logging
     * entries lower than INFO level and adds a handler that produces
     * JME formatted log messages.
     */
    protected void initializeLogHandler() {
        Logger log = LogManager.getLogManager().getLogger("");
        for (Handler handler : log.getHandlers()) {
            if (log.getLevel() != null && log.getLevel().intValue() <= Level.FINE.intValue()) {
                Log.v("AndroidHarness", "Removing Handler class: " + handler.getClass().getName());
            }
            log.removeHandler(handler);
        }
        Handler handler = new AndroidLogHandler();
        log.addHandler(handler);
        handler.setLevel(Level.ALL);
    }

    @Override
    public void initialize() {
        app.initialize();
        if (handleExitHook) {
            // remove existing mapping from SimpleApplication that stops the app
            // when the esc key is pressed (esc key = android back key) so that
            // AndroidHarness can produce the exit app dialog box.
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
        // call to remove the splash screen, if present.
        // call after app.update() to make sure no gap between
        // splash screen going away and app display being shown.
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
            getActivity().finish();
        }
    }

    @Override
    public void gainFocus() {
        logger.fine("gainFocus");
        if (view != null) {
            view.onResume();
        }

        if (app != null) {
            //resume the audio
            AudioRenderer audioRenderer = app.getAudioRenderer();
            if (audioRenderer != null) {
                audioRenderer.resumeAll();
            }
            //resume the sensors (aka joysticks)
            if (app.getContext() != null) {
                JoyInput joyInput = app.getContext().getJoyInput();
                if (joyInput != null) {
                    if (joyInput instanceof AndroidSensorJoyInput) {
                        AndroidSensorJoyInput androidJoyInput = (AndroidSensorJoyInput) joyInput;
                        androidJoyInput.resumeSensors();
                    }
                }
            }
        }

        if (app != null) {
            app.gainFocus();
        }
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
            //pause the audio
            AudioRenderer audioRenderer = app.getAudioRenderer();
            if (audioRenderer != null) {
                audioRenderer.pauseAll();
            }
            //pause the sensors (aka joysticks)
            if (app.getContext() != null) {
                JoyInput joyInput = app.getContext().getJoyInput();
                if (joyInput != null) {
                    if (joyInput instanceof AndroidSensorJoyInput) {
                        AndroidSensorJoyInput androidJoyInput = (AndroidSensorJoyInput) joyInput;
                        androidJoyInput.pauseSensors();
                    }
                }
            }
        }

    }

    @Override
    public void onLayoutChange(View v,
            int left, int top, int right, int bottom,
            int oldLeft, int oldTop, int oldRight, int oldBottom) {

        if (v.equals(view)) {
//            logger.log(Level.INFO, "surfaceview layout changed. left: {0}, top: {1}, right: {2}, bottom: {3}",
//                    new Object[]{left, top, right, bottom});

            if (v.equals(view) && maxResolutionDimension > 0) {
                int newWidth = right-left;
                int newHeight = bottom-top;

                if (viewWidth != newWidth || viewHeight != newHeight) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "SurfaceView layout changed: old width: {0}, old height: {1}, new width: {2}, new height: {3}",
                                new Object[]{viewWidth, viewHeight, newWidth, newHeight});
                    }
                    viewWidth = newWidth;
                    viewHeight = newHeight;

                    int fixedSizeWidth = viewWidth;
                    int fixedSizeHeight = viewHeight;
                    if (viewWidth > viewHeight && viewWidth > maxResolutionDimension) {
                        // landscape
                        fixedSizeWidth = maxResolutionDimension;
                        fixedSizeHeight = (int)(maxResolutionDimension * (viewHeight / (float)viewWidth));
                    } else if (viewHeight > viewWidth && viewHeight > maxResolutionDimension) {
                        // portrait
                        fixedSizeWidth = (int)(maxResolutionDimension * (viewWidth / (float)viewHeight));
                        fixedSizeHeight = maxResolutionDimension;
                    } else if (viewWidth == viewHeight && viewWidth > maxResolutionDimension) {
                        fixedSizeWidth = maxResolutionDimension;
                        fixedSizeHeight = maxResolutionDimension;
                    }
                    // set the surfaceview resolution if the size != current view size
                    if (fixedSizeWidth != viewWidth || fixedSizeHeight != viewHeight) {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, "setting surfaceview resolution to width: {0}, height: {1}",
                                    new Object[]{fixedSizeWidth, fixedSizeHeight});
                        }
                        view.getHolder().setFixedSize(fixedSizeWidth, fixedSizeHeight);
                    }
                }
            }
        }
    }

}
