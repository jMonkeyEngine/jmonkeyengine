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

import com.jme3.app.state.AppState;
import com.jme3.system.AppSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This is how a test is configured and started. It uses a fluent API.
 *
 * @author Richard Tingle (aka richtea)
 */
public class ScreenshotTest{

    TestType testType = TestType.MUST_PASS;

    AppState[] states;

    List<Integer> framesToTakeScreenshotsOn = new ArrayList<>();

    TestResolution resolution = new TestResolution(500, 400);

    String baseImageFileName = null;

    public ScreenshotTest(AppState... initialStates){
        states = initialStates;
        framesToTakeScreenshotsOn.add(1); //default behaviour is to take a screenshot on the first frame
    }

    /**
     * Sets the frames to take screenshots on. Frames are at a hard coded 60 FPS (from JME's perspective, clock time may vary).
     */
    public ScreenshotTest setFramesToTakeScreenshotsOn(Integer... frames){
        framesToTakeScreenshotsOn.clear();
        framesToTakeScreenshotsOn.addAll(Arrays.asList(frames));
        return this;
    }

    /**
     * Sets the test type (i.e. what the pass/fail rules are for the test
     */
    public ScreenshotTest setTestType(TestType testType){
        this.testType = testType;
        return this;
    }

    public ScreenshotTest setTestResolution(TestResolution resolution){
        this.resolution = resolution;
        return this;
    }

    /**
     * Sets the file name to be used (as the first part) of saved images in both the resources directory and
     * the generated image. Note that you only have to call this if you want to override the default behaviour which is
     * to use the calling class and method name, like org.jmonkeyengine.screenshottests.water.TestPostWater.testPostWater
     */
    public ScreenshotTest setBaseImageFileName(String baseImageFileName){
        this.baseImageFileName = baseImageFileName;
        return this;
    }

    public void run(){
        AppSettings settings = new AppSettings(true);
        settings.setResolution(resolution.getWidth(), resolution.getHeight());
        settings.setAudioRenderer(null); // Disable audio (for headless)
        settings.setUseInput(false); //while it will run with inputs on it causes non-fatal errors.

        String imageFilePrefix = baseImageFileName == null ? calculateImageFilePrefix() : baseImageFileName;

        TestDriver.bootAppForTest(testType,settings,imageFilePrefix, framesToTakeScreenshotsOn, states);
    }


    private String calculateImageFilePrefix(){
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // The element at index 2 is the caller of this method, so at 3 should be the test class
        if (stackTrace.length > 3) {
            StackTraceElement caller = stackTrace[3];
            return caller.getClassName() + "." + caller.getMethodName();
        } else {
            throw new RuntimeException("Caller information is not available.");
        }
    }


}
