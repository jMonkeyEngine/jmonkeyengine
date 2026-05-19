package org.jmonkeyengine.screenshottests.testframework.desktop;

import org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ExtentReportExtensionJunitJupiter.class)
@Tag("integration")
public abstract class ScreenshotTestDesktopBase extends ScreenshotTestBase {
}
