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

import static org.junit.jupiter.api.Assertions.fail;

import com.jme3.app.state.AppState;
import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.math.FastMath;
import com.jme3.system.AppSettings;
import com.jme3.texture.Image;
import com.jme3.texture.plugins.StbImageLoader;

import org.jmonkeyengine.screenshottests.image.ImagePixelWrapper;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

/**
 * This is how a test is configured and started. It uses a fluent API.
 *
 * @author Richard Tingle (aka richtea)
 */
public class ScreenshotTest{

    public static final String IMAGES_ARE_DIFFERENT = "Generated images is different from committed image. (If you are running the test locally this is expected, images only reproducible on github CI infrastructure)";

    public static final String IMAGES_ARE_DIFFERENT_BETWEEN_SCENARIOS = "Images are different between scenarios.";

    public static final String IMAGES_ARE_DIFFERENT_SIZES = "Images are different sizes.";

    public static final String KNOWN_BAD_TEST_IMAGES_DIFFERENT = "Images are different. This is a known broken test.";

    public static final String KNOWN_BAD_TEST_IMAGES_SAME = "This is (or was?) a known broken test but it is now passing, please change the test type to MUST_PASS.";

    public static final String NON_DETERMINISTIC_TEST = "This is a non deterministic test, please manually review the expected and actual images to make sure they are approximately the same.";


    private static final Logger logger = Logger.getLogger(ScreenshotTest.class.getName());


    TestType testType = TestType.MUST_PASS;

    /**
     * Usually there will be a single scenario but sometimes it will be desirable to test that two ways
     * of doing something produce the same result. In that case there will be multiple scenarios.
     */
    List<Scenario> scenarios = new ArrayList<>();

    List<Integer> framesToTakeScreenshotsOn = new ArrayList<>();

    TestResolution resolution = new TestResolution(500, 400);

    String baseImageFileName = null;

    private AppRunner osSpecificRunner;

    public ScreenshotTest(AppState... initialStates){
        scenarios.add(new Scenario("SimpleSingleScenario", initialStates));
        framesToTakeScreenshotsOn.add(1); //default behaviour is to take a screenshot on the first frame
    }
    public ScreenshotTest(Scenario... scenarios){
        this.scenarios.addAll(Arrays.asList(scenarios));
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

    public void run(AppRunner osSpecificRunner){
        this.osSpecificRunner = osSpecificRunner;
        AppSettings settings = new AppSettings(true);
        settings.setResolution(resolution.getWidth(), resolution.getHeight());
        settings.setAudioRenderer(null); // Disable audio (for headless)
        settings.setUseInput(false); //while it will run with inputs on it causes non-fatal errors.
        settings.setRenderer(AppSettings.LWJGL_OPENGL45);

        String imageFilePrefix = baseImageFileName == null ? calculateImageFilePrefix() : baseImageFileName;

        bootAppForTest(testType,settings,imageFilePrefix, framesToTakeScreenshotsOn, scenarios, osSpecificRunner);
    }

    /**
     * Boots up the application on a separate thread (blocks this thread) and then does the following:
     * - Takes screenshots on the requested frames
     * - After all the frames have been taken it stops the application
     * - Compares the screenshot to the expected screenshot (if any). Fails the test if they are different
     */
    private void bootAppForTest(TestType testType, AppSettings appSettings, String baseImageFileName, List<Integer> framesToTakeScreenshotsOn, List<Scenario> scenarios, AppRunner osSpecificRunner){

        Collections.sort(framesToTakeScreenshotsOn);
        ScenarioScreenshotRecorder overallScreenshots = new ScenarioScreenshotRecorder();
        // usually there is a single scenario, but the framework can be set up to expect multiple scenarios that give identical results
        for(Scenario scenario : scenarios) {
            FastMath.rand.setSeed(0); //try to make things deterministic by setting the random seed


            List<AppState> states = new ArrayList<>(Arrays.asList(scenario.states));
            TestDriver testDriver = new TestDriver(scenario.scenarioName, framesToTakeScreenshotsOn);
            states.add(testDriver);

            TestContainingApp app = new TestContainingApp(states.toArray(new AppState[0]));
            app.setSettings(appSettings);
            app.setShowSettings(false);
            testDriver.waitLatch = new CountDownLatch(1);

            osSpecificRunner.runApplicationUntilScenarioCompletes(app, testDriver.waitLatch);

            overallScreenshots.addAll(testDriver.screenshotsAtFrames);
        }
        String failureMessage = null;

        try {
            String primeScenarioName = scenarios.get(0).scenarioName;
            if(scenarios.size()>1){

                // check each scenario gave the same results (before checking a single scenario against the reference images
                for(int i=1;i<scenarios.size();i++){
                    String thisScenarioName = scenarios.get(i).scenarioName;

                    for(int frame : framesToTakeScreenshotsOn) {
                        Path primeGeneratedImagePath = overallScreenshots.getScreenshotsAtFrame(primeScenarioName, frame).orElseGet(() -> osSpecificRunner.fail(
                                "Scenario " + primeScenarioName + " did not take screenshot on frame " + frame
                        ));
                        Path otherGeneratedImagePath = overallScreenshots.getScreenshotsAtFrame(primeScenarioName, frame).orElseGet(() -> osSpecificRunner.fail(
                                "Scenario " + thisScenarioName + " did not take screenshot on frame " + frame
                        ));

                        Image primeGeneratedImage = readImage(primeGeneratedImagePath.toFile());
                        Image otherGeneratedImage = readImage(otherGeneratedImagePath.toFile());

                        String thisFrameBaseImageFileName = baseImageFileName + "_f" + frame;

                        if(!imagesAreSameSize(primeGeneratedImage, otherGeneratedImage)){
                            attachImage("Scenario " + primeScenarioName + " " + frame, thisFrameBaseImageFileName + "_" + primeScenarioName + ".png", primeGeneratedImage);
                            attachImage("Scenario " + thisScenarioName + " " + frame, thisFrameBaseImageFileName + "_" + thisScenarioName + ".png", otherGeneratedImage);
                            TestReportCaptureBase.INSTANCE.warning("Scenario " + primeScenarioName + " size : " + primeGeneratedImage.getWidth() + "x" + primeGeneratedImage.getHeight());
                            TestReportCaptureBase.INSTANCE.warning("Scenario " + thisScenarioName + " size : " + otherGeneratedImage.getWidth() + "x" + otherGeneratedImage.getHeight());
                            osSpecificRunner.fail(IMAGES_ARE_DIFFERENT_SIZES);
                            if(failureMessage==null){ //only want the first thing to go wrong as the junit test fail reason
                                failureMessage = IMAGES_ARE_DIFFERENT_SIZES;
                            }
                        }else if (!imagesAreVerySimilar(primeGeneratedImage, otherGeneratedImage)) {
                            attachImage("Scenario " + primeScenarioName + " " + frame, thisFrameBaseImageFileName + "_" + primeScenarioName + ".png", primeGeneratedImage);
                            attachImage("Scenario " + thisScenarioName + " " + frame, thisFrameBaseImageFileName + "_" + thisScenarioName + ".png", otherGeneratedImage);
                            attachImage("Diff (between above scenarios)", thisFrameBaseImageFileName + "_" + primeScenarioName + "_" + thisScenarioName + "_diff.png", createComparisonImage(primeGeneratedImage, otherGeneratedImage));

                            if(failureMessage==null){ //only want the first thing to go wrong as the junit test fail reason
                                failureMessage = IMAGES_ARE_DIFFERENT_BETWEEN_SCENARIOS;
                            }
                            TestReportCaptureBase.INSTANCE.markFailInReport(IMAGES_ARE_DIFFERENT_BETWEEN_SCENARIOS);
                        }
                    }
                }
            }


            for(int frame : framesToTakeScreenshotsOn) {
                Path generatedImagePath = overallScreenshots.getScreenshotsAtFrame(primeScenarioName, frame).orElseGet(() -> osSpecificRunner.fail(
                        "Scenario " + primeScenarioName + " did not take screenshot on frame " + frame
                ));

                String thisFrameBaseImageFileName = baseImageFileName + "_f" + frame;

                Enumeration<URL> expectedImageResources = ScreenshotTest.class.getClassLoader().getResources(thisFrameBaseImageFileName + ".png");

                if(!expectedImageResources.hasMoreElements()){
                    try{
                        Image otherGeneratedImage = readImage(generatedImagePath.toFile());
                        osSpecificRunner.saveGeneratedImageToChangedImages(otherGeneratedImage, thisFrameBaseImageFileName + ".png");
                        attachImage("New image:", thisFrameBaseImageFileName + ".png", otherGeneratedImage);
                        String message = "Expected image not found, is this a new test? If so collect the new image from the step artefacts (on github). If running locally you can see them at build/changed-images but those should not be committed";
                        if(failureMessage==null){ //only want the first thing to go wrong as the junit test fail reason
                            failureMessage = message;
                        }
                        TestReportCaptureBase.INSTANCE.markFailInReport(message);
                        continue;
                    } catch(IOException e){
                        throw new RuntimeException(e);
                    }
                }

                Image generatedImage = readImage(generatedImagePath.toFile());
                Image expectedImage = readImage(expectedImageResources.nextElement());

                if(!imagesAreSameSize(generatedImage, expectedImage)){
                    attachImage("Expected", thisFrameBaseImageFileName + "_expected.png", expectedImage);
                    attachImage("Actual", thisFrameBaseImageFileName + "_actual.png", generatedImage);
                    osSpecificRunner.saveGeneratedImageToChangedImages(generatedImage, thisFrameBaseImageFileName);

                    TestReportCaptureBase.INSTANCE.warning("Image 1 size : " + generatedImage.getWidth() + "x" + generatedImage.getHeight());
                    TestReportCaptureBase.INSTANCE.warning("Image 2 size : " + expectedImage.getWidth() + "x" + expectedImage.getHeight());
                    osSpecificRunner.fail(IMAGES_ARE_DIFFERENT_SIZES);
                    if(failureMessage==null){ //only want the first thing to go wrong as the junit test fail reason
                        failureMessage = IMAGES_ARE_DIFFERENT_SIZES;
                    }
                }else if (imagesAreVerySimilar(generatedImage, expectedImage))  {
                    if(testType == TestType.KNOWN_TO_FAIL){
                        TestReportCaptureBase.INSTANCE.warning(KNOWN_BAD_TEST_IMAGES_SAME);
                    }
                } else {
                    //save the generated image to the build directory
                    osSpecificRunner.saveGeneratedImageToChangedImages(generatedImage, thisFrameBaseImageFileName);

                    attachImage("Expected", thisFrameBaseImageFileName + "_expected.png", expectedImage);
                    attachImage("Actual", thisFrameBaseImageFileName + "_actual.png", generatedImage);
                    attachImage("Diff", thisFrameBaseImageFileName + "_diff.png", createComparisonImage(generatedImage, expectedImage));

                    switch(testType){
                        case MUST_PASS:
                            if(failureMessage==null){ //only want the first thing to go wrong as the junit test fail reason
                                failureMessage = IMAGES_ARE_DIFFERENT;
                            }
                            TestReportCaptureBase.INSTANCE.markFailInReport(IMAGES_ARE_DIFFERENT);
                            break;
                        case NON_DETERMINISTIC:
                            TestReportCaptureBase.INSTANCE.warning(NON_DETERMINISTIC_TEST);
                            break;
                        case KNOWN_TO_FAIL:
                            TestReportCaptureBase.INSTANCE.warning(KNOWN_BAD_TEST_IMAGES_DIFFERENT);
                            break;
                    }
                }

            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading images", e);
        }

        if(failureMessage!=null){
            osSpecificRunner.fail(failureMessage);
        }
    }

    private Image readImage(URL location) {
        if ("file".equals(location.getProtocol())) {
            return readImage(new File(location.getFile()));
        }

        // internal jar load
        String path = location.getPath();
        int separatorIndex = path.indexOf("!/");
        String internalPath = separatorIndex != -1 ? path.substring(separatorIndex + 2) : path;
        if (!internalPath.startsWith("/")) {
            internalPath = "/" + internalPath;
        }
        return readImageFromClasspath(internalPath);
    }

    private Image readImageFromClasspath(String location) {
        AssetManager assetManager = new DesktopAssetManager();
        assetManager.registerLocator("", ClasspathLocator.class);
        assetManager.registerLoader(StbImageLoader.class, "png", "jpg", "jpeg");
        return assetManager.loadTexture(location).getImage();
    }

    private Image readImage(File file) {
        AssetManager assetManager = new DesktopAssetManager();
        assetManager.registerLocator(file.getParent(), FileLocator.class);
        assetManager.registerLoader(StbImageLoader.class, "png", "jpg", "jpeg");
        return assetManager.loadTexture(file.getName()).getImage();
    }

    public static ByteBuffer loadFileIntoByteBuffer(File file) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             FileChannel channel = raf.getChannel()) {
            ByteBuffer buffer = ByteBuffer.allocateDirect((int) channel.size());
            channel.read(buffer);
            buffer.flip(); // Prepare for reading
            return buffer;
        }
    }

    private String calculateImageFilePrefix(){
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        Class<?> screenshotTestBaseClass = ScreenshotTestBase.class;

        for (int i = 1; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            try {
                Class<?> clazz = Class.forName(element.getClassName());
                if (screenshotTestBaseClass.isAssignableFrom(clazz)) {
                    return element.getClassName() + "." + element.getMethodName();
                }
            } catch (ClassNotFoundException e) {
                // Class not found, skip
                continue;
            }
        }

        throw new RuntimeException("No caller class extending ScreenshotTestBase found in stack trace.");
    }

    /**
     * Attaches the image to the report. The image is written to the report directory
     */
    private void attachImage(String title, String fileName, Image originalImage) throws IOException{
        TestReportCaptureBase.INSTANCE.attachImage(title, fileName, originalImage);
    }

    private static boolean imagesAreSameSize(Image img1, Image img2) {
        return img1.getWidth() == img2.getWidth() && img1.getHeight() == img2.getHeight();
    }

    /**
     * Tests that the images are the same for the purposes of the test.
     * If they are not the same it will return false (which may fail the test depending on the test type).
     * Different sizes are so fatal that they will immediately fail the test.
     */
    private static boolean imagesAreVerySimilar(Image img1, Image img2) {
        ImagePixelWrapper image1Wrapper = new ImagePixelWrapper(img1);
        ImagePixelWrapper image2Wrapper = new ImagePixelWrapper(img2);

        for (int y = 0; y < img1.getHeight(); y++) {
            for (int x = 0; x < img1.getWidth(); x++) {

                int pixel1 = image1Wrapper.getARGB(x, y);
                int pixel2 = image2Wrapper.getARGB(x, y);

                int largestPixelValueDifference = getMaximumComponentDifference(pixel1, pixel2);

                if(largestPixelValueDifference>PixelSamenessDegree.NEGLIGIBLY_DIFFERENT.getMaximumAllowedDifference()){
                    return false;
                }

            }
        }
        return true;
    }

    /**
     * Creates an image that highlights the differences between the two images. The reference image is shown
     * dully in grey with blue, yellow, orange and red showing where pixels are different.
     */
    private static Image createComparisonImage(Image img1, Image img2) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(img1.getWidth() * img1.getHeight() * 4);
        ImagePixelWrapper comparisonImage = new ImagePixelWrapper(new Image(Image.Format.RGBA8, img1.getWidth(), img1.getHeight(), buffer, img1.getColorSpace()));
        ImagePixelWrapper image1Wrapped = new ImagePixelWrapper(img1);
        ImagePixelWrapper image2Wrapped = new ImagePixelWrapper(img2);

        for (int y = 0; y < img1.getHeight(); y++) {
            for (int x = 0; x < img1.getWidth(); x++) {
                PixelSamenessDegree pixelSameness = categorisePixelDifference(image1Wrapped.getARGB(x, y),image2Wrapped.getARGB(x, y));

                if(pixelSameness == PixelSamenessDegree.SAME){
                    int washedOutPixel = getWashedOutPixel(image1Wrapped, x, y, 0.9f);
                    //Color rawColor = new Color(img1.getRGB(x, y), true);
                    comparisonImage.setARGB(x, y, washedOutPixel);
                }else{
                    comparisonImage.setARGB(x, y, pixelSameness.getColorInDebugImage().asIntARGB());
                }
            }
        }
        return comparisonImage.getUnderlyingImage();
    }

    /**
     * This produces the almost grey ghost of the original image, used when the differences are being highlighted
     */
    public static int getWashedOutPixel(ImagePixelWrapper img, int x, int y, float alpha) {
        // Get the raw pixel value
        int rgb = img.getARGB(x, y);

        // Extract the color components
        int a = (rgb >> 24) & 0xFF;
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        // Define the overlay gray color (same value for r, g, b)
        int gray = 128;

        // Blend the original color with the gray color
        r = (int) ((1 - alpha) * r + alpha * gray);
        g = (int) ((1 - alpha) * g + alpha * gray);
        b = (int) ((1 - alpha) * b + alpha * gray);

        // Clamp the values to the range [0, 255]
        r = Math.min(255, r);
        g = Math.min(255, g);
        b = Math.min(255, b);

        // Combine the components back into a single int

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static PixelSamenessDegree categorisePixelDifference(int pixel1, int pixel2){
        if(pixel1 == pixel2){
            return PixelSamenessDegree.SAME;
        }

        int pixelDifference = getMaximumComponentDifference(pixel1, pixel2);

        if(pixelDifference<= PixelSamenessDegree.NEGLIGIBLY_DIFFERENT.getMaximumAllowedDifference()){
            return PixelSamenessDegree.NEGLIGIBLY_DIFFERENT;
        }
        if(pixelDifference<= PixelSamenessDegree.SUBTLY_DIFFERENT.getMaximumAllowedDifference()){
            return PixelSamenessDegree.SUBTLY_DIFFERENT;
        }
        if(pixelDifference<= PixelSamenessDegree.MEDIUMLY_DIFFERENT.getMaximumAllowedDifference()){
            return PixelSamenessDegree.MEDIUMLY_DIFFERENT;
        }
        if(pixelDifference<= PixelSamenessDegree.VERY_DIFFERENT.getMaximumAllowedDifference()){
            return PixelSamenessDegree.VERY_DIFFERENT;
        }
        return PixelSamenessDegree.EXTREMELY_DIFFERENT;
    }

    private static int getMaximumComponentDifference(int pixel1, int pixel2){
        int r1 = (pixel1 >> 16) & 0xFF;
        int g1 = (pixel1 >> 8) & 0xFF;
        int b1 = pixel1 & 0xFF;
        int a1 = (pixel1 >> 24) & 0xFF;

        int r2 = (pixel2 >> 16) & 0xFF;
        int g2 = (pixel2 >> 8) & 0xFF;
        int b2 = pixel2 & 0xFF;
        int a2 = (pixel2 >> 24) & 0xFF;

        return Math.max(Math.abs(r1 - r2), Math.max(Math.abs(g1 - g2), Math.max(Math.abs(b1 - b2), Math.abs(a1 - a2))));
    }



}
