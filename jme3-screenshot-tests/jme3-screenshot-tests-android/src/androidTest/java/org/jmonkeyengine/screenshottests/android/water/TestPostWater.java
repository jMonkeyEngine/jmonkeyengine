package org.jmonkeyengine.screenshottests.android.water;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.jmonkeyengine.screenshottests.android.android.ScreenshotTestAndroidBase;
import org.jmonkeyengine.screenshottests.scenarios.water.ScenarioPostWater;
import org.jmonkeyengine.screenshottests.testframework.AndroidRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TestPostWater extends ScreenshotTestAndroidBase {

    /**
     * This test creates a scene with a terrain and post process water filter.
     */
    @Test
    public void testPostWater() {
        ScenarioPostWater.testPostWater().run(new AndroidRunner());
    }
}
