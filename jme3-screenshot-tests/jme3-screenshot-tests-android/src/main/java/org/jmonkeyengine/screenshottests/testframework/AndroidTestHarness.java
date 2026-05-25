package org.jmonkeyengine.screenshottests.testframework;

import android.opengl.GLSurfaceView;

import com.jme3.app.AndroidHarnessFragment;
import com.jme3.app.LegacyApplication;
import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AndroidTestHarness extends AndroidHarnessFragment {

    private static final Logger logger = Logger.getLogger(AndroidTestHarness.class.getName());

    private final SimpleApplication application;
    private final CountDownLatch applicationFinishedLatch;

    public AndroidTestHarness(SimpleApplication application, CountDownLatch applicationFinishedLatch) {
        this.application = application;
        this.applicationFinishedLatch = applicationFinishedLatch;
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

    @Override
    public void handleError(String errorMsg, Throwable throwable) {
        logger.log(Level.WARNING, "Error in test application", throwable);
        applicationFinishedLatch.countDown();

        super.handleError(errorMsg, throwable);
    }
}
