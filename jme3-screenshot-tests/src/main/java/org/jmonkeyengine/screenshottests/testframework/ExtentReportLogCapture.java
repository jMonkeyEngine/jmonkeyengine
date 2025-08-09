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

import com.aventstack.extentreports.ExtentTest;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * This class captures console logs and adds them to the ExtentReport.
 * It redirects System.out to both the original console and the ExtentReport.
 *
 * @author Richard Tingle (aka richtea)
 */
public class ExtentReportLogCapture {

    private static final PrintStream originalOut = System.out;
    private static final PrintStream originalErr = System.err;
    private static boolean initialized = false;

    /**
     * Initializes the log capture system. This should be called once at the start of the test suite.
     */
    public static void initialize() {
        if (!initialized) {
            // Redirect System.out and System.err
            System.setOut(new ExtentReportPrintStream(originalOut));
            System.setErr(new ExtentReportPrintStream(originalErr));

            initialized = true;
        }
    }

    /**
     * Restores the original System.out. This should be called at the end of the test suite.
     */
    public static void restore() {
        if(initialized) {
            // Restore System.out and System.err
            System.setOut(originalOut);
            System.setErr(originalErr);
            initialized = false;
        }
    }

    /**
     * A custom PrintStream that redirects output to both the original console and the ExtentReport.
     */
    private static class ExtentReportPrintStream extends PrintStream {
        private StringBuilder buffer = new StringBuilder();

        public ExtentReportPrintStream(OutputStream out) {
            super(out, true);
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            super.write(buf, off, len);

            // Convert the byte array to a string and add to buffer
            String s = new String(buf, off, len);
            buffer.append(s);

            // If we have a complete line (ends with newline), process it
            if (s.endsWith("\n") || s.endsWith("\r\n")) {
                String line = buffer.toString().trim();
                if (!line.isEmpty()) {
                    addToExtentReport(line);
                }
                buffer.setLength(0); // Clear the buffer
            }
        }

        private void addToExtentReport(String s) {
            try {
                ExtentTest currentTest = ExtentReportExtension.getCurrentTest();
                if (currentTest != null) {
                    currentTest.info(s);
                }
            } catch (Exception e) {
                // If there's an error adding to the report, just continue
                // This ensures that console logs are still displayed even if there's an issue with the report
            }
        }
    }

}
