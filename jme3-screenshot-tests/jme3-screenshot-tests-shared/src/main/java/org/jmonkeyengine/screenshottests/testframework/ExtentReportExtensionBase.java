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

import java.io.File;
import java.util.Optional;

/**
 * This creates the Extent report and manages the test lifecycle
 *
 * @author Richard Tingle (aka richtea)
 */
public abstract class ExtentReportExtensionBase{
    private static ExtentReports extent;
    private static ExtentTest currentTest;

    public abstract File reportPath();

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

    public void completeReport() {
        /*
        * this writes the entire report after each test class. This sucks but I don't think there is
        * anywhere else I can hook into the lifecycle of the end of all tests to write the report.
        */
        extent.flush();

        // Restore the original System.out
        ExtentReportLogCapture.restore();
    }

    public void testSuccessful() {
        getCurrentTest().pass("Test passed");
    }

    public void testFailed(Throwable cause) {
        getCurrentTest().fail(cause);
    }

    public void testAborted(Throwable cause) {
        getCurrentTest().skip("Test aborted " + cause.toString());
    }

    public void testDisabled(Optional<String> reason) {
        getCurrentTest().skip("Test disabled: " + reason.orElse("No reason"));
    }

    public void setUpForTest(String testName, String className){
        currentTest = extent.createTest(className + "." + testName);
    }

    public static ExtentTest getCurrentTest() {
        return currentTest;
    }
}
