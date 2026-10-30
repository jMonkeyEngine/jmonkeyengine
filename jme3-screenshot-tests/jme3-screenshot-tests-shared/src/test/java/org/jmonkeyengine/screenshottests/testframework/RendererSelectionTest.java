package org.jmonkeyengine.screenshottests.testframework;

import com.jme3.system.AppSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for renderer selection in {@link ScreenshotTest}.
 * These do not boot any renderer; they only verify that the
 * {@code jme.screenshot.renderer} system property resolves to the expected value.
 */
public class RendererSelectionTest {

    private static final String PROPERTY = ScreenshotTest.RENDERER_SYSTEM_PROPERTY;

    @AfterEach
    public void clearRendererProperty() {
        System.clearProperty(PROPERTY);
    }

    @Test
    public void defaultRendererIsOpenGL45() {
        System.clearProperty(PROPERTY);
        assertEquals(AppSettings.LWJGL_OPENGL45, ScreenshotTest.resolveRenderer());
    }

    @Test
    public void angleRendererIsHonoured() {
        System.setProperty(PROPERTY, AppSettings.ANGLE_GLES3);
        assertEquals(AppSettings.ANGLE_GLES3, ScreenshotTest.resolveRenderer());
    }

    @Test
    public void defaultRendererYieldsUnsuffixedReferenceImages() {
        System.clearProperty(PROPERTY);
        assertEquals("", ScreenshotTest.referenceImageSuffix(ScreenshotTest.resolveRenderer()));
    }

    @Test
    public void angleRendererYieldsAngleSuffixedReferenceImages() {
        System.setProperty(PROPERTY, AppSettings.ANGLE_GLES3);
        assertEquals("_angle", ScreenshotTest.referenceImageSuffix(ScreenshotTest.resolveRenderer()));
    }
}
