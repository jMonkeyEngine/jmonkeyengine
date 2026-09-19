package org.jmonkeyengine.jme3androidexamples;

import android.os.Bundle;
import com.jme3.app.AndroidHarness;

/**
 * Example and verification Activity extending the compatibility AndroidHarness.
 */
public class TestHarnessActivity extends AndroidHarness {

    public TestHarnessActivity() {
        appClass = "jme3test.android.TestAndroidSensors";
        eglBitsPerPixel = 24;
        eglAlphaBits = 0;
        eglDepthBits = 24;
        eglSamples = 0;
        eglStencilBits = 8;
        frameRate = 60;
        mouseEventsEnabled = true;
        keyEventsEnabled = true;
        joystickEventsEnabled = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.containsKey(MainActivity.SELECTED_APP_CLASS)) {
            appClass = bundle.getString(MainActivity.SELECTED_APP_CLASS);
        }
        super.onCreate(savedInstanceState);
    }
}
