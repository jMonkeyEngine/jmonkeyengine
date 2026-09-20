package org.jmonkeyengine.screenshottests.testframework.desktop;

import com.jme3.math.ColorRGBA;
import com.jme3.texture.Image;
import com.jme3.texture.image.ColorSpace;
import com.jme3.texture.image.ImageRaster;

import org.jmonkeyengine.screenshottests.testframework.ImageDifference;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTest;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTestMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for how {@link DesktopRunner} stores and finds reference images. No renderer is
 * booted; the images are built in memory.
 */
public class DesktopReferenceImagesTest {

    private static final String REFERENCE_DIRECTORY_PROPERTY = ScreenshotTest.REFERENCE_DIRECTORY_SYSTEM_PROPERTY;

    private static final String MODE_PROPERTY = ScreenshotTest.MODE_SYSTEM_PROPERTY;

    @AfterEach
    public void clearProperties() {
        System.clearProperty(REFERENCE_DIRECTORY_PROPERTY);
        System.clearProperty(MODE_PROPERTY);
    }

    @Test
    public void referenceImagesDefaultToTheBuildDirectory() {
        System.clearProperty(REFERENCE_DIRECTORY_PROPERTY);
        // Compared as a Path so the test passes on Windows too, where the separator differs.
        assertEquals(Path.of(DesktopRunner.DEFAULT_REFERENCE_IMAGES_DIRECTORY),
                DesktopRunner.getReferenceImagesDirectory());
    }

    @Test
    public void referenceDirectoryPropertyIsHonoured(@TempDir Path temporaryDirectory) {
        System.setProperty(REFERENCE_DIRECTORY_PROPERTY, temporaryDirectory.toString());
        assertEquals(temporaryDirectory, DesktopRunner.getReferenceImagesDirectory());
    }

    @Test
    public void unconfiguredModeIsRejected() {
        System.clearProperty(MODE_PROPERTY);
        assertThrows(IllegalArgumentException.class, () -> new DesktopRunner().getScreenshotTestMode());
    }

    @Test
    public void modePropertyIsHonoured() {
        System.setProperty(MODE_PROPERTY, "generate");
        assertEquals(ScreenshotTestMode.GENERATE, new DesktopRunner().getScreenshotTestMode());
    }

    @Test
    public void anImageThatWasNeverGeneratedIsNotFound(@TempDir Path temporaryDirectory) {
        // This is what tells a comparison run that a test is new on the branch under test.
        System.setProperty(REFERENCE_DIRECTORY_PROPERTY, temporaryDirectory.toString());

        assertFalse(new DesktopRunner().loadReferenceImage("never.generated.Test.test_f1.png").isPresent());
    }

    @Test
    public void aSavedReferenceImageIsFoundAndUnchanged(@TempDir Path temporaryDirectory) {
        System.setProperty(REFERENCE_DIRECTORY_PROPERTY, temporaryDirectory.toString());
        DesktopRunner runner = new DesktopRunner();
        Image original = gradient(8, 4);

        runner.saveReferenceImage(original, "com.example.TestFoo.testFoo_f1.png");

        assertTrue(Files.isRegularFile(temporaryDirectory.resolve("com.example.TestFoo.testFoo_f1.png")));

        Optional<Image> loaded = runner.loadReferenceImage("com.example.TestFoo.testFoo_f1.png");

        assertTrue(loaded.isPresent());
        assertEquals(original.getWidth(), loaded.get().getWidth());
        assertEquals(original.getHeight(), loaded.get().getHeight());
        // A round trip through PNG has to be lossless, otherwise every comparison would start from
        // a difference that has nothing to do with what was rendered.
        assertEquals(0, ImageDifference.of(original, loaded.get()).getDifferentPixels());
    }

    /**
     * Builds an image whose pixels all differ, so that a round trip that muddled them would show up.
     */
    private static Image gradient(int width, int height) {
        ByteBuffer data = ByteBuffer.allocateDirect(width * height * 4);
        data.order(ByteOrder.LITTLE_ENDIAN);
        Image image = new Image(Image.Format.RGBA8, width, height, data, ColorSpace.sRGB);

        ImageRaster raster = ImageRaster.create(image);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float red = (float) x / (width - 1);
                float green = (float) y / (height - 1);
                raster.setPixel(x, y, new ColorRGBA(red, green, 0.25f, 1f));
            }
        }
        return image;
    }
}
