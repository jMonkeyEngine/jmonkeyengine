package com.jmonkeyengine.tests;

import android.content.Intent;
import com.jme3.app.AndroidHarness;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import com.jme3.system.android.AndroidConfigChooser.ConfigType;

public class TestsHarness extends AndroidHarness{

    /*
     * Note that you can ignore the errors displayed in this file,
     * the android project will build regardless.
     * Install the 'Android' plugin under Tools->Plugins->Available Plugins
     * to get error checks and code completion for the Android project files.
     */

    public TestsHarness(){
        // Set the application class to run
        appClass = "mygame.Main";
        // Try ConfigType.FASTEST; or ConfigType.LEGACY if you have problems
        eglConfigType = ConfigType.BEST;
        // Exit Dialog title & message
        exitDialogTitle = "Exit?";
        exitDialogMessage = "Press Yes";
        // Enable verbose logging
        eglConfigVerboseLogging = false;
        // Choose screen orientation
        // This test project also set the Activity to Landscape in the AndroidManifest.xml
        // If you modify this, also modify AndroidManifest.xml
        screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        // Invert the MouseEvents X (default = true)
        mouseEventsInvertX = true;
        // Invert the MouseEvents Y (default = true)
        mouseEventsInvertY = true;
        // Add splash screen drawable resource
        splashPicID = R.drawable.monkey256_9;
        // Simulate a joystick with Android device orientation data (default = false)
        joystickEventsEnabled = false;
        // Simulate mouse events with Android touch input (default = true)
        mouseEventsEnabled = true;
        mouseEventsInvertX = false;
        mouseEventsInvertY = false;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        appClass = intent.getStringExtra(MainActivity.SELECTED_APP_CLASS);
        mouseEventsEnabled = intent.getBooleanExtra(MainActivity.ENABLE_MOUSE_EVENTS, mouseEventsEnabled);
        joystickEventsEnabled = intent.getBooleanExtra(MainActivity.ENABLE_JOYSTICK_EVENTS, joystickEventsEnabled);

        super.onCreate(savedInstanceState);
    }

}
