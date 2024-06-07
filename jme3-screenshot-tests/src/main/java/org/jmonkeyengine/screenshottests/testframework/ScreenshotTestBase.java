package org.jmonkeyengine.screenshottests.testframework;

import com.jme3.app.state.AppState;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ExtentReportExtension.class)
@Tag("integration")
public abstract class ScreenshotTestBase{

    public ScreenshotTest screenshotTest(AppState... initialStates){
        return new ScreenshotTest(initialStates);
    }
}
