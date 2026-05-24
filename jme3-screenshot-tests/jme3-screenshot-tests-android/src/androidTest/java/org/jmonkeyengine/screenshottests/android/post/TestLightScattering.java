package org.jmonkeyengine.screenshottests.android.post;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.jmonkeyengine.screenshottests.android.android.ScreenshotTestAndroidBase;
import org.jmonkeyengine.screenshottests.scenarios.post.ScenarioLightScattering;
import org.jmonkeyengine.screenshottests.testframework.AndroidRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TestLightScattering extends ScreenshotTestAndroidBase {

    /**
     * This test creates a scene with a light scattering effect.
     */
    @Test
    public void testLightScattering() {
        ScenarioLightScattering.testLightScattering().run(new AndroidRunner());
    }
}
