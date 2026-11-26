/*
 * Copyright (c) 2026 jMonkeyEngine
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
package org.jmonkeyengine.screenshottests.android.input;

import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.input.InputManager;
import com.jme3.input.JoystickAxis;
import com.jme3.input.RawInputListenerAdapter;
import com.jme3.input.android.AndroidInputHandler26;
import com.jme3.input.android.AndroidJoyInput;
import com.jme3.input.dummy.DummyKeyInput;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.virtual.VirtualJoystick;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.texture.plugins.AndroidBufferImageLoader;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class VirtualJoystickTouchTest {
    private static final int LOGICAL_WIDTH = 450;
    private static final int LOGICAL_HEIGHT = 300;
    private static final String STICK_X = JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_X;
    private static final String STICK_Y = JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_Y;

    @Test
    @UiThreadTest
    public void dragStickWithLogicalSizeGui() {
        checkStickDrag(LOGICAL_WIDTH, LOGICAL_HEIGHT);
    }

    @Test
    @UiThreadTest
    public void dragStickWithIndependentlySizedGui() {
        checkStickDrag(1200, 800);
    }

    private static void checkStickDrag(int guiWidth, int guiHeight) {
        GLSurfaceView view = new GLSurfaceView(ApplicationProvider.getApplicationContext());
        view.layout(0, 0, 900, 600);

        AppSettings settings = new AppSettings(true);
        settings.setResolution(LOGICAL_WIDTH, LOGICAL_HEIGHT);
        settings.setUseJoysticks(true);
        settings.setUseAndroidSensorJoystick(false);
        settings.setVirtualJoystick(AppSettings.VIRTUAL_JOYSTICK_ENABLED);
        settings.setVirtualJoystickDefaultLayout(AppSettings.VIRTUAL_JOYSTICK_LAYOUT_XBOX);

        AndroidInputHandler26 handler = new AndroidInputHandler26();
        handler.setView(view);
        handler.loadSettings(settings);
        AndroidJoyInput joyInput = (AndroidJoyInput) handler.getJoyInput();
        DummyKeyInput keys = new DummyKeyInput();
        keys.initialize();
        joyInput.initialize();
        handler.getTouchInput().initialize();
        handler.getMouseInput().initialize();
        try {
            InputManager input = new InputManager(handler.getMouseInput(), keys, joyInput, handler.getTouchInput());
            VirtualJoystick joystick = (VirtualJoystick) input.getJoysticks()[0];
            Map<String, Float> axes = new HashMap<>();
            input.addRawInputListener(new RawInputListenerAdapter() {
                @Override
                public void onJoyAxisEvent(JoyAxisEvent event) {
                    if (event.getAxis().getJoystick() == joystick) {
                        axes.put(event.getAxis().getLogicalId(), event.getValue());
                    }
                }
            });

            DesktopAssetManager assets = new DesktopAssetManager();
            assets.registerLocator("/", ClasspathLocator.class);
            assets.registerLoader(J3MLoader.class, "j3md");
            assets.registerLoader(AndroidBufferImageLoader.class, "png");
            joystick.getTheme().setFontPath(null);
            Node gui = new Node("gui");
            joystick.updateVisuals(gui, assets, guiWidth, guiHeight, 0f);

            // Use the scene geometry as the reference for the stick's center and radius.
            Node stick = (Node) gui.getChild("Virtual Joystick " + STICK_X);
            assertNotNull("The left stick must be present in the GUI", stick);
            Spatial pad = stick.getChild(STICK_X);
            assertNotNull("The left stick must have a base picture", pad);
            Vector3f center = stick.getLocalTranslation();
            float centerX = center.x * view.getWidth() / guiWidth;
            float centerY = view.getHeight() - center.y * view.getHeight() / guiHeight;
            float halfRadius = pad.getLocalScale().y / 4f;

            long downTime = SystemClock.uptimeMillis();
            dispatchTouch(view, downTime, MotionEvent.ACTION_DOWN, centerX, centerY);
            assertEquals("A touch at the visual center must capture the stick",
                    1L, joyInput.getCapturedPointerMask());

            dispatchTouch(view, downTime, MotionEvent.ACTION_MOVE,
                    centerX + halfRadius * view.getWidth() / guiWidth,
                    centerY - halfRadius * view.getHeight() / guiHeight);
            input.update(1f / 60f);
            assertEquals("Dragging right must emit the corresponding X value",
                    0.5f, axes.getOrDefault(STICK_X, Float.NaN), 0.001f);
            assertEquals("Dragging up must emit the corresponding Y value",
                    -0.5f, axes.getOrDefault(STICK_Y, Float.NaN), 0.001f);

            axes.clear();
            dispatchTouch(view, downTime, MotionEvent.ACTION_UP, view.getWidth() / 2f, centerY);
            input.update(1f / 60f);
            assertEquals("Lifting the finger outside the stick must release it",
                    0L, joyInput.getCapturedPointerMask());
            assertEquals(0f, axes.getOrDefault(STICK_X, Float.NaN), 0f);
            assertEquals(0f, axes.getOrDefault(STICK_Y, Float.NaN), 0f);
        } finally {
            handler.setView(null);
            joyInput.destroy();
            handler.getTouchInput().destroy();
            handler.getMouseInput().destroy();
            keys.destroy();
        }
    }

    private static void dispatchTouch(GLSurfaceView view, long downTime, int action, float x, float y) {
        MotionEvent event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0);
        try {
            event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            view.dispatchTouchEvent(event);
        } finally {
            event.recycle();
        }
    }
}
