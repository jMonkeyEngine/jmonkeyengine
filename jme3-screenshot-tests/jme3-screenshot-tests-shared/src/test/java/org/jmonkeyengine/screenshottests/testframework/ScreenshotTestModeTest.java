package org.jmonkeyengine.screenshottests.testframework;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ScreenshotTestMode}. These boot no renderer; they only cover how the
 * configured value is turned into a mode.
 */
public class ScreenshotTestModeTest {

    @Test
    public void unconfiguredIsRejectedWithAnExplanation() {
        // There is no default mode, because either candidate default fails silently and green. The
        // failure is how most people will learn what these tests do, so it has to teach.
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> ScreenshotTestMode.fromConfigurationValue(null));

        assertTrue(thrown.getMessage().contains("-Pscreenshot.mode=generate"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("-Pscreenshot.mode=compare"), thrown.getMessage());
    }

    @Test
    public void blankValueIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ScreenshotTestMode.fromConfigurationValue(""));
        assertThrows(IllegalArgumentException.class, () -> ScreenshotTestMode.fromConfigurationValue("   "));
    }

    @Test
    public void everyModeIsNamedInTheExplanation() {
        // Adding a mode without documenting it would leave the failure message lying.
        for (ScreenshotTestMode mode : ScreenshotTestMode.values()) {
            assertTrue(ScreenshotTestMode.HOW_TO_SELECT_A_MODE.contains(mode.getConfigurationValue()),
                    mode + " is missing from HOW_TO_SELECT_A_MODE");
        }
    }

    @Test
    public void eachModeIsSelectedByItsConfigurationValue() {
        for (ScreenshotTestMode mode : ScreenshotTestMode.values()) {
            assertEquals(mode, ScreenshotTestMode.fromConfigurationValue(mode.getConfigurationValue()));
        }
    }

    @Test
    public void configurationValueIsCaseInsensitiveAndTrimmed() {
        assertEquals(ScreenshotTestMode.GENERATE, ScreenshotTestMode.fromConfigurationValue("GENERATE"));
        assertEquals(ScreenshotTestMode.GENERATE, ScreenshotTestMode.fromConfigurationValue(" generate "));
        assertEquals(ScreenshotTestMode.COMPARE, ScreenshotTestMode.fromConfigurationValue("Compare"));
    }

    @Test
    public void unknownValueIsRejected() {
        // Guessing what was meant would be dangerous: read as "compare", a typo in the reference
        // pass produces no reference images at all, and the comparison pass then reports every
        // test as new.
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> ScreenshotTestMode.fromConfigurationValue("genrate"));

        assertTrue(thrown.getMessage().contains("genrate"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("generate"), thrown.getMessage());
    }

    @Test
    public void configurationPropertyNamesAreStable() {
        // The Gradle tasks and the CI workflows set these; renaming one silently would leave the
        // tests running in the wrong mode.
        assertEquals("jme.screenshot.mode", ScreenshotTest.MODE_SYSTEM_PROPERTY);
        assertEquals("jme.screenshot.referenceDir", ScreenshotTest.REFERENCE_DIRECTORY_SYSTEM_PROPERTY);
    }
}
