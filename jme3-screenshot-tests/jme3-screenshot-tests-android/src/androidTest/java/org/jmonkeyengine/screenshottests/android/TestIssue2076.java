package org.jmonkeyengine.screenshottests.android;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.jmonkeyengine.screenshottests.scenarios.animation.ScenarioIssue2076;
import org.jmonkeyengine.screenshottests.testframework.AndroidRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TestIssue2076{

    // Storage permissions are not needed for getExternalFilesDir() on modern Android
    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE);

    /**
     * This test creates a scene with two Jaime models, one using the old animation system
     * and one using the new animation system, both with software skinning and no vertex normals.
     */
    @Test
    public void testIssue2076() {
        ScenarioIssue2076.testIssue2076().run(new AndroidRunner());
    }
}