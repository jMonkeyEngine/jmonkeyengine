package com.jme3.input.lwjgl;

import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.system.AppSettings;
import com.jme3.system.lwjgl.LwjglWindow;
import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import org.junit.jupiter.api.Test;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDLMouse;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_MOTION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
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

    @Test
    public void shouldRestoreCursorPositionBeforeDisablingRelativeMode() throws Exception {
        LwjglWindow context = mockRenderableContext(800, 600);
        List<String> calls = new ArrayList<>();

        try (MockedStatic<SDLMouse> sdlMouse = mockStatic(SDLMouse.class)) {
            sdlMouse.when(() -> SDLMouse.SDL_GetMouseState(any(FloatBuffer.class), any(FloatBuffer.class)))
                    .thenAnswer(invocation -> {
                        invocation.<FloatBuffer>getArgument(0).put(0, 123f);
                        invocation.<FloatBuffer>getArgument(1).put(0, 456f);
                        return 0;
                    });
            sdlMouse.when(() -> SDLMouse.SDL_SetWindowRelativeMouseMode(42L, true))
                    .thenReturn(true);
            sdlMouse.when(() -> SDLMouse.SDL_WarpMouseInWindow(42L, 123f, 456f))
                    .thenAnswer(invocation -> {
                        calls.add("warp");
                        return null;
                    });
            sdlMouse.when(() -> SDLMouse.SDL_SetWindowRelativeMouseMode(42L, false))
                    .thenAnswer(invocation -> {
                        calls.add("disable relative mode");
                        return true;
                    });

            SdlMouseInput mouseInput = new SdlMouseInput(context);
            mouseInput.setCursorVisible(false);
            setField(mouseInput, "mouseX", 999);
            setField(mouseInput, "mouseY", -100);
            calls.clear();

            mouseInput.setCursorVisible(true);

            assertEquals(Arrays.asList("warp", "disable relative mode"), calls);
            assertEquals(123, (int) getField(mouseInput, "mouseX"));
            assertEquals(144, (int) getField(mouseInput, "mouseY"));
            Queue<MouseMotionEvent> mouseMotionEvents = getField(mouseInput, "mouseMotionEvents");
            MouseMotionEvent syncEvent = mouseMotionEvents.remove();
            assertEquals(123, syncEvent.getX());
            assertEquals(144, syncEvent.getY());
            assertEquals(0, syncEvent.getDX());
            assertEquals(0, syncEvent.getDY());
            assertTrue(mouseMotionEvents.isEmpty());
        }
    }

    @Test
    public void shouldNotClampVirtualCursorPositionInRelativeMode() throws Exception {
        LwjglWindow context = mockRenderableContext(800, 600);
        when(context.getWindowId()).thenReturn(7);

        try (MockedStatic<SDLMouse> sdlMouse = mockStatic(SDLMouse.class)) {
            sdlMouse.when(() -> SDLMouse.SDL_GetWindowRelativeMouseMode(42L)).thenReturn(true);

            SdlMouseInput mouseInput = new SdlMouseInput(context);
            setField(mouseInput, "mouseX", 799);
            setField(mouseInput, "mouseY", 1);

            SDL_Event event = SDL_Event.calloc();
            try {
                event.type(SDL_EVENT_MOUSE_MOTION);
                event.motion().windowID(7);
                event.motion().xrel(5f);
                event.motion().yrel(5f);
                mouseInput.onSDLEvent(event);
            } finally {
                event.free();
            }

            assertEquals(804, (int) getField(mouseInput, "mouseX"));
            assertEquals(-4, (int) getField(mouseInput, "mouseY"));
        }
    }

    @Test
    public void shouldKeepVirtualCursorPositionForButtonsInRelativeMode() throws Exception {
        LwjglWindow context = mockRenderableContext(800, 600);
        when(context.getWindowId()).thenReturn(7);

        try (MockedStatic<SDLMouse> sdlMouse = mockStatic(SDLMouse.class)) {
            sdlMouse.when(() -> SDLMouse.SDL_GetWindowRelativeMouseMode(42L)).thenReturn(true);

            SdlMouseInput mouseInput = new SdlMouseInput(context);
            setField(mouseInput, "mouseX", 900);
            setField(mouseInput, "mouseY", -100);

            SDL_Event event = SDL_Event.calloc();
            try {
                event.type(SDL_EVENT_MOUSE_BUTTON_DOWN);
                event.button().windowID(7);
                event.button().button((byte) SDLMouse.SDL_BUTTON_LEFT);
                event.button().down(true);
                event.button().x(10f);
                event.button().y(20f);
                mouseInput.onSDLEvent(event);
            } finally {
                event.free();
            }

            assertEquals(900, (int) getField(mouseInput, "mouseX"));
            assertEquals(-100, (int) getField(mouseInput, "mouseY"));
            Queue<MouseButtonEvent> mouseButtonEvents = getField(mouseInput, "mouseButtonEvents");
            MouseButtonEvent buttonEvent = mouseButtonEvents.remove();
            assertEquals(900, buttonEvent.getX());
            assertEquals(-100, buttonEvent.getY());
            assertTrue(mouseButtonEvents.isEmpty());
        }
    }

    private static LwjglWindow mockRenderableContext(int width, int height) {
        AppSettings settings = new AppSettings(false);
        settings.setResolution(width, height);
        LwjglWindow context = mock(LwjglWindow.class);
        when(context.isRenderable()).thenReturn(true);
        when(context.getWindowHandle()).thenReturn(42L);
        when(context.getSettings()).thenReturn(settings);
        return context;
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
