package org.jmonkeyengine.screenshottests.testframework;

import com.jme3.app.state.AppState;
import com.jme3.system.AppSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        settings.setUseInput(false);

        String imageFilePrefix = baseImageFileName == null ? calculateImageFilePrefix() : baseImageFileName;

        TestDriver.bootAppForTest(testType,settings,imageFilePrefix, framesToTakeScreenshotsOn, states);
    }


    private String calculateImageFilePrefix(){
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // The element at index 2 is the caller of this method
        // (0 is getStackTrace, 1 is getCallerInfo, 2 is the caller of getCallerInfo etc)
        if (stackTrace.length > 3) {
            StackTraceElement caller = stackTrace[3];
            return caller.getClassName() + "." + caller.getMethodName();
        } else {
            throw new RuntimeException("Caller information is not available.");
        }
    }


}
