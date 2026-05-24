package org.jmonkeyengine.screenshottests.android.export;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.jmonkeyengine.screenshottests.android.android.ScreenshotTestAndroidBase;
import org.jmonkeyengine.screenshottests.scenarios.export.ScenarioOgreConvert;
import org.jmonkeyengine.screenshottests.testframework.AndroidRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TestOgreConvert extends ScreenshotTestAndroidBase {

    /**
     * This tests loads an Ogre model, converts it to binary, and then reloads it.
     */
    @Test
    public void testOgreConvert() {
        ScenarioOgreConvert.testOgreConvert().run(new AndroidRunner());
    }
}
