package com.jme3.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.audio.android.AndroidAudioRenderer;
import com.jme3.input.JoyInput;
import com.jme3.input.TouchInput;
import com.jme3.input.android.AndroidSensorJoyInput;
import com.jme3.input.controls.TouchListener;
import com.jme3.input.controls.TouchTrigger;
import com.jme3.input.event.TouchEvent;
import com.jme3.renderer.android.AndroidGLSurfaceView;
import com.jme3.system.AppSettings;
import com.jme3.system.SystemListener;
import com.jme3.system.android.AndroidConfigChooser;
import com.jme3.system.android.AndroidConfigChooser.ConfigType;
import com.jme3.system.android.JmeAndroidSystem;
import com.jme3.system.android.OGLESContext;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>AndroidHarness</code> wraps a jme application object and runs it on
 * Android
 *
 * @author Kirill
 * @author larynx
 */
public class AndroidHarness extends Activity implements TouchListener, DialogInterface.OnClickListener, SystemListener {

    protected final static Logger logger = Logger.getLogger(AndroidHarness.class.getName());
    /**
     * The application class to start
     */
    protected String appClass = "jme3test.android.Test";
    /**
     * The jme3 application object
     */
    protected Application app = null;

    /**
     * ConfigType.FASTEST is RGB565, GLSurfaceView default ConfigType.BEST is
     * RGBA8888 or better if supported by the hardware
     */
    protected ConfigType eglConfigType = ConfigType.FASTEST;
    /**
     * If true all valid and not valid egl configs are logged
     * @deprecated this has no use
     */
    @Deprecated
    protected boolean eglConfigVerboseLogging = false;
    
    /**
     * set to 2, 4 to enable multisampling.
     */
    protected int antiAliasingSamples = 0;
    /**
     * If true Android Sensors are used as simulated Joysticks Users can use the
     * Android sensor feedback through the RawInputListener or by registering
     * JoyAxisTriggers.
     */
    protected boolean joystickEventsEnabled = false;
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
     * Set the screen window mode. If screenFullSize is true, then the
     * notification bar and title bar are removed and the screen covers the
     * entire display.   If screenFullSize is false, then the notification bar
     * remains visible if screenShowTitle is true while screenFullScreen is
     * false, then the title bar is also displayed under the notification bar.
     */
    protected boolean screenFullScreen = true;
    /**
     * if screenShowTitle is true while screenFullScreen is false, then the
     * title bar is also displayed under the notification bar
     */
    protected boolean screenShowTitle = true;
    /**
     * Splash Screen picture Resource ID. If a Splash Screen is desired, set
     * splashPicID to the value of the Resource ID (i.e. R.drawable.picname). If
     * splashPicID = 0, then no splash screen will be displayed.
     */
    protected int splashPicID = 0;
    /**
     * Set the screen orientation, default is SENSOR
     * ActivityInfo.SCREEN_ORIENTATION_* constants package
     * android.content.pm.ActivityInfo
     *
     * SCREEN_ORIENTATION_UNSPECIFIED SCREEN_ORIENTATION_LANDSCAPE
     * SCREEN_ORIENTATION_PORTRAIT SCREEN_ORIENTATION_USER
     * SCREEN_ORIENTATION_BEHIND SCREEN_ORIENTATION_SENSOR (default)
     * SCREEN_ORIENTATION_NOSENSOR
     */
    protected int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
    protected OGLESContext ctx;
    protected AndroidGLSurfaceView view = null;
    protected boolean isGLThreadPaused = true;
    protected ImageView splashImageView = null;
    protected FrameLayout frameLayout = null;
    final private String ESCAPE_EVENT = "TouchEscape";
    private boolean firstDrawFrame = true;
    private boolean inConfigChange = false;

    private class DataObject {
        protected Application app = null;
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        logger.log(Level.FINE, "onRetainNonConfigurationInstance");
        final DataObject data = new DataObject();
        data.app = this.app;
        inConfigChange = true;

        return data;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        logger.fine("onCreate");
        super.onCreate(savedInstanceState);

        JmeAndroidSystem.setActivity(this);
        if (screenFullScreen) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            if (!screenShowTitle) {
                requestWindowFeature(Window.FEATURE_NO_TITLE);
            }
        }

        setRequestedOrientation(screenOrientation);

        final DataObject data = (DataObject) getLastNonConfigurationInstance();
        if (data != null) {
            logger.log(Level.FINE, "Using Retained App");
            this.app = data.app;

        } else {
            // Discover the screen reolution
            //TODO try to find a better way to get a hand on the resolution
            WindowManager wind = this.getWindowManager();
            Display disp = wind.getDefaultDisplay();
            Log.d("AndroidHarness", "Resolution from Window, width:" + disp.getWidth() + ", height: " + disp.getHeight());

            // Create Settings
            logger.log(Level.FINE, "Creating settings");
            AppSettings settings = new AppSettings(true);
            settings.setEmulateMouse(mouseEventsEnabled);
            settings.setEmulateMouseFlipAxis(mouseEventsInvertX, mouseEventsInvertY);
            settings.setUseJoysticks(joystickEventsEnabled);
            settings.setSamples(antiAliasingSamples);
            settings.setResolution(disp.getWidth(), disp.getHeight());
            settings.put(AndroidConfigChooser.SETTINGS_CONFIG_TYPE, eglConfigType);
            

            // Create application instance
            try {
                if (app == null) {
                    @SuppressWarnings("unchecked")
                    Class<? extends Application> clazz = (Class<? extends Application>) Class.forName(appClass);
                    app = clazz.newInstance();
                }

                app.setSettings(settings);
                app.start();


            } catch (Exception ex) {
                handleError("Class " + appClass + " init failed", ex);
                setContentView(new TextView(this));
            }
        }

        ctx = (OGLESContext) app.getContext();
        view = ctx.createView();
        // AndroidHarness wraps the app as a SystemListener.
        ctx.setSystemListener(this);
        layoutDisplay();

    }

    @Override
    protected void onRestart() {
        logger.fine("onRestart");
        super.onRestart();
        if (app != null) {
            app.restart();
        }

    }

    @Override
    protected void onStart() {
        logger.fine("onStart");
        super.onStart();
    }

    @Override
    protected void onResume() {
        logger.fine("onResume");
        super.onResume();
        if (view != null) {
            view.onResume();
        }

        if (app != null) {
            //resume the audio
            AudioRenderer result = app.getAudioRenderer();
            if (result != null) {
                if (result instanceof AndroidAudioRenderer) {
                    AndroidAudioRenderer renderer = (AndroidAudioRenderer) result;
                    renderer.resumeAll();
                }
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

        isGLThreadPaused = false;

        gainFocus();
    }

    @Override
    protected void onPause() {
        loseFocus();

        logger.fine("onPause");
        super.onPause();
        if (view != null) {
            view.onPause();
        }

        if (app != null) {
            //pause the audio
            AudioRenderer result = app.getAudioRenderer();
            if (result != null) {
                logger.log(Level.FINE, "pause: {0}", result.getClass().getSimpleName());
                if (result instanceof AndroidAudioRenderer) {
                    AndroidAudioRenderer renderer = (AndroidAudioRenderer) result;
                    renderer.pauseAll();
                }
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
        isGLThreadPaused = true;

    }

    @Override
    protected void onStop() {
        logger.fine("onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        logger.fine("onDestroy");
        final DataObject data = (DataObject) getLastNonConfigurationInstance();
        if (data != null || inConfigChange) {
            logger.fine("In Config Change, not stopping app.");
        } else {
            if (app != null) {
                app.stop(!isGLThreadPaused);
            }
        }
        JmeAndroidSystem.setActivity(null);
        ctx = null;
        app = null;
        view = null;

        super.onDestroy();
    }

    public Application getJmeApplication() {
        return app;
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

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog dialog = new AlertDialog.Builder(AndroidHarness.this) // .setIcon(R.drawable.alert_dialog_icon)
                        .setTitle(finalTitle).setPositiveButton("Kill", AndroidHarness.this).setMessage(finalMsg).create();
                dialog.show();
            }
        });
    }

    /**
     * Called by the android alert dialog, terminate the activity and OpenGL
     * rendering
     *
     * @param dialog
     * @param whichButton
     */
    public void onClick(DialogInterface dialog, int whichButton) {
        if (whichButton != -2) {
            if (app != null) {
                app.stop(true);
            }
            app = null;
            this.finish();
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
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog dialog = new AlertDialog.Builder(AndroidHarness.this) // .setIcon(R.drawable.alert_dialog_icon)
                                    .setTitle(exitDialogTitle).setPositiveButton("Yes", AndroidHarness.this).setNegativeButton("No", AndroidHarness.this).setMessage(exitDialogMessage).create();
                            dialog.show();
                        }
                    });
                    break;
                default:
                    break;
            }
        }
    }

    public void layoutDisplay() {
        logger.log(Level.FINE, "Splash Screen Picture Resource ID: {0}", splashPicID);
        if (view == null) {
            logger.log(Level.FINE, "view is null!");
        }
        if (splashPicID != 0) {
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    LayoutParams.FILL_PARENT,
                    LayoutParams.FILL_PARENT,
                    Gravity.CENTER);

            frameLayout = new FrameLayout(this);
            splashImageView = new ImageView(this);

            Drawable drawable = this.getResources().getDrawable(splashPicID);
            if (drawable instanceof NinePatchDrawable) {
                splashImageView.setBackgroundDrawable(drawable);
            } else {
                splashImageView.setImageResource(splashPicID);
            }

            if (view.getParent() != null) {
                ((ViewGroup) view.getParent()).removeView(view);
            }
            frameLayout.addView(view);

            if (splashImageView.getParent() != null) {
                ((ViewGroup) splashImageView.getParent()).removeView(splashImageView);
            }
            frameLayout.addView(splashImageView, lp);

            setContentView(frameLayout);
            logger.log(Level.FINE, "Splash Screen Created");
        } else {
            logger.log(Level.FINE, "Splash Screen Skipped.");
            setContentView(view);
        }
    }

    public void removeSplashScreen() {
        logger.log(Level.FINE, "Splash Screen Picture Resource ID: {0}", splashPicID);
        if (splashPicID != 0) {
            if (frameLayout != null) {
                if (splashImageView != null) {
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            splashImageView.setVisibility(View.INVISIBLE);
                            frameLayout.removeView(splashImageView);
                        }
                    });
                } else {
                    logger.log(Level.FINE, "splashImageView is null");
                }
            } else {
                logger.log(Level.FINE, "frameLayout is null");
            }
        }
    }

    public void initialize() {
        app.initialize();
        if (handleExitHook) {
            app.getInputManager().addMapping(ESCAPE_EVENT, new TouchTrigger(TouchInput.KEYCODE_BACK));
            app.getInputManager().addListener(this, new String[]{ESCAPE_EVENT});
        }
    }

    public void reshape(int width, int height) {
        app.reshape(width, height);
    }

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

    public void requestClose(boolean esc) {
        app.requestClose(esc);
    }

    public void destroy() {
        if (app != null) {
            app.destroy();
        }
        if (finishOnAppStop) {
            finish();
        }
    }

    public void gainFocus() {
        if (app != null) {
            app.gainFocus();
        }
    }

    public void loseFocus() {
        if (app != null) {
            app.loseFocus();
        }
    }
}
