/*
 * Copyright (c) 2019-2021 jMonkeyEngine
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
package com.jme3.test;

import com.jme3.anim.AnimComposer;
import com.jme3.anim.Joint;
import com.jme3.anim.SkinningControl;
import com.jme3.app.LegacyApplication;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.dummy.DummyKeyInput;
import com.jme3.input.dummy.DummyMouseInput;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.system.JmeSystem;
import com.jme3.system.NullRenderer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Test Suite to prevent regressions from previously fixed Core issues
 * @author Stephen Gold &lt;sgold@sonic.net&gt;
 */
public class PreventCoreIssueRegressions {

    /**
     * Test case for JME issue #1421: ScreenshotAppState never cleans up.
     */
    @Test
    public void testIssue1421() throws NoSuchFieldException, IllegalAccessException {
        ScreenshotAppState screenshotAppState = new ScreenshotAppState("./", "screen_shot");

        SimpleApplication app = new SimpleApplication(new AppState[]{}) {
            // Dummy application because SimpleApp is abstract
            @Override
            public void simpleInitApp() { }
        };

        app.setAssetManager(new DesktopAssetManager(true));
        Field f1 = LegacyApplication.class.getDeclaredField("inputManager");
        f1.setAccessible(true);
        f1.set(app, new InputManager(new DummyMouseInput(), new DummyKeyInput(), null, null));

        Field f2 = LegacyApplication.class.getDeclaredField("renderManager");
        f2.setAccessible(true);
        f2.set(app, new RenderManager(new NullRenderer()));

        app.getRenderManager().createPostView("null", new Camera(1, 1));

        assertTrue(app.getStateManager().attach(screenshotAppState));
        app.getStateManager().update(0f); // Causes SAS#initialize to be called

        // Confirm that the SceneProcessor is attached.
        List<ViewPort> vps = app.getRenderManager().getPostViews();
        assertEquals(1, vps.size());
        assertEquals(1, vps.get(0).getProcessors().size());
        assertTrue(app.getInputManager().hasMapping("ScreenShot")); // Confirm that KEY_SYSRQ is mapped.
        assertTrue(app.getStateManager().detach(screenshotAppState));

        app.getStateManager().update(0f); // Causes SAS#cleanup to be called

        // Check whether the SceneProcessor is still attached.
        assertEquals(0, vps.get(0).getProcessors().size());
        assertFalse(app.getInputManager().hasMapping("ScreenShot")); // Confirm that KEY_SYSRQ is unmapped.
    }

    /**
     * Test case for JME issue #1138: Elephant's legUp animation sets Joint translation to NaN.
     */
    @Test
    public void testIssue1138() {
        AssetManager am = JmeSystem.newAssetManager(PreventCoreIssueRegressions.class.getResource("/com/jme3/asset/Desktop.cfg"));
        Node cgModel = (Node)am.loadModel("Models/Elephant/Elephant.mesh.xml");
        cgModel.rotate(0f, -1f, 0f);
        cgModel.scale(0.04f);

        AnimComposer composer = cgModel.getControl(AnimComposer.class);
        composer.setCurrentAction("legUp");
        SkinningControl sControl = cgModel.getControl(SkinningControl.class);

        for (Joint joint : sControl.getArmature().getJointList()) {
            assertTrue(Vector3f.isValidVector(joint.getLocalTranslation()),
                    "Invalid translation for joint " + joint.getName());
        }

        cgModel.updateLogicalState(1.0f);
        cgModel.updateGeometricState();

        for (Joint joint : sControl.getArmature().getJointList()) {
            assertTrue(Vector3f.isValidVector(joint.getLocalTranslation()),
                    "Invalid translation for joint " + joint.getName());
        }
    }

    @Test
    public void testDeletedMappingClearsPressedKeyState() {
        TestKeyInput keys = new TestKeyInput();
        keys.initialize();
        DummyMouseInput mouse = new DummyMouseInput();
        mouse.initialize();
        InputManager inputManager = new InputManager(mouse, keys, null, null);
        List<String> calls = new ArrayList<>();
        KeyTrigger trigger = new KeyTrigger(KeyInput.KEY_D);

        inputManager.addMapping("walk", trigger);
        inputManager.addListener((AnalogListener) (name, value, tpf) -> calls.add("old"), "walk");
        keys.queue(key(KeyInput.KEY_D, true, keys.getInputTimeNanos()));
        inputManager.update(0.016f);

        inputManager.deleteMapping("walk");
        inputManager.addMapping("walk", trigger);
        inputManager.addListener((AnalogListener) (name, value, tpf) -> calls.add("new"), "walk");
        inputManager.update(0.016f);

        assertFalse(calls.contains("new"));
    }

    @Test
    public void testDeletedTriggerClearsPressedKeyState() {
        TestKeyInput keys = new TestKeyInput();
        keys.initialize();
        DummyMouseInput mouse = new DummyMouseInput();
        mouse.initialize();
        InputManager inputManager = new InputManager(mouse, keys, null, null);
        List<String> calls = new ArrayList<>();
        KeyTrigger trigger = new KeyTrigger(KeyInput.KEY_D);

        inputManager.addMapping("walk", trigger);
        inputManager.addListener((AnalogListener) (name, value, tpf) -> calls.add("old"), "walk");
        keys.queue(key(KeyInput.KEY_D, true, keys.getInputTimeNanos()));
        inputManager.update(0.016f);

        inputManager.deleteTrigger("walk", trigger);
        inputManager.addMapping("newWalk", trigger);
        inputManager.addListener((AnalogListener) (name, value, tpf) -> calls.add("new"), "newWalk");
        inputManager.update(0.016f);

        assertFalse(calls.contains("new"));
    }

    private static KeyInputEvent key(int keyCode, boolean pressed, long time) {
        KeyInputEvent event = new KeyInputEvent(keyCode, '\0', pressed, false);
        event.setTime(time);
        return event;
    }

    private static final class TestKeyInput extends DummyKeyInput {
        private final Queue<KeyInputEvent> events = new ArrayDeque<>();
        private RawInputListener listener;
        private long timeNanos = 1_000_000L;

        private void queue(KeyInputEvent event) {
            events.add(event);
        }

        @Override
        public void setInputListener(RawInputListener listener) {
            this.listener = listener;
        }

        @Override
        public void update() {
            super.update();
            while (!events.isEmpty()) {
                listener.onKeyEvent(events.remove());
            }
        }

        @Override
        public long getInputTimeNanos() {
            timeNanos += 16_000_000L;
            return timeNanos;
        }
    }
}
