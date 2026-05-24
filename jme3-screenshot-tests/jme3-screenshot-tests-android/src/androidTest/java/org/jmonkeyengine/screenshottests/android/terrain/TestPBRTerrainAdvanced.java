package org.jmonkeyengine.screenshottests.android.terrain;

import org.jmonkeyengine.screenshottests.android.android.ScreenshotTestAndroidBase;
import org.jmonkeyengine.screenshottests.scenarios.terrain.ScenarioPBRTerrainAdvanced;
import org.jmonkeyengine.screenshottests.testframework.AndroidRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class TestPBRTerrainAdvanced extends ScreenshotTestAndroidBase {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "FinalRender", 0 },
                { "AmbientOcclusion", 4 },
                { "Emissive", 5 }
        });
    }

    private String testName;
    private int debugMode;

    public TestPBRTerrainAdvanced(String testName, int debugMode) {
        this.testName = testName;
        this.debugMode = debugMode;
    }

    @Test
    public void testPBRTerrainAdvanced() {
        String imageName = getClass().getName() + ".testPBRTerrainAdvanced_" + testName;
        ScenarioPBRTerrainAdvanced.testPBRTerrainAdvanced(debugMode)
                .setBaseImageFileName(imageName)
                .run(new AndroidRunner());
    }
}
