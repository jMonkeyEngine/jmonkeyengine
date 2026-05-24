package org.jmonkeyengine.screenshottests.android.android;

import androidx.test.rule.GrantPermissionRule;

import org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase;
import org.junit.Rule;

public abstract class ScreenshotTestAndroidBase extends ScreenshotTestBase {

    @Rule
    public ExtentReportExtensionJunit4 extentReportExtension = new ExtentReportExtensionJunit4();

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE);

}
