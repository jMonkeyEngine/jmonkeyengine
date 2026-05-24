package org.jmonkeyengine.screenshottests.android.scene.instancing;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.jmonkeyengine.screenshottests.android.android.ScreenshotTestAndroidBase;
import org.jmonkeyengine.screenshottests.scenarios.scene.instancing.ScenarioInstanceNodeWithPbr;
import org.jmonkeyengine.screenshottests.testframework.AndroidRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TestInstanceNodeWithPbr extends ScreenshotTestAndroidBase {

    /**
     * This test specifically validates the corrected PBR rendering when combined
     * with instancing, as addressed in issue #2435.
     */
    @Test
    public void testInstanceNodeWithPbr() {
        ScenarioInstanceNodeWithPbr.testInstanceNodeWithPbr().run(new AndroidRunner());
    }
}
