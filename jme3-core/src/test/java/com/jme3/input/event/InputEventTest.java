package com.jme3.input.event;

import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import com.jme3.input.KeyInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InputEventTest {

    @Test
    void inputEventTracksTimeAndConsumedState() {
        KeyInputEvent event = new KeyInputEvent(KeyInput.KEY_A, 'a', true, false);

        event.setTime(123L);
        event.setConsumed();

        assertEquals(123L, event.getTime());
        assertTrue(event.isConsumed());
    }

    @Test
    void keyInputEventReportsPressReleaseRepeatAndText() {
        KeyInputEvent pressed = new KeyInputEvent(KeyInput.KEY_A, 'a', true, false);
        KeyInputEvent repeating = new KeyInputEvent(KeyInput.KEY_A, 'a', true, true);
        KeyInputEvent released = new KeyInputEvent(KeyInput.KEY_A, 'a', false, false);

        assertEquals(KeyInput.KEY_A, pressed.getKeyCode());
        assertEquals('a', pressed.getKeyChar());
        assertTrue(pressed.isPressed());
        assertFalse(pressed.isReleased());
        assertFalse(pressed.isRepeating());
        assertEquals("Key(CODE=30, CHAR=a, PRESSED)", pressed.toString());
        assertEquals("Key(CODE=30, CHAR=a, REPEATING)", repeating.toString());
        assertEquals("Key(CODE=30, CHAR=a, RELEASED)", released.toString());
    }

    @Test
    void mouseButtonEventReportsCoordinatesAndPressState() {
        MouseButtonEvent pressed = new MouseButtonEvent(1, true, 20, 30);
        MouseButtonEvent released = new MouseButtonEvent(1, false, 20, 30);

        assertEquals(1, pressed.getButtonIndex());
        assertEquals(20, pressed.getX());
        assertEquals(30, pressed.getY());
        assertTrue(pressed.isPressed());
        assertFalse(pressed.isReleased());
        assertEquals("MouseButton(BTN=1, PRESSED)", pressed.toString());
        assertEquals("MouseButton(BTN=1, RELEASED)", released.toString());
    }

    @Test
    void mouseMotionEventReportsPositionDeltaWheelAndText() {
        MouseMotionEvent event = new MouseMotionEvent(10, 11, -2, 3, 4, -1);

        assertEquals(10, event.getX());
        assertEquals(11, event.getY());
        assertEquals(-2, event.getDX());
        assertEquals(3, event.getDY());
        assertEquals(4, event.getWheel());
        assertEquals(-1, event.getDeltaWheel());
        assertEquals("MouseMotion(X=10, Y=11, DX=-2, DY=3, Wheel=4, dWheel=-1)", event.toString());
    }

    @Test
    void joystickAxisEventReportsAxisJoystickValueAndRawValue() {
        JoystickAxis axis = axis(8, 2);
        JoyAxisEvent event = new JoyAxisEvent(axis, 0.25f, 0.5f);
        JoyAxisEvent unchanged = new JoyAxisEvent(axis, -0.75f);

        assertSame(axis, event.getAxis());
        assertEquals(2, event.getAxisIndex());
        assertEquals(8, event.getJoyIndex());
        assertEquals(0.25f, event.getValue());
        assertEquals(0.5f, event.getRawValue());
        assertEquals(-0.75f, unchanged.getValue());
        assertEquals(-0.75f, unchanged.getRawValue());
    }

    @Test
    void joystickButtonEventReportsButtonJoystickAndPressState() {
        JoystickButton button = button(6, 4);
        JoyButtonEvent event = new JoyButtonEvent(button, true);

        assertSame(button, event.getButton());
        assertEquals(4, event.getButtonIndex());
        assertEquals(6, event.getJoyIndex());
        assertTrue(event.isPressed());
    }

    @Test
    void touchEventSettersAndResetCoverGestureKeyAndScaleState() {
        TouchEvent event = new TouchEvent(TouchEvent.Type.MOVE, 1f, 2f, 3f, 4f);
        event.setPointerId(9);
        event.setPressure(0.8f);
        event.setKeyCode(KeyInput.KEY_RETURN);
        event.setCharacters("\n");
        event.setScaleFactor(1.5f);
        event.setScaleSpan(20f);
        event.setDeltaScaleSpan(2f);
        event.setScaleSpanInProgress(true);
        event.setConsumed();

        assertEquals(TouchEvent.Type.MOVE, event.getType());
        assertEquals(1f, event.getX());
        assertEquals(2f, event.getY());
        assertEquals(3f, event.getDeltaX());
        assertEquals(4f, event.getDeltaY());
        assertEquals(9, event.getPointerId());
        assertEquals(0.8f, event.getPressure());
        assertEquals(KeyInput.KEY_RETURN, event.getKeyCode());
        assertEquals("\n", event.getCharacters());
        assertEquals(1.5f, event.getScaleFactor());
        assertEquals(20f, event.getScaleSpan());
        assertEquals(2f, event.getDeltaScaleSpan());
        assertTrue(event.isScaleSpanInProgress());
        assertTrue(event.isConsumed());
        assertEquals("TouchEvent(PointerId=9, Type=MOVE, X=1.0, Y=2.0, DX=3.0, DY=4.0, "
                + "ScaleSpan=20.0, dScaleSpan=2.0)", event.toString());

        event.set(TouchEvent.Type.UP);

        assertEquals(TouchEvent.Type.UP, event.getType());
        assertEquals(0f, event.getX());
        assertEquals(0f, event.getY());
        assertEquals(0, event.getPointerId());
        assertEquals(0f, event.getPressure());
        assertEquals(0, event.getKeyCode());
        assertEquals("", event.getCharacters());
        assertEquals(0f, event.getScaleFactor());
        assertEquals(0f, event.getScaleSpan());
        assertEquals(0f, event.getDeltaScaleSpan());
        assertFalse(event.isScaleSpanInProgress());
        assertFalse(event.isConsumed());
    }

    @Test
    void defaultTouchEventStartsIdle() {
        TouchEvent event = new TouchEvent();

        assertEquals(TouchEvent.Type.IDLE, event.getType());
        assertEquals(0f, event.getX());
        assertEquals(0f, event.getY());
    }

    private static JoystickAxis axis(int joyId, int axisId) {
        Joystick joystick = mock(Joystick.class);
        when(joystick.getJoyId()).thenReturn(joyId);

        JoystickAxis axis = mock(JoystickAxis.class);
        when(axis.getJoystick()).thenReturn(joystick);
        when(axis.getAxisId()).thenReturn(axisId);
        return axis;
    }

    private static JoystickButton button(int joyId, int buttonId) {
        Joystick joystick = mock(Joystick.class);
        when(joystick.getJoyId()).thenReturn(joyId);

        JoystickButton button = mock(JoystickButton.class);
        when(button.getJoystick()).thenReturn(joystick);
        when(button.getButtonId()).thenReturn(buttonId);
        return button;
    }
}
