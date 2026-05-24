package org.jmonkeyengine.screenshottests.testframework.desktop;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.jme3.system.JmeSystem;
import com.jme3.texture.Image;

import org.jmonkeyengine.screenshottests.testframework.ExtentReportLogCapture;
import org.jmonkeyengine.screenshottests.testframework.TestReportCaptureBase;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class ExtentReportExtensionJunitJupiter extends TestReportCaptureBase implements BeforeAllCallback, AfterAllCallback, TestWatcher, BeforeTestExecutionCallback {

    private static ExtentReports extent;
    private static ExtentTest currentTest;

    public File reportPath() {
        return new File("build/reports/ScreenshotDiffReport.html");
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        TestReportCaptureBase.INSTANCE = this;
        startReportIfNecessary();
    }

    @Override
    public void afterAll(ExtensionContext context){
        ExtentReportLogCapture.getAndPurgeLogs().forEach(logLine -> {
            if (currentTest != null) {
                currentTest.info(logLine);
            }
        });

        /*
         * this writes the entire report after each test class. This sucks but I don't think there is
         * anywhere else I can hook into the lifecycle of the end of all tests to write the report.
         */
        extent.flush();

        // Restore the original System.out
        ExtentReportLogCapture.restore();
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        currentTest.pass("Test passed");
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        currentTest.fail(cause);
    }

    @Override
    public void markFailInReport(String message) {
        currentTest.fail(message);
    }

    @Override
    public void warning(String logString) {
        currentTest.warning(logString);
    }

    @Override
    public void attachImage(String title, String fileName, Image originalImage) {
        try (FileOutputStream fileOutBuf = new FileOutputStream(reportPath().toPath().resolve(fileName).toFile())) {
            JmeSystem.writeImageFile(fileOutBuf, "png",originalImage.getData(0),originalImage.getWidth(), originalImage.getHeight());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        currentTest.addScreenCaptureFromPath(fileName, title);
    }

    @Override
    public void beforeTestExecution(ExtensionContext context){
        String testName = context.getDisplayName();
        String className = context.getRequiredTestClass().getSimpleName();
        currentTest = extent.createTest(className + "." + testName);
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        currentTest.skip("Test disabled: " + reason.orElse("No reason available"));
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        currentTest.skip("Test aborted " + cause.toString());
    }

    protected void startReportIfNecessary() {
        if(extent==null){
            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath());
            spark.config().setTheme(Theme.STANDARD);
            spark.config().setDocumentTitle("Screenshot Test Report");
            spark.config().setReportName("Screenshot Test Report");
            extent = new ExtentReports();
            extent.attachReporter(spark);
        }
        // Initialize log capture to redirect console output to the report
        ExtentReportLogCapture.initialize();
    }
}
