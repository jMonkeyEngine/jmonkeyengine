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
package org.jmonkeyengine.screenshottests.testframework.protoreport;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UpgradeProtoReportToExtentReport {
    private static final Logger logger = Logger.getLogger(UpgradeProtoReportToExtentReport.class.getName());

    public static void main(String[] args) {
        if (args.length != 2) {
            throw new RuntimeException("Expecting exactly 2 args; the proto report in path and the extent folder out path");
        }

        File protoInFolderPath = new File(args[0]);
        logger.info("Processing report from " + protoInFolderPath.getAbsolutePath());
        File extentOutFolderPath = new File(args[1]);
        logger.info("Writing report to " + extentOutFolderPath.getAbsolutePath());

        if (!extentOutFolderPath.exists() && !extentOutFolderPath.mkdirs()) {
            throw new RuntimeException("Could not create output folder: " + extentOutFolderPath.getAbsolutePath());
        }

        File jsonFile = new File(protoInFolderPath, "screenshotProtoReport.json");
        if (!jsonFile.exists()) {
            throw new RuntimeException("Could not find screenshotProtoReport.json in " + protoInFolderPath.getAbsolutePath());
        }

        ObjectMapper mapper = new ObjectMapper();
        ProtoReport protoReport;
        try {
            protoReport = mapper.readValue(jsonFile, ProtoReport.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read ProtoReport JSON", e);
        }

        ExtentSparkReporter spark = new ExtentSparkReporter(new File(extentOutFolderPath, "index.html"));
        spark.config().setTheme(Theme.STANDARD);
        spark.config().setDocumentTitle(protoReport.getReportTitle());
        spark.config().setReportName(protoReport.getReportTitle());

        ExtentReports extent = new ExtentReports();
        extent.attachReporter(spark);

        for (ProtoReportTestItem item : protoReport.getTestResults()) {
            ExtentTest test = extent.createTest(item.getClassSimpleName() + "." + item.getMethodName());

            for (String log : item.getLogs()) {
                test.info(log);
            }

            for (ProtoReportTestItem.ReportEvent event : item.getReportEvents()) {
                Status status = convertStatus(event.getStatus());
                test.log(status, event.getMessage());
            }

            for (ProtoReportTestItem.ImageReference imageRef : item.getAttachedImages()) {
                File sourceImage = new File(protoInFolderPath, imageRef.getRelativeFileName());
                File destImage = new File(extentOutFolderPath, imageRef.getRelativeFileName());

                if (sourceImage.exists()) {
                    try {
                        Files.copy(sourceImage.toPath(), destImage.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        test.addScreenCaptureFromPath(imageRef.getRelativeFileName(), imageRef.getTitle());
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Failed to copy image: " + imageRef.getRelativeFileName(), e);
                    }
                } else {
                    logger.warning("Referenced image not found: " + sourceImage.getAbsolutePath());
                }
            }
        }

        extent.flush();
        logger.info("Report generation complete.");
    }

    private static Status convertStatus(ProtoReportTestItem.ReportStatus status) {
        switch (status) {
            case PASSED:
                return Status.PASS;
            case FAILED:
                return Status.FAIL;
            case SKIPPED:
                return Status.SKIP;
            case WARNING:
                return Status.WARNING;
            default:
                return Status.INFO;
        }
    }
}
