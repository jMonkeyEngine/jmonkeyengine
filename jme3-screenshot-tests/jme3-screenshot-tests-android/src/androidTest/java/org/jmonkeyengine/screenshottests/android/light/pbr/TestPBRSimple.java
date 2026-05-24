package org.jmonkeyengine.screenshottests.android.light.pbr;

import org.jmonkeyengine.screenshottests.android.android.ScreenshotTestAndroidBase;
import org.jmonkeyengine.screenshottests.scenarios.light.pbr.ScenarioPBRSimple;
import org.jmonkeyengine.screenshottests.testframework.AndroidRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class TestPBRSimple extends ScreenshotTestAndroidBase {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "WithRealtimeBaking", true },
                { "WithoutRealtimeBaking", false }
        });
    }

    private String testName;
    private boolean realtimeBaking;

    public TestPBRSimple(String testName, boolean realtimeBaking) {
        this.testName = testName;
        this.realtimeBaking = realtimeBaking;
    }

    @Test
    public void testPBRSimple() {
        String imageName = getClass().getName() + ".testPBRSimple_" + testName;
        ScenarioPBRSimple.testPBRSimple(realtimeBaking)
                .setBaseImageFileName(imageName)
                .run(new AndroidRunner());
    }
}
