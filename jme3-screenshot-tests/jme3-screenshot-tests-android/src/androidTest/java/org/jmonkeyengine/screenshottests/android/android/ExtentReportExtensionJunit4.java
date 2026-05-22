package org.jmonkeyengine.screenshottests.android.android;

import androidx.test.platform.app.InstrumentationRegistry;

import org.jmonkeyengine.screenshottests.testframework.ExtentReportExtensionBase;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;

public class ExtentReportExtensionJunit4 extends ExtentReportExtensionBase implements org.junit.rules.TestRule {

    private final TestWatcher watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            startReportIfNecessary();
            setUpForTest(description.getMethodName(), description.getTestClass().getSimpleName());
        }

        @Override
        protected void succeeded(Description description) {
            testSuccessful();
        }

        @Override
        protected void failed(Throwable e, Description description) {
            testFailed(e);
        }

        @Override
        protected void skipped(org.junit.AssumptionViolatedException e, Description description) {
            testAborted(e);
        }

        @Override
        protected void finished(Description description) {
            completeReport();
        }
    };

    @Override
    public File reportPath() {
        File externalFilesDir = InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null);
        return new File(externalFilesDir, "/report/ScreenshotDiffReport.html");
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return watcher.apply(base, description);
    }
}
