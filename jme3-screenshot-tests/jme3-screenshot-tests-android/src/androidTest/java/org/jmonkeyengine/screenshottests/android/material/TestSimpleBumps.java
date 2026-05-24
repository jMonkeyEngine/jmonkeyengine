package org.jmonkeyengine.screenshottests.android.material;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.jmonkeyengine.screenshottests.android.android.ScreenshotTestAndroidBase;
import org.jmonkeyengine.screenshottests.scenarios.material.ScenarioSimpleBumps;
import org.jmonkeyengine.screenshottests.testframework.AndroidRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TestSimpleBumps extends ScreenshotTestAndroidBase {

    /**
     * This test creates a scene with a bump-mapped quad and an orbiting light.
     */
    @Test
    public void testSimpleBumps() {
        ScenarioSimpleBumps.testSimpleBumps().run(new AndroidRunner());
    }
}
