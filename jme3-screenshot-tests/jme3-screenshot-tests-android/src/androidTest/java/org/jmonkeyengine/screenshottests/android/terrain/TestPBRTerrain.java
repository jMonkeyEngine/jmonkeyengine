package org.jmonkeyengine.screenshottests.android.terrain;

import org.jmonkeyengine.screenshottests.android.android.ScreenshotTestAndroidBase;
import org.jmonkeyengine.screenshottests.scenarios.terrain.ScenarioPBRTerrain;
import org.jmonkeyengine.screenshottests.testframework.AndroidRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class TestPBRTerrain extends ScreenshotTestAndroidBase {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "FinalRender", 0 },
                { "NormalMap", 1 },
                { "RoughnessMap", 2 },
                { "MetallicMap", 3 },
                { "GeometryNormals", 8 }
        });
    }

    private String testName;
    private int debugMode;

    public TestPBRTerrain(String testName, int debugMode) {
        this.testName = testName;
        this.debugMode = debugMode;
    }

    @Test
    public void testPBRTerrain() {
        String imageName = getClass().getName() + ".testPBRTerrain_" + testName;
        ScenarioPBRTerrain.testPBRTerrain(debugMode)
                .setBaseImageFileName(imageName)
                .run(new AndroidRunner());
    }
}
