package org.jmonkeyengine.screenshottests.android.effects;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.jmonkeyengine.screenshottests.android.android.ScreenshotTestAndroidBase;
import org.jmonkeyengine.screenshottests.scenarios.effects.ScenarioExplosionEffect;
import org.jmonkeyengine.screenshottests.testframework.AndroidRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TestExplosionEffect extends ScreenshotTestAndroidBase {

    /**
     * This test's particle effects (using an explosion)
     */
    @Test
    public void testExplosionEffect() {
        ScenarioExplosionEffect.testExplosionEffect().run(new AndroidRunner());
    }
}
