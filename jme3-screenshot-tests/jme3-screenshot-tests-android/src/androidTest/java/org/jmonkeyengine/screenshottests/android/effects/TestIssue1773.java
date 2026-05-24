package org.jmonkeyengine.screenshottests.android.effects;

import org.jmonkeyengine.screenshottests.android.android.ScreenshotTestAndroidBase;
import org.jmonkeyengine.screenshottests.scenarios.effects.ScenarioIssue1773;
import org.jmonkeyengine.screenshottests.testframework.AndroidRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class TestIssue1773 extends ScreenshotTestAndroidBase {

    @Parameterized.Parameters(name = "worldSpace={0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { true }, { false }
        });
    }

    private boolean worldSpace;

    public TestIssue1773(boolean worldSpace) {
        this.worldSpace = worldSpace;
    }

    @Test
    public void testIssue1773() {
        String imageName = getClass().getName() + ".testIssue1773" + (worldSpace ? "_worldSpace" : "_localSpace");
        ScenarioIssue1773.testIssue1773(worldSpace)
                .setBaseImageFileName(imageName)
                .run(new AndroidRunner());
    }
}
