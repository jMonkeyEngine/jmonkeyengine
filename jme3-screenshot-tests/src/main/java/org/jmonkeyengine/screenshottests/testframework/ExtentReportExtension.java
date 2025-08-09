/*
 * Copyright (c) 2024 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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

import java.util.Optional;

/**
 * This creates the Extent report and manages the test lifecycle
 *
 * @author Richard Tingle (aka richtea)
 */
public class ExtentReportExtension implements BeforeAllCallback, AfterAllCallback, TestWatcher, BeforeTestExecutionCallback{
    private static ExtentReports extent;
    private static ExtentTest currentTest;

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
        // Initialize log capture to redirect console output to the report
        ExtentReportLogCapture.initialize();
    }

    @Override
    public void afterAll(ExtensionContext context) {
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
        currentTest = extent.createTest(testName);
    }

    public static ExtentTest getCurrentTest() {
        return currentTest;
    }
}
