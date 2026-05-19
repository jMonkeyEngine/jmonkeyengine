package org.jmonkeyengine.screenshottests.testframework.desktop;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import org.jmonkeyengine.screenshottests.testframework.ExtentReportExtensionBase;
import org.jmonkeyengine.screenshottests.testframework.ExtentReportLogCapture;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.io.File;
import java.util.Optional;

public class ExtentReportExtensionJunitJupiter extends ExtentReportExtensionBase implements BeforeAllCallback, AfterAllCallback, TestWatcher, BeforeTestExecutionCallback {

    @Override
    public File reportPath() {
        return new File("build/reports/ScreenshotDiffReport.html");
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        startReportIfNecessary();
    }

    @Override
    public void afterAll(ExtensionContext context){
        completeReport();
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        testSuccessful();
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        testFailed(cause);
    }

    @Override
    public void beforeTestExecution(ExtensionContext context){
        String testName = context.getDisplayName();
        String className = context.getRequiredTestClass().getSimpleName();
        setUpForTest(testName, className);
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        testDisabled(reason);
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        testAborted(cause);
    }
}
