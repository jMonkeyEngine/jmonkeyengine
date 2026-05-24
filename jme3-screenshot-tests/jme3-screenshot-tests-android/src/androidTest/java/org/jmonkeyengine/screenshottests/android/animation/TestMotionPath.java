package org.jmonkeyengine.screenshottests.android.animation;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.jmonkeyengine.screenshottests.android.android.ScreenshotTestAndroidBase;
import org.jmonkeyengine.screenshottests.scenarios.animation.ScenarioMotionPath;
import org.jmonkeyengine.screenshottests.testframework.AndroidRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TestMotionPath extends ScreenshotTestAndroidBase {

    /**
     * This test creates a scene with a teapot following a motion path.
     */
    @Test
    public void testMotionPath() {
        ScenarioMotionPath.testMotionPath().run(new AndroidRunner());
    }
}
