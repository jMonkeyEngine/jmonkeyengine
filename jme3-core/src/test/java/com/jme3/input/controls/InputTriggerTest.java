package com.jme3.input.controls;

import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputTriggerTest {

    @Test
    void keyTriggerUsesLowByteHashAndReadableName() {
        KeyTrigger trigger = new KeyTrigger(KeyInput.KEY_SPACE);

        assertEquals(KeyInput.KEY_SPACE, trigger.getKeyCode());
        assertEquals("KeyCode " + KeyInput.KEY_SPACE, trigger.getName());
        assertEquals(KeyInput.KEY_SPACE & 0xff, trigger.triggerHashCode());
    }

    @Test
    void mouseAxisTriggerNamesKnownAxesAndEncodesDirection() {
        MouseAxisTrigger xPositive = new MouseAxisTrigger(MouseInput.AXIS_X, false);
        MouseAxisTrigger wheelNegative = new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true);

        assertEquals(MouseInput.AXIS_X, xPositive.getMouseAxis());
        assertFalse(xPositive.isNegative());
        assertEquals("Mouse X Axis Positive", xPositive.getName());
        assertEquals("Mouse Wheel Negative", wheelNegative.getName());
        assertEquals(512 | MouseInput.AXIS_X, xPositive.triggerHashCode());
        assertEquals(768 | MouseInput.AXIS_WHEEL, wheelNegative.triggerHashCode());
        assertThrows(IllegalArgumentException.class, () -> new MouseAxisTrigger(9, false));
    }

    @Test
    void mouseButtonTriggerUsesButtonHashAndName() {
        MouseButtonTrigger trigger = new MouseButtonTrigger(MouseInput.BUTTON_RIGHT);

        assertEquals(MouseInput.BUTTON_RIGHT, trigger.getMouseButton());
        assertEquals("Mouse Button " + MouseInput.BUTTON_RIGHT, trigger.getName());
        assertEquals(256 | MouseInput.BUTTON_RIGHT, trigger.triggerHashCode());
    }

    @Test
    void joystickAxisTriggerEncodesJoystickAxisAndSign() {
        JoyAxisTrigger positive = new JoyAxisTrigger(2, 3, false);
        JoyAxisTrigger negative = new JoyAxisTrigger(2, 3, true);

        assertEquals(2, positive.getJoyId());
        assertEquals(3, positive.getAxisId());
        assertFalse(positive.isNegative());
        assertTrue(negative.isNegative());
        assertEquals("JoyAxis[joyId=2, axisId=3, neg=false]", positive.getName());
        assertEquals((2048 * 2) | 1024 | 3, positive.triggerHashCode());
        assertEquals((2048 * 2) | 1280 | 3, negative.triggerHashCode());
    }

    @Test
    void joystickButtonTriggerEncodesJoystickAndButton() {
        JoyButtonTrigger trigger = new JoyButtonTrigger(4, 7);

        assertEquals(4, trigger.getJoyId());
        assertEquals(7, trigger.getAxisId());
        assertEquals("JoyButton[joyId=4, axisId=7]", trigger.getName());
        assertEquals((2048 * 4) | 1536 | 7, trigger.triggerHashCode());
    }

    @Test
    void touchTriggerDistinguishesZeroFromNonZeroKeyCodes() {
        TouchTrigger zero = new TouchTrigger(0);
        TouchTrigger key = new TouchTrigger(KeyInput.KEY_RETURN);

        assertEquals("TouchInput KeyCode 0", zero.getName());
        assertEquals("TouchInput", key.getName());
        assertEquals(0, zero.getKeyCode());
        assertEquals(KeyInput.KEY_RETURN, key.getKeyCode());
        assertEquals(TouchTrigger.touchHash(0), zero.triggerHashCode());
        assertEquals(TouchTrigger.touchHash(KeyInput.KEY_RETURN), key.triggerHashCode());
    }
}
