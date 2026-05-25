package org.jmonkeyengine.screenshottests.android.post;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.jmonkeyengine.screenshottests.android.android.ScreenshotTestAndroidBase;
import org.jmonkeyengine.screenshottests.scenarios.post.ScenarioFog;
import org.jmonkeyengine.screenshottests.testframework.AndroidRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TestFog extends ScreenshotTestAndroidBase {

    @Test
    public void testFog() {
        ScenarioFog.testFog().run(new AndroidRunner());
    }
}
