package org.jmonkeyengine.jme3androidexamples;

import android.os.Bundle;
import com.jme3.app.AndroidHarnessFragment;
import com.jme3.app.LegacyApplication;
import com.jme3.system.AppSettings;
import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 * A placeholder fragment containing a jME GLSurfaceView.
 */
public class JmeFragment extends AndroidHarnessFragment {
    private String appClass;
    private boolean joystickEventsEnabled;
    private boolean keyEventsEnabled = true;
    private boolean mouseEventsEnabled = true;

    public JmeFragment() {
        finishOnAppStop = true;
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

    @Override
    protected LegacyApplication createApplication() throws Exception {
        Class<?> clazz = Class.forName(appClass);
        return (LegacyApplication) clazz.getDeclaredConstructor().newInstance();
    }

}
