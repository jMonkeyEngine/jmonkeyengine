package org.jmonkeyengine.screenshottests.testframework;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;


public class ExtentReportExtension implements BeforeAllCallback, AfterAllCallback, TestWatcher, BeforeTestExecutionCallback{
    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> test = new ThreadLocal<>();

    @Override
    public void beforeAll(ExtensionContext context) {
        if(extent==null){
            ExtentSparkReporter spark = new ExtentSparkReporter("build/reports/ScreenshotDiffReport.html");
            spark.config().setTheme(Theme.STANDARD);
            spark.config().setDocumentTitle("Screenshot Test Report");
            spark.config().setReportName("Screenshot Test Report");
            extent = new ExtentReports();
            extent.attachReporter(spark);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        extent.flush();
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        getCurrentTest().pass("Test passed");
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        getCurrentTest().fail(cause);
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        getCurrentTest().skip("Test aborted " + cause.toString());
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        getCurrentTest().skip("Test disabled: " + reason.orElse("No reason"));
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        String testName = context.getDisplayName();
        test.set(extent.createTest(testName));
    }

    public static ExtentTest getCurrentTest() {
        return test.get();
    }
}