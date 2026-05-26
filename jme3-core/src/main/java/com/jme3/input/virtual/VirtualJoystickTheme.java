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
package com.jme3.input.virtual;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * Virtual joystick look and feel
 */
public class VirtualJoystickTheme implements Savable {

    private static final String DEFAULT_FONT = "Interface/Fonts/Default.fnt";

    public enum TextureKey {
        BUTTON,
        BUTTON_WIDE,
        BUTTON_A_ICON,
        BUTTON_B_ICON,
        BUTTON_X_ICON,
        BUTTON_Y_ICON,
        BUTTON_BACK_ICON,
        BUTTON_START_ICON,
        STICK_PAD,
        STICK_NUB,
        DPAD_UP,
        DPAD_DOWN,
        DPAD_LEFT,
        DPAD_RIGHT
    }

    private final Map<TextureKey, String> textures = new EnumMap<>(TextureKey.class);
    private volatile String fontPath = DEFAULT_FONT;
    private transient volatile boolean updateNeeded = true;

    public VirtualJoystickTheme() {
        resetToDefault();
    }

    public synchronized final void resetToDefault() {
        textures.clear();
        fontPath = DEFAULT_FONT;
        textures.put(TextureKey.BUTTON, "Common/VirtualJoystick/button_circle.png");
        textures.put(TextureKey.BUTTON_WIDE, "Common/VirtualJoystick/button_circle_wide.png");
        textures.put(TextureKey.BUTTON_A_ICON, "Common/VirtualJoystick/icon_button_a.png");
        textures.put(TextureKey.BUTTON_B_ICON, "Common/VirtualJoystick/icon_button_b.png");
        textures.put(TextureKey.BUTTON_X_ICON, "Common/VirtualJoystick/icon_button_x.png");
        textures.put(TextureKey.BUTTON_Y_ICON, "Common/VirtualJoystick/icon_button_y.png");
        textures.put(TextureKey.BUTTON_BACK_ICON, "Common/VirtualJoystick/icon_menu.png");
        textures.put(TextureKey.BUTTON_START_ICON, "Common/VirtualJoystick/icon_star.png");
        textures.put(TextureKey.STICK_PAD, "Common/VirtualJoystick/joystick_circle_pad_a.png");
        textures.put(TextureKey.STICK_NUB, "Common/VirtualJoystick/joystick_circle_nub_a.png");
        textures.put(TextureKey.DPAD_UP, "Common/VirtualJoystick/dpad_element_north.png");
        textures.put(TextureKey.DPAD_DOWN, "Common/VirtualJoystick/dpad_element_south.png");
        textures.put(TextureKey.DPAD_LEFT, "Common/VirtualJoystick/dpad_element_west.png");
        textures.put(TextureKey.DPAD_RIGHT, "Common/VirtualJoystick/dpad_element_east.png");
        markUpdateNeeded();
    }

    public String getFontPath() {
        return fontPath;
    }

    public void setFontPath(String fontPath) {
        this.fontPath = fontPath;
        markUpdateNeeded();
    }

    public synchronized String getTexture(TextureKey key) {
        return textures.get(key);
    }

    public synchronized void setTexture(TextureKey key, String texturePath) {
        if (key == null) {
            throw new IllegalArgumentException("Texture key cannot be null.");
        }
        if (texturePath == null) {
            textures.remove(key);
        } else {
            textures.put(key, texturePath);
        }
        markUpdateNeeded();
    }

    boolean isUpdateNeeded() {
        return updateNeeded;
    }

    void markUpdateNeeded() {
        updateNeeded = true;
    }

    void clearUpdateNeeded() {
        updateNeeded = false;
    }

    @Override
    public synchronized void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(fontPath, "fontPath", DEFAULT_FONT);
        String[] keys = new String[textures.size()];
        int index = 0;
        for (TextureKey key : textures.keySet()) {
            keys[index++] = key.name();
        }
        capsule.write(keys, "keys", null);
        capsule.write(textures.values().toArray(new String[0]), "paths", null);
    }

    @Override
    public synchronized void read(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);
        fontPath = capsule.readString("fontPath", DEFAULT_FONT);
        String[] keys = capsule.readStringArray("keys", null);
        String[] paths = capsule.readStringArray("paths", null);
        textures.clear();
        if (keys != null && paths != null) {
            int count = Math.min(keys.length, paths.length);
            for (int i = 0; i < count; i++) {
                textures.put(TextureKey.valueOf(keys[i]), paths[i]);
            }
        }
        markUpdateNeeded();
    }
}
