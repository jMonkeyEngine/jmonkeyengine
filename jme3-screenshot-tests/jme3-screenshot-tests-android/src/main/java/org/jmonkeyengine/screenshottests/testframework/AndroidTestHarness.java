package org.jmonkeyengine.screenshottests.testframework;

import android.opengl.GLSurfaceView;

import com.jme3.app.AndroidHarnessFragment;
import com.jme3.app.LegacyApplication;
import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;

public class AndroidTestHarness extends AndroidHarnessFragment {

    SimpleApplication application;

    public AndroidTestHarness(SimpleApplication application) {
        this.application = application;
    }

    @Override
    protected LegacyApplication createApplication(){
        return application;
    }

    @Override
    protected void configureSettings(AppSettings settings) {
        super.configureSettings(settings);
        settings.setAudioRenderer(null);
    }

    public GLSurfaceView getGLSurfaceView() {
        return this.view;
    }
}
