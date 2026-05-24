package org.jmonkeyengine.screenshottests.android.animation;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.jmonkeyengine.screenshottests.android.android.ScreenshotTestAndroidBase;
import org.jmonkeyengine.screenshottests.scenarios.animation.ScenarioIssue2076;
import org.jmonkeyengine.screenshottests.testframework.AndroidRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TestIssue2076 extends ScreenshotTestAndroidBase {

    /**
     * This test creates a scene with two Jaime models, one using the old animation system
     * and one using the new animation system, both with software skinning and no vertex normals.
     */
    @Test
    public void testIssue2076() {
        ScenarioIssue2076.testIssue2076().run(new AndroidRunner());
    }
}