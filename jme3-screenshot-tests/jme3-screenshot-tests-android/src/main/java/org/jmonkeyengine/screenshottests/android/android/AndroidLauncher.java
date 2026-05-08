package org.jmonkeyengine.screenshottests.android.android;

import com.jme3.app.AndroidHarness;


public class AndroidLauncher extends AndroidHarness {

    public AndroidLauncher() {
        appClass = JmeAndroidApp.class.getCanonicalName();
    }
}
