package org.jmonkeyengine.screenshottests.testframework;

import com.aventstack.extentreports.ExtentTest;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.FastMath;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * The test driver allows for controlled interaction between the thread running the application and the thread
 * running the test
 */
public class TestDriver extends BaseAppState{

    public static final String IMAGES_ARE_DIFFERENT = "Images are different.";

    public static final String IMAGES_ARE_DIFFERENT_SIZES = "Images are different sizes.";

    public static final String KNOWN_BAD_TEST_IMAGES_DIFFERENT = "Images are different. This is a known broken test.";

    public static final String KNOWN_BAD_TEST_IMAGES_SAME = "This is (or was?) a known broken test but it is now passing, please change the test type to MUST_PASS.";

    public static final String NON_DETERMINISTIC_TEST = "This is a non deterministic test, please manually review the expected and actual images to make sure they are approximately the same.";

    private static final Executor executor = Executors.newSingleThreadExecutor( (r) -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });

    int tick = 0;

    Collection<Integer> framesToTakeScreenshotsOn;

    ScreenshotNoInputAppState screenshotAppState;

    private final Object waitLock = new Object();

    private final int tickToTerminateApp;

    public TestDriver(ScreenshotNoInputAppState screenshotAppState, Collection<Integer> framesToTakeScreenshotsOn){
        this.screenshotAppState = screenshotAppState;
        this.framesToTakeScreenshotsOn = framesToTakeScreenshotsOn;
        this.tickToTerminateApp = framesToTakeScreenshotsOn.stream().mapToInt(i -> i).max().orElse(0) + 1;
    }

    @Override
    public void update(float tpf){
        super.update(tpf);

        if(framesToTakeScreenshotsOn.contains(tick)){
            screenshotAppState.takeScreenshot();
        }
        if(tick >= tickToTerminateApp){
            getApplication().stop(true);
            synchronized (waitLock) {
                waitLock.notify(); // Release the wait
            }
        }

        tick++;
    }

    @Override protected void initialize(Application app){}

    @Override protected void cleanup(Application app){}

    @Override protected void onEnable(){}

    @Override protected void onDisable(){}


    /**
     * Boots up the application on a separate thread (blocks this thread) and then does the following:
     * - Takes screenshots on the requested frames
     * - After all the frames have been taken it stops the application
     * - Compares the screenshot to the expected screenshot (if any). Fails the test if they are different
     */
    public static void bootAppForTest(TestType testType, AppSettings appSettings, String baseImageFileName, List<Integer> framesToTakeScreenshotsOn, AppState... initialStates){
        FastMath.rand.setSeed(0); //try to make things deterministic by setting the random seed
        Collections.sort(framesToTakeScreenshotsOn);

        Path imageTempDir;

        try{
            imageTempDir = Files.createTempDirectory("jmeSnapshotTest");
        } catch(IOException e){
            throw new RuntimeException(e);
        }

        ScreenshotNoInputAppState screenshotAppState = new ScreenshotNoInputAppState(imageTempDir.toString() + "/");
        String screenshotAppFileNamePrefix = "Screenshot-";
        screenshotAppState.setFileName(screenshotAppFileNamePrefix);

        List<AppState> states = new ArrayList<>(Arrays.asList(initialStates));
        TestDriver testDriver = new TestDriver(screenshotAppState, framesToTakeScreenshotsOn);
        states.add(screenshotAppState);
        states.add(testDriver);

        SimpleApplication app = new App(states.toArray(new AppState[0]));
        app.setSettings(appSettings);
        app.setShowSettings(false);

        executor.execute(() -> app.start(JmeContext.Type.Display));

        synchronized (testDriver.waitLock) {
            try {
                testDriver.waitLock.wait(10000); // Wait for the screenshot to be taken and application to stop
                Thread.sleep(200); //give time for openGL is fully released before starting a new test (get random JVM crashes without this)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }

        //search the imageTempDir
        List<Path> imageFiles = new ArrayList<>();
        try(Stream<Path> paths = Files.list(imageTempDir)){
            paths.forEach(imageFiles::add);
        } catch(IOException e){
            throw new RuntimeException(e);
        }

        //this resorts with natural numeric ordering (so App10.png comes after App9.png)
        imageFiles.sort(new Comparator<Path>(){
            @Override
            public int compare(Path p1, Path p2){
                return extractNumber(p1).compareTo(extractNumber(p2));
            }

            private Integer extractNumber(Path path){
                String name = path.getFileName().toString();
                int numStart = screenshotAppFileNamePrefix.length();
                int numEnd = name.lastIndexOf(".png");
                return Integer.parseInt(name.substring(numStart, numEnd));
            }
        });

        if(imageFiles.isEmpty()){
            fail("No screenshot found in the temporary directory.");
        }
        if(imageFiles.size() != framesToTakeScreenshotsOn.size()){
            fail("Not all screenshots were taken, expected " + framesToTakeScreenshotsOn.size() + " but got " + imageFiles.size());
        }

        String failureMessage = null;

        try {
            for(int screenshotIndex=0;screenshotIndex<framesToTakeScreenshotsOn.size();screenshotIndex++){
                Path generatedImage = imageFiles.get(screenshotIndex);
                int frame = framesToTakeScreenshotsOn.get(screenshotIndex);

                String thisFrameBaseImageFileName = baseImageFileName + "_f" + frame;

                Path expectedImage = Paths.get("src/test/resources/" + thisFrameBaseImageFileName + ".png");

                if(!Files.exists(expectedImage)){
                    try{
                        Path savedImage = saveGeneratedImageToSavedImages(generatedImage, thisFrameBaseImageFileName);
                        attachImage("New image:", thisFrameBaseImageFileName + ".png", savedImage);
                        String message = "Expected image not found, is this a new test? If so collect the new image from the step artefacts";
                        if(failureMessage==null){ //only want the first thing to go wrong as the junit test fail reason
                            failureMessage = message;
                        }
                        ExtentReportExtension.getCurrentTest().fail(message);
                        continue;
                    } catch(IOException e){
                        throw new RuntimeException(e);
                    }
                }

                BufferedImage img1 = ImageIO.read(generatedImage.toFile());
                BufferedImage img2 = ImageIO.read(expectedImage.toFile());

                if (imagesAreTheSame(img1, img2)) {
                    if(testType == TestType.KNOWN_TO_FAIL){
                        ExtentReportExtension.getCurrentTest().warning(KNOWN_BAD_TEST_IMAGES_SAME);
                    }
                } else {
                    //save the generated image to the build directory
                    Path savedImage = saveGeneratedImageToSavedImages(generatedImage, thisFrameBaseImageFileName);

                    attachImage("Expected", thisFrameBaseImageFileName + "_expected.png", expectedImage);
                    attachImage("Actual", thisFrameBaseImageFileName + "_actual.png", savedImage);
                    attachImage("Diff", thisFrameBaseImageFileName + "_diff.png", createComparisonImage(img1, img2));

                    switch(testType){
                        case MUST_PASS:
                            if(failureMessage==null){ //only want the first thing to go wrong as the junit test fail reason
                                failureMessage = IMAGES_ARE_DIFFERENT;
                            }
                            ExtentReportExtension.getCurrentTest().fail(IMAGES_ARE_DIFFERENT);
                            break;
                        case NON_DETERMINISTIC:
                            ExtentReportExtension.getCurrentTest().warning(NON_DETERMINISTIC_TEST);
                            break;
                        case KNOWN_TO_FAIL:
                            ExtentReportExtension.getCurrentTest().warning(KNOWN_BAD_TEST_IMAGES_DIFFERENT);
                            break;
                    }
                }

            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading images", e);
        } finally{
            clearTemporaryFolder(imageTempDir);
        }

        if(failureMessage!=null){
            fail(failureMessage);
        }
    }

    private static void clearTemporaryFolder(Path temporaryFolder){
        try (Stream<Path> paths = Files.walk(temporaryFolder)) {
            paths.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Path saveGeneratedImageToSavedImages(Path generatedImage, String imageFileName) throws IOException{
        Path savedImage = Paths.get("build/changed-images/" + imageFileName + ".png");
        Files.createDirectories(savedImage.getParent());
        Files.copy(generatedImage, savedImage, StandardCopyOption.REPLACE_EXISTING);
        aggressivelyCompressImage(savedImage);
        return savedImage;
    }

    /**
     * This remains lossless but makes the maximum effort to compress the image. As these images
     * may be committed to the repository it is important to keep them as small as possible and worth the extra CPU time
     * to do so
     */
    public static void aggressivelyCompressImage(Path path) throws IOException {
        // Load your image
        BufferedImage image = ImageIO.read(path.toFile());

        // Get a PNG writer
        ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
        ImageWriteParam writeParam = writer.getDefaultWriteParam();

        // Increase compression effort
        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        writeParam.setCompressionQuality(0.0f); // 0.0 means maximum compression

        // Save the image with increased compression
        try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(path.toFile())) {
            writer.setOutput(outputStream);
            writer.write(null, new IIOImage(image, null, null), writeParam);
        }

        // Clean up
        writer.dispose();
    }

    public static void attachImage(String title, String fileName, Path originalImage) throws IOException{
        ExtentTest test = ExtentReportExtension.getCurrentTest();
        Files.copy(originalImage.toAbsolutePath(), Paths.get("build/reports/" + fileName), StandardCopyOption.REPLACE_EXISTING);
        test.addScreenCaptureFromPath(fileName, title);
    }

    public static void attachImage(String title, String fileName, BufferedImage originalImage) throws IOException{
        ExtentTest test = ExtentReportExtension.getCurrentTest();
        ImageIO.write(originalImage, "png", Paths.get("build/reports/" + fileName).toFile());
        test.addScreenCaptureFromPath(fileName, title);
    }

    private static boolean imagesAreTheSame(BufferedImage img1, BufferedImage img2) {
        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            ExtentReportExtension.getCurrentTest().createNode("Image 1 size : " + img1.getWidth() + "x" + img1.getHeight());
            ExtentReportExtension.getCurrentTest().createNode("Image 2 size : " + img2.getWidth() + "x" + img2.getHeight());
            fail(IMAGES_ARE_DIFFERENT_SIZES);
        }

        for (int y = 0; y < img1.getHeight(); y++) {
            for (int x = 0; x < img1.getWidth(); x++) {
                if (!pixelsApproximatelyEqual(img1.getRGB(x, y),img2.getRGB(x, y))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static BufferedImage createComparisonImage(BufferedImage img1, BufferedImage img2) {
        BufferedImage comparisonImage = new BufferedImage(img1.getWidth(), img1.getHeight(), BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < img1.getHeight(); y++) {
            for (int x = 0; x < img1.getWidth(); x++) {
                PixelSamenessDegree pixelSameness = categorisePixelDifference(img1.getRGB(x, y),img2.getRGB(x, y));

                if(pixelSameness == PixelSamenessDegree.SAME){
                    int washedOutPixel = getWashedOutPixel(img1, x, y, 0.9f);
                    //Color rawColor = new Color(img1.getRGB(x, y), true);
                    comparisonImage.setRGB(x, y, washedOutPixel);
                }else{
                    comparisonImage.setRGB(x, y, pixelSameness.getColorInDebugImage().asIntARGB());
                }
            }
        }
        return comparisonImage;
    }

    /**
     * This produces the almost grey ghost of the original image, used when the differences are being highlighted
     */
    public static int getWashedOutPixel(BufferedImage img, int x, int y, float alpha) {
        // Get the raw pixel value
        int rgb = img.getRGB(x, y);

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

    private static boolean pixelsApproximatelyEqual(int pixel1, int pixel2){
        if(pixel1 == pixel2){
            return true;
        }

        int pixelDifference = getMaximumComponentDifference(pixel1, pixel2);

        return pixelDifference <= PixelSamenessDegree.SAME.getMaximumAllowedDifference();
    }

    private static PixelSamenessDegree categorisePixelDifference(int pixel1, int pixel2){
        if(pixel1 == pixel2){
            return PixelSamenessDegree.SAME;
        }

        int pixelDifference = getMaximumComponentDifference(pixel1, pixel2);

        if(pixelDifference<= PixelSamenessDegree.SAME.getMaximumAllowedDifference()){
            return PixelSamenessDegree.SAME;
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
