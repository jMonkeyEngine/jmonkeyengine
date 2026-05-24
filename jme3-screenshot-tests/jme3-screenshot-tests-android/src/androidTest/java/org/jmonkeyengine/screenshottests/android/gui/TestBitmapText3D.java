package org.jmonkeyengine.screenshottests.android.gui;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.jmonkeyengine.screenshottests.android.android.ScreenshotTestAndroidBase;
import org.jmonkeyengine.screenshottests.scenarios.gui.ScenarioBitmapText3D;
import org.jmonkeyengine.screenshottests.testframework.AndroidRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TestBitmapText3D extends ScreenshotTestAndroidBase {

    /**
     * This tests both that bitmap text is rendered correctly and that it is
     * wrapped correctly.
     */
    @Test
    public void testBitmapText3D() {
        ScenarioBitmapText3D.testBitmapText3D().run(new AndroidRunner());
    }
}
