package org.jmonkeyengine.screenshottests.android.android;

import androidx.test.rule.GrantPermissionRule;

import org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase;
import org.junit.Rule;

public abstract class ScreenshotTestAndroidBase extends ScreenshotTestBase {

    @Rule
    public ExtentReportExtensionJunit4 extentReportExtension = new ExtentReportExtensionJunit4();
}
