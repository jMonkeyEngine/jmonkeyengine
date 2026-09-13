package com.jme3.input;

import com.jme3.input.controls.JoyAxisTrigger;
import com.jme3.input.controls.JoyButtonTrigger;
import com.jme3.input.controls.Trigger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DefaultJoystickTest {

    @Test
    void storesAxesAndButtonsAsReadOnlyLogicalCollections() {
        TestJoystick joystick = new TestJoystick(mock(InputManager.class), mock(JoyInput.class), 3, "Gamepad");
        DefaultJoystickAxis xAxis = new DefaultJoystickAxis(null, joystick, 0, "X Axis",
                JoystickAxis.X_AXIS, true, false, 0.1f);
        DefaultJoystickButton fire = new DefaultJoystickButton(null, joystick, 1, "Fire",
                JoystickButton.BUTTON_1);

        joystick.addTestAxis(xAxis);
        joystick.addTestButton(fire);

        assertSame(xAxis, joystick.getAxis(JoystickAxis.X_AXIS));
        assertNull(joystick.getAxis(JoystickAxis.Y_AXIS));
        assertSame(fire, joystick.getButton(JoystickButton.BUTTON_1));
        assertNull(joystick.getButton(JoystickButton.BUTTON_2));
        assertEquals(1, joystick.getAxisCount());
        assertEquals(1, joystick.getButtonCount());
        assertThrows(UnsupportedOperationException.class, () -> joystick.getAxes().clear());
        assertThrows(UnsupportedOperationException.class, () -> joystick.getButtons().clear());
        assertEquals("Joystick[name=Gamepad, id=3, buttons=1, axes=1]", joystick.toString());
    }

    @Test
    void rumbleDelegatesToJoyInput() {
        JoyInput joyInput = mock(JoyInput.class);
        TestJoystick joystick = new TestJoystick(mock(InputManager.class), joyInput, 7, "Wheel");

        joystick.rumble(0.75f);

        verify(joyInput).setJoyRumble(7, 0.75f);
    }

    @Test
    void defaultAxisExposesStateAndAssignsPositiveAndNegativeTriggers() {
        InputManager inputManager = mock(InputManager.class);
        TestJoystick joystick = new TestJoystick(inputManager, mock(JoyInput.class), 2, "Arcade Stick");
        DefaultJoystickAxis axis = new DefaultJoystickAxis(inputManager, joystick, 4, "Throttle",
                "throttle", true, false, 0.2f);

        axis.setDeadZone(0.35f);
        axis.assignAxis("Throttle+", "Throttle-");

        assertSame(joystick, axis.getJoystick());
        assertEquals("Throttle", axis.getName());
        assertEquals("throttle", axis.getLogicalId());
        assertEquals(4, axis.getAxisId());
        assertTrue(axis.isAnalog());
        assertFalse(axis.isRelative());
        assertEquals(0.35f, axis.getDeadZone());
        assertEquals(0f, axis.getJitterThreshold());
        assertEquals("JoystickAxis[name=Throttle, parent=Arcade Stick, id=4, logicalId=throttle, "
                + "isAnalog=true, isRelative=false, deadZone=0.35, jitterThreshold=0.0]", axis.toString());

        ArgumentCaptor<Trigger[]> positive = ArgumentCaptor.forClass(Trigger[].class);
        ArgumentCaptor<Trigger[]> negative = ArgumentCaptor.forClass(Trigger[].class);
        verify(inputManager).addMapping(eq("Throttle+"), positive.capture());
        verify(inputManager).addMapping(eq("Throttle-"), negative.capture());
        JoyAxisTrigger positiveTrigger = (JoyAxisTrigger) positive.getValue()[0];
        JoyAxisTrigger negativeTrigger = (JoyAxisTrigger) negative.getValue()[0];
        assertEquals(2, positiveTrigger.getJoyId());
        assertEquals(4, positiveTrigger.getAxisId());
        assertFalse(positiveTrigger.isNegative());
        assertTrue(negativeTrigger.isNegative());
    }

    @Test
    void defaultAxisWithUnknownIndexDoesNotAssignMappings() {
        InputManager inputManager = mock(InputManager.class);
        TestJoystick joystick = new TestJoystick(inputManager, mock(JoyInput.class), 2, "Unknown");
        DefaultJoystickAxis axis = new DefaultJoystickAxis(inputManager, joystick, -1, "Unknown",
                "unknown", false, true, 0f);

        axis.assignAxis("Positive", "Negative");

        org.mockito.Mockito.verifyNoInteractions(inputManager);
    }

    @Test
    void defaultButtonExposesStateAndAssignsTrigger() {
        InputManager inputManager = mock(InputManager.class);
        TestJoystick joystick = new TestJoystick(inputManager, mock(JoyInput.class), 5, "Pad");
        DefaultJoystickButton button = new DefaultJoystickButton(inputManager, joystick, 6, "Start",
                JoystickButton.BUTTON_XBOX_START);

        button.assignButton("Pause");

        assertSame(joystick, button.getJoystick());
        assertEquals("Start", button.getName());
        assertEquals(JoystickButton.BUTTON_XBOX_START, button.getLogicalId());
        assertEquals(6, button.getButtonId());
        assertEquals("JoystickButton[name=Start, parent=Pad, id=6, logicalId=9]", button.toString());

        ArgumentCaptor<Trigger[]> triggers = ArgumentCaptor.forClass(Trigger[].class);
        verify(inputManager).addMapping(eq("Pause"), triggers.capture());
        JoyButtonTrigger trigger = (JoyButtonTrigger) triggers.getValue()[0];
        assertEquals(5, trigger.getJoyId());
        assertEquals(6, trigger.getAxisId());
    }

    private static final class TestJoystick extends AbstractJoystick {
        private TestJoystick(InputManager inputManager, JoyInput joyInput, int joyId, String name) {
            super(inputManager, joyInput, joyId, name);
        }

        void addTestAxis(JoystickAxis axis) {
            addAxis(axis);
        }

        void addTestButton(JoystickButton button) {
            addButton(button);
        }

        @Override
        public JoystickAxis getXAxis() {
            return getAxis(JoystickAxis.X_AXIS);
        }

        @Override
        public JoystickAxis getYAxis() {
            return getAxis(JoystickAxis.Y_AXIS);
        }

        @Override
        public JoystickAxis getPovXAxis() {
            return getAxis(JoystickAxis.POV_X);
        }

        @Override
        public JoystickAxis getPovYAxis() {
            return getAxis(JoystickAxis.POV_Y);
        }
    }
}
