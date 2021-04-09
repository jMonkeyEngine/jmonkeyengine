package org.jmonkeyengine.jme3androidexamples;

import android.os.Bundle;
import com.jme3.app.AndroidHarnessFragment;
import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 * A placeholder fragment containing a jME GLSurfaceView.
 */
public class JmeFragment extends AndroidHarnessFragment {

    public JmeFragment() {
        // Set the desired EGL configuration
        eglBitsPerPixel = 24;
        eglAlphaBits = 0;
        eglDepthBits = 16;
        eglSamples = 0;
        eglStencilBits = 0;

        // Set the maximum framerate
        // (default = -1 for unlimited)
        frameRate = -1;

        // Set the maximum resolution dimension
        // (the smaller side, height or width, is set automatically
        // to maintain the original device screen aspect ratio)
        // (default = -1 to match device screen resolution)
        maxResolutionDimension = -1;

        /*
        Skip these settings and use the settings stored in the Bundle retrieved during onCreate.

        // Set main project class (fully qualified path)
        appClass = "";

        // Set input configuration settings
        joystickEventsEnabled = false;
        keyEventsEnabled = true;
        mouseEventsEnabled = true;
        */

        // Set application exit settings
        finishOnAppStop = true;
        handleExitHook = true;
        exitDialogTitle = "Do you want to exit?";
        exitDialogMessage = "Use your home key to bring this app into the background or exit to terminate it.";

        // Set splash screen resource id, if used
        // (default = 0, no splash screen)
        // For example, if the image file name is "splash"...
        //     splashPicID = R.drawable.splash;
        splashPicID = 0;
//        splashPicID = R.drawable.android_splash;

        // Set the default logging level (default=Level.INFO, Level.ALL=All Debug Info)
        LogManager.getLogManager().getLogger("").setLevel(Level.INFO);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Bundle bundle=getArguments();

        appClass = bundle.getString(MainActivity.SELECTED_APP_CLASS);
//        Log.d(this.getClass().getSimpleName(), "AppClass: " + appClass);
        joystickEventsEnabled = bundle.getBoolean(MainActivity.ENABLE_JOYSTICK_EVENTS);
//        Log.d(this.getClass().getSimpleName(), "JoystickEventsEnabled: " + joystickEventsEnabled);
        keyEventsEnabled = bundle.getBoolean(MainActivity.ENABLE_KEY_EVENTS);
//        Log.d(this.getClass().getSimpleName(), "KeyEventsEnabled: " + keyEventsEnabled);
        mouseEventsEnabled = bundle.getBoolean(MainActivity.ENABLE_MOUSE_EVENTS);
//        Log.d(this.getClass().getSimpleName(), "MouseEventsEnabled: " + mouseEventsEnabled);
        boolean verboseLogging = bundle.getBoolean(MainActivity.VERBOSE_LOGGING);
//        Log.d(this.getClass().getSimpleName(), "VerboseLogging: " + verboseLogging);
        if (verboseLogging) {
            // Set the default logging level (default=Level.INFO, Level.ALL=All Debug Info)
            LogManager.getLogManager().getLogger("").setLevel(Level.ALL);
        } else {
            // Set the default logging level (default=Level.INFO, Level.ALL=All Debug Info)
            LogManager.getLogManager().getLogger("").setLevel(Level.INFO);
        }

        super.onCreate(savedInstanceState);
    }
}
