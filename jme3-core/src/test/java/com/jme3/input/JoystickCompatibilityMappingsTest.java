package com.jme3.input;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JoystickCompatibilityMappingsTest {

    @Test
    void remapsSpecificAxisButtonAndGenericComponents() {
        String stick = "Codex Test Stick";
        JoystickCompatibilityMappings.addAxisMapping(stick, "x_raw", JoystickAxis.X_AXIS);
        JoystickCompatibilityMappings.addButtonMapping(stick, "0_raw", JoystickButton.BUTTON_XBOX_A);
        JoystickCompatibilityMappings.addMapping(stick, "misc_raw", "misc");

        assertEquals(JoystickAxis.X_AXIS, JoystickCompatibilityMappings.remapAxis("  " + stick + "  ", "x_raw"));
        assertEquals(JoystickButton.BUTTON_XBOX_A, JoystickCompatibilityMappings.remapButton(stick, "0_raw"));
        assertEquals("misc", JoystickCompatibilityMappings.remapComponent(stick, "misc_raw"));
        assertEquals("unmapped", JoystickCompatibilityMappings.remapAxis(stick, "unmapped"));
        assertEquals("unmapped", JoystickCompatibilityMappings.remapButton(stick, "unmapped"));
        assertEquals("unmapped", JoystickCompatibilityMappings.remapComponent(stick, "unmapped"));
    }

    @Test
    void returnsUnmodifiableMappingViews() {
        String stick = "Codex Test Mapping View";
        JoystickCompatibilityMappings.addMapping(stick, "raw", "logical");
        JoystickCompatibilityMappings.addButtonMapping(stick, "buttonRaw", "buttonLogical");

        Map<String, String> componentMappings = JoystickCompatibilityMappings.getJoystickMappings(stick);
        Map<String, String> buttonMappings = JoystickCompatibilityMappings.getJoystickButtonMappings(stick);

        assertEquals("logical", componentMappings.get("raw"));
        assertEquals("buttonLogical", buttonMappings.get("buttonRaw"));
        assertThrows(UnsupportedOperationException.class, () -> componentMappings.put("other", "value"));
        assertThrows(UnsupportedOperationException.class, () -> buttonMappings.put("other", "value"));
        assertEquals(0, JoystickCompatibilityMappings.getJoystickMappings("No Such Stick").size());
    }

    @Test
    void addMappingsParsesTypedEntriesRangesAndNameRegex() {
        Properties properties = new Properties();
        properties.setProperty("axis.Codex Regex Stick.x", "left_x [-1.0, 1.0]");
        properties.setProperty("button.Codex Regex Stick.trigger", "fire");
        properties.setProperty("Codex Regex Stick.misc", "menu");
        properties.setProperty("Codex Regex Stick.regex", "Codex Regex Stick \\(rev \\d+\\)");

        JoystickCompatibilityMappings.addMappings(properties);

        String physicalName = "Codex Regex Stick (rev 42)";
        assertEquals("left_x", JoystickCompatibilityMappings.remapAxis(physicalName, "x"));
        assertEquals("fire", JoystickCompatibilityMappings.remapButton(physicalName, "trigger"));
        assertEquals("menu", JoystickCompatibilityMappings.remapComponent(physicalName, "misc"));
    }

    @Test
    void remapAxisRangeUsesConfiguredRangeAndCachesMissingMappings() {
        String stick = "Codex Test Axis Range";
        JoystickCompatibilityMappings.addAxisMapping(stick, "slider", "slider", new float[]{0f, 1f});
        JoystickAxis mappedAxis = axis(stick, "slider");
        JoystickAxis unmappedAxis = axis(stick, "unknown");

        assertEquals(0.25f, JoystickCompatibilityMappings.remapAxisRange(mappedAxis, -0.5f));
        assertEquals(0.75f, JoystickCompatibilityMappings.remapAxisRange(mappedAxis, 0.5f));
        assertEquals(0.4f, JoystickCompatibilityMappings.remapAxisRange(unmappedAxis, 0.4f));
        assertEquals(-0.6f, JoystickCompatibilityMappings.remapAxisRange(unmappedAxis, -0.6f));
    }

    @Test
    void rejectsInvalidAxisRange() {
        assertThrows(IllegalArgumentException.class,
                () -> JoystickCompatibilityMappings.addAxisMapping("Bad Range", "axis", "axis", new float[]{0f}));
    }

    private static JoystickAxis axis(String joystickName, String axisName) {
        Joystick joystick = mock(Joystick.class);
        when(joystick.getName()).thenReturn(joystickName);

        JoystickAxis axis = mock(JoystickAxis.class);
        when(axis.getJoystick()).thenReturn(joystick);
        when(axis.getName()).thenReturn(axisName);
        return axis;
    }
}
