package com.jme3.input.lwjgl;

import com.jme3.system.lwjgl.LwjglWindow;
import java.lang.reflect.Field;
import java.util.Queue;
import org.junit.jupiter.api.Test;
import org.lwjgl.sdl.SDL_Event;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_MOTION;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SdlMouseInputTest {

    @Test
    public void shouldIgnoreMouseMotionWhenWindowIsUnfocused() throws Exception {
        LwjglWindow context = mock(LwjglWindow.class);
        when(context.getWindowId()).thenReturn(7);

        SdlMouseInput mouseInput = new SdlMouseInput(context);
        setField(mouseInput, "windowFocused", false);

        SDL_Event event = SDL_Event.calloc();
        try {
            event.type(SDL_EVENT_MOUSE_MOTION);
            event.motion().windowID(7);

            mouseInput.onSDLEvent(event);
        } finally {
            event.free();
        }

        Queue<?> mouseMotionEvents = getField(mouseInput, "mouseMotionEvents");
        assertTrue(mouseMotionEvents.isEmpty());
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(target);
    }
}
