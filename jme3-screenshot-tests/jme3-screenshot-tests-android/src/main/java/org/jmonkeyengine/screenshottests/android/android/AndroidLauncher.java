package org.jmonkeyengine.screenshottests.android.android;

import com.jme3.app.AndroidHarness;


public class AndroidLauncher extends AndroidHarness {

    public AndroidLauncher() {
        appClass = JmeAndroidApp.class.getCanonicalName();
        // Enable alpha bits to force the GLSurfaceView into the main view hierarchy
        eglAlphaBits = 8;
    }
}
