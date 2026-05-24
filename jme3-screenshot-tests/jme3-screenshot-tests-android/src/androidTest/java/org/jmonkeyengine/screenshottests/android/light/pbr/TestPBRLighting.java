package org.jmonkeyengine.screenshottests.android.light.pbr;

import org.jmonkeyengine.screenshottests.android.android.ScreenshotTestAndroidBase;
import org.jmonkeyengine.screenshottests.scenarios.light.pbr.ScenarioPBRLighting;
import org.jmonkeyengine.screenshottests.testframework.AndroidRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class TestPBRLighting extends ScreenshotTestAndroidBase {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "LowRoughness", 0.1f, false },
                { "HighRoughness", 1.0f, false },
                { "DefaultDirectionalLight", 0.5f, false },
                { "UpdatedDirectionalLight", 0.5f, true }
        });
    }

    private String testName;
    private float roughness;
    private boolean updateLight;

    public TestPBRLighting(String testName, float roughness, boolean updateLight) {
        this.testName = testName;
        this.roughness = roughness;
        this.updateLight = updateLight;
    }

    @Test
    public void testPBRLighting() {
        String imageName = getClass().getName() + ".testPBRLighting_" + testName;
        ScenarioPBRLighting.testPBRLighting(roughness, updateLight)
                .setBaseImageFileName(imageName)
                .run(new AndroidRunner());
    }
}
