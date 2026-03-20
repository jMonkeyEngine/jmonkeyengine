/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.input.lwjgl;

import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.system.lwjgl.LwjglWindow;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.logging.Logger;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_KeyboardEvent;

import static org.lwjgl.sdl.SDLKeyboard.*;
import static org.lwjgl.sdl.SDLScancode.*;
import static org.lwjgl.sdl.SDLEvents.*;
import static org.lwjgl.sdl.SDLTimer.*;

/**
 * SDL implementation of {@link KeyInput}.
 */
public class SdlKeyInput implements KeyInput {

    private static final Logger LOGGER = Logger.getLogger(SdlKeyInput.class.getName());

    private final Queue<KeyInputEvent> keyInputEvents = new ArrayDeque<>();
    private final LwjglWindow context;

    private RawInputListener listener;
    private boolean initialized;

    public SdlKeyInput(final LwjglWindow context) {
        this.context = context;
    }

    @Override
    public void initialize() {
        if (!context.isRenderable()) {
            return;
        }
        // Text input events are delivered through SDL_EVENT_TEXT_INPUT.
        SDL_StartTextInput(context.getWindowHandle());
        initialized = true;
        LOGGER.fine("SDL keyboard created.");
    }

    public void resetContext() {
        if (!context.isRenderable()) {
            return;
        }
        SDL_StartTextInput(context.getWindowHandle());
    }

    public void onSDLEvent(SDL_Event event) {
        final int type = event.type();
        if (type == SDL_EVENT_KEY_DOWN || type == SDL_EVENT_KEY_UP) {
            final SDL_KeyboardEvent key = event.key();
            if (key.windowID() != context.getWindowId()) {
                return;
            }

            final int jmeKey = SdlKeyMap.toJmeKeyCode(key.scancode());
            final KeyInputEvent keyEvent = new KeyInputEvent(jmeKey, '\0', key.down(), key.repeat());
            keyEvent.setTime(key.timestamp());
            keyInputEvents.add(keyEvent);
            return;
        }

        if (type == SDL_EVENT_TEXT_INPUT) {
            if (event.text().windowID() != context.getWindowId()) {
                return;
            }

            final String text = event.text().textString();
            if (text == null || text.isEmpty()) {
                return;
            }

            for (int i = 0; i < text.length(); i++) {
                final char keyChar = text.charAt(i);
                final long time = event.text().timestamp();

                KeyInputEvent pressed = new KeyInputEvent(KeyInput.KEY_UNKNOWN, keyChar, true, false);
                pressed.setTime(time);
                keyInputEvents.add(pressed);

                KeyInputEvent released = new KeyInputEvent(KeyInput.KEY_UNKNOWN, keyChar, false, false);
                released.setTime(time);
                keyInputEvents.add(released);
            }
        }
    }

    @Override
    public String getKeyName(int jmeKey) {
        int sdlScancode = SdlKeyMap.fromJmeKeyCode(jmeKey);
        if (sdlScancode == SDL_SCANCODE_UNKNOWN) {
            return null;
        }
        return SDL_GetScancodeName(sdlScancode);
    }

    public int getKeyCount() {
        return SDL_SCANCODE_COUNT;
    }

    @Override
    public void update() {
        if (!context.isRenderable() || listener == null) {
            return;
        }
        while (!keyInputEvents.isEmpty()) {
            listener.onKeyEvent(keyInputEvents.poll());
        }
    }

    @Override
    public void destroy() {
        if (context.isRenderable()) {
            SDL_StopTextInput(context.getWindowHandle());
        }
        keyInputEvents.clear();
        initialized = false;
        LOGGER.fine("SDL keyboard destroyed.");
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void setInputListener(RawInputListener listener) {
        this.listener = listener;
    }

    @Override
    public long getInputTimeNanos() {
        return SDL_GetTicksNS();
    }
}
