package org.jmonkeyengine.screenshottests.android.model.shape;

import com.jme3.math.Vector3f;
import org.jmonkeyengine.screenshottests.android.android.ScreenshotTestAndroidBase;
import org.jmonkeyengine.screenshottests.scenarios.model.shape.ScenarioBillboard;
import org.jmonkeyengine.screenshottests.testframework.AndroidRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class TestBillboard extends ScreenshotTestAndroidBase {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "fromFront", new Vector3f(0, 1, 15) },
                { "fromAbove", new Vector3f(0, 15, 6) },
                { "fromRight", new Vector3f(-15, 10, 5) }
        });
    }

    private String testName;
    private Vector3f cameraPosition;

    public TestBillboard(String testName, Vector3f cameraPosition) {
        this.testName = testName;
        this.cameraPosition = cameraPosition;
    }

    @Test
    public void testBillboard() {
        String imageName = getClass().getName() + ".testBillboard_" + testName;
        ScenarioBillboard.testBillboard(cameraPosition)
                .setBaseImageFileName(imageName)
                .run(new AndroidRunner());
    }
}
