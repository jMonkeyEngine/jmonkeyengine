package org.jmonkeyengine.screenshottests.testframework;

import com.jme3.system.AppSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the per-renderer reference image naming.
 * These do not boot any renderer; they only verify the naming contract:
 * OpenGL runs use the historic unsuffixed names, ANGLE runs use the {@code _angle} suffix.
 */
public class RendererReferenceSuffixTest {

    @Test
    public void openGLRendererHasNoSuffix() {
        assertEquals("", ScreenshotTest.referenceImageSuffix(AppSettings.LWJGL_OPENGL45));
    }

    @Test
    public void angleRendererHasAngleSuffix() {
        assertEquals("_angle", ScreenshotTest.referenceImageSuffix(AppSettings.ANGLE_GLES3));
    }

    @Test
    public void unknownRendererHasNoSuffix() {
        assertEquals("", ScreenshotTest.referenceImageSuffix("SOME_UNKNOWN_RENDERER"));
    }

    @Test
    public void nullRendererHasNoSuffix() {
        assertEquals("", ScreenshotTest.referenceImageSuffix(null));
    }

    @Test
    public void rendererPropertyNameIsStable() {
        // The Gradle tasks and the CI workflow set this property; renaming it
        // silently would break renderer selection.
        assertEquals("jme.screenshot.renderer", ScreenshotTest.RENDERER_SYSTEM_PROPERTY);
    }
}
