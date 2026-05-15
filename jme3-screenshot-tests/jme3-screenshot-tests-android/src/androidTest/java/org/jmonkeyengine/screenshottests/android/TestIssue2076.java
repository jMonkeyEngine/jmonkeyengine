package org.jmonkeyengine.screenshottests.android;

import org.jmonkeyengine.screenshottests.scenarios.animation.ScenarioIssue2076;
import org.jmonkeyengine.screenshottests.testframework.AndroidRunner;
import org.junit.Test;

public class TestIssue2076{

    /**
     * This test creates a scene with two Jaime models, one using the old animation system
     * and one using the new animation system, both with software skinning and no vertex normals.
     */
    @Test
    public void testIssue2076() {
        ScenarioIssue2076.testIssue2076().run(new AndroidRunner());
    }
}