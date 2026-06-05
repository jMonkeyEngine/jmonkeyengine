/*
 * Copyright (c) 2026 jMonkeyEngine
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
package org.jmonkeyengine.screenshottests.android.android;

import androidx.test.services.storage.TestStorage;

import com.jme3.system.JmeSystem;
import com.jme3.texture.Image;

import org.jmonkeyengine.screenshottests.testframework.ExtentReportLogCapture;
import org.jmonkeyengine.screenshottests.testframework.TestReportCaptureBase;
import org.jmonkeyengine.screenshottests.testframework.protoreport.ProtoReport;
import org.jmonkeyengine.screenshottests.testframework.protoreport.ProtoReportTestItem;
import org.junit.AssumptionViolatedException;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import tools.jackson.databind.ObjectMapper;

public class ExtentReportExtensionJunit4 extends TestReportCaptureBase implements org.junit.rules.TestRule {

    private static final String REPORT_DIRECTORY = "report";

    static ProtoReport report = new ProtoReport("Screenshot Test Report - Android");
    ProtoReportTestItem testInProgress;
    TestStorage testStorage = new TestStorage();

    private final TestWatcher watcher = new TestWatcher() {

        @Override
        protected void starting(Description description) {
            ExtentReportLogCapture.initialize();
            testInProgress = new ProtoReportTestItem(description.getMethodName(), description.getTestClass().getSimpleName());
        }

        @Override
        protected void succeeded(Description description) {
            testInProgress.addStatus(ProtoReportTestItem.ReportStatus.PASSED, "Test passed");
        }

        @Override
        protected void failed(Throwable e, Description description) {
            testInProgress.addStatus(ProtoReportTestItem.ReportStatus.FAILED, getStackTraceAsString(e));
        }

        @Override
        protected void skipped(AssumptionViolatedException e, Description description) {
            testInProgress.addStatus(ProtoReportTestItem.ReportStatus.SKIPPED, e.getLocalizedMessage());
        }

        @Override
        protected void finished(Description description) {
            testInProgress.addLogs(ExtentReportLogCapture.getAndPurgeLogs());
            report.addTest(testInProgress);
            ExtentReportLogCapture.restore();
            testInProgress = null;
            persistReport(); // it sucks that we do this every test
        }
    };

    @Override
    public Statement apply(Statement base, Description description) {
        TestReportCaptureBase.INSTANCE = this;
        return watcher.apply(base, description);
    }

    public static String getStackTraceAsString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    @Override
    public void markFailInReport(String message) {
        testInProgress.addStatus(ProtoReportTestItem.ReportStatus.FAILED, message);
    }

    @Override
    public void warning(String message) {
        testInProgress.addStatus(ProtoReportTestItem.ReportStatus.WARNING, message);
    }

    public OutputStream getPersistentFileOutputStream(String relativePath){
        try{
            return testStorage.openOutputFile(relativePath);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void persistReport(){
        ObjectMapper mapper = new ObjectMapper();

        String reportJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);

        try (OutputStream out = getPersistentFileOutputStream(REPORT_DIRECTORY + "/screenshotProtoReport.json")) {
            out.write(reportJson.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void attachImage(String title, String fileName, Image image) {
        try (OutputStream out = getPersistentFileOutputStream(REPORT_DIRECTORY + "/" + fileName)) {
            JmeSystem.writeImageFile(out, "png",image.getData(0), image.getWidth(), image.getHeight());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        testInProgress.addImageReference(title, fileName);
    }
}
