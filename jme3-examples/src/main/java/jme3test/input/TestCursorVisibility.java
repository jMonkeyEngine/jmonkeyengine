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
package jme3test.input;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.system.AppSettings;

/**
 * Demonstrates toggling cursor visibility while tracking its position.
 */
public class TestCursorVisibility extends SimpleApplication implements ActionListener {

    private static final String TOGGLE_CURSOR = "Toggle cursor mode";

    private BitmapText cursorStatus;

    public static void main(String[] args) {
        TestCursorVisibility app = new TestCursorVisibility();
        AppSettings settings = new AppSettings(true);
        settings.setX11PlatformPreferred(true);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setEnabled(false);
        viewPort.setBackgroundColor(ColorRGBA.DarkGray);
        inputManager.setCursorVisible(true);

        inputManager.addMapping(TOGGLE_CURSOR, new KeyTrigger(KeyInput.KEY_H));
        inputManager.addListener(this, TOGGLE_CURSOR);

        BitmapText instructions = new BitmapText(guiFont);
        instructions.setText("press H to toggle cursor mode");
        instructions.setLocalTranslation(10f, cam.getHeight() - 10f, 0f);
        guiNode.attachChild(instructions);

        cursorStatus = new BitmapText(guiFont);
        cursorStatus.setLocalTranslation(10f,
                instructions.getLocalTranslation().y - instructions.getLineHeight() * 2f, 0f);
        guiNode.attachChild(cursorStatus);
        updateCursorStatus();
    }

    @Override
    public void simpleUpdate(float tpf) {
        updateCursorStatus();
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (TOGGLE_CURSOR.equals(name) && isPressed) {
            inputManager.setCursorVisible(!inputManager.isCursorVisible());
            updateCursorStatus();
        }
    }

    private void updateCursorStatus() {
        Vector2f position = inputManager.getCursorPosition();
        String mode = inputManager.isCursorVisible() ? "visible" : "hidden (relative)";
        cursorStatus.setText("Cursor position: (" + position.x + ", " + position.y + ")\n"
                + "Cursor mode: " + mode);
    }
}
