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
package com.jme3.input;

import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.dummy.DummyInput;
import com.jme3.input.dummy.DummyKeyInput;
import com.jme3.input.dummy.DummyMouseInput;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Queue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlyByCameraTest {

    private static final float EPSILON = 1e-6f;
    private static final String TOUCH_MAPPING = "FLYCAM_Touch";
    private static final int ACTIVE_POINTER = 23;
    private static final int SECONDARY_POINTER = 0;

    @ParameterizedTest
    @ValueSource(floats = {-256f, 256f})
    void horizontalTouchUsesMouseYawSignAndPixelScale(float deltaX) {
        Fixture fixture = new Fixture();

        fixture.frame(down(ACTIVE_POINTER), move(ACTIVE_POINTER, deltaX, 0f));

        assertDirection(fixture.camera, -deltaX / 1024f, 0f);
    }

    @ParameterizedTest
    @ValueSource(floats = {-192f, 192f})
    void verticalTouchUsesMousePitchSignAndPixelScale(float deltaY) {
        Fixture fixture = new Fixture();

        fixture.frame(down(ACTIVE_POINTER), move(ACTIVE_POINTER, 0f, deltaY));

        assertDirection(fixture.camera, 0f, deltaY / 1024f);
    }

    @ParameterizedTest
    @ValueSource(floats = {-192f, 192f})
    void invertYReversesPitchWithoutReversingYaw(float deltaY) {
        Fixture fixture = new Fixture();
        fixture.invertY();

        fixture.frame(down(ACTIVE_POINTER), move(ACTIVE_POINTER, 128f, deltaY));

        assertDirection(fixture.camera, -128f / 1024f, -deltaY / 1024f);
    }

    @ParameterizedTest
    @ValueSource(floats = {0f, 0.5f, 2.5f})
    void touchRotationRespectsRotationSpeed(float speed) {
        Fixture fixture = new Fixture();
        fixture.flyCam.setRotationSpeed(speed);

        fixture.frame(down(ACTIVE_POINTER), move(ACTIVE_POINTER, 128f, -64f));

        assertDirection(fixture.camera, -128f / 1024f * speed, -64f / 1024f * speed);
    }

    @ParameterizedTest
    @ValueSource(floats = {0.005f, 0.05f, 0.25f})
    void pixelRotationIsIndependentOfFrameTime(float tpf) {
        Fixture fixture = new Fixture();
        fixture.input.queue(down(ACTIVE_POINTER), move(ACTIVE_POINTER, 128f, 64f));

        fixture.manager.update(tpf);

        assertDirection(fixture.camera, -128f / 1024f, 64f / 1024f);
    }

    @Test
    void moveWithoutDownDoesNotRotateOrAcquirePointer() {
        Fixture fixture = new Fixture();
        Quaternion before = fixture.camera.getRotation().clone();

        fixture.frame(move(ACTIVE_POINTER, 128f, 64f), move(SECONDARY_POINTER, 128f, 64f));
        assertRotation(before, fixture.camera);

        fixture.frame(down(SECONDARY_POINTER), move(SECONDARY_POINTER, 128f, 0f));
        assertDirection(fixture.camera, -128f / 1024f, 0f);
    }

    @Test
    void firstDownLatchesNonzeroPointerAndIgnoresSecondaryDownAndMoves() {
        Fixture fixture = new Fixture();
        Quaternion before = fixture.camera.getRotation().clone();

        fixture.frame(down(ACTIVE_POINTER), down(SECONDARY_POINTER),
                move(SECONDARY_POINTER, 256f, 192f));
        assertRotation(before, fixture.camera);

        fixture.frame(move(ACTIVE_POINTER, 128f, 64f));
        assertDirection(fixture.camera, -128f / 1024f, 64f / 1024f);
    }

    @Test
    void consumedPointerZeroDownAllowsFirstUnconsumedNonzeroPointerToRotate() {
        Fixture fixture = new Fixture();
        fixture.manager.addRawInputListener(new RawInputListenerAdapter() {
            @Override
            public void onTouchEvent(TouchEvent event) {
                if (event.getPointerId() == SECONDARY_POINTER && event.getType() == TouchEvent.Type.DOWN) {
                    event.setConsumed();
                }
            }
        });
        Quaternion before = fixture.camera.getRotation().clone();

        fixture.frame(down(SECONDARY_POINTER), down(ACTIVE_POINTER),
                move(SECONDARY_POINTER, 256f, 192f));
        assertRotation(before, fixture.camera);

        fixture.frame(move(ACTIVE_POINTER, 128f, 64f));
        assertDirection(fixture.camera, -128f / 1024f, 64f / 1024f);
    }

    @Test
    void secondaryUpDoesNotReleaseActivePointer() {
        Fixture fixture = new Fixture();

        fixture.frame(down(ACTIVE_POINTER), down(SECONDARY_POINTER), up(SECONDARY_POINTER),
                move(ACTIVE_POINTER, 128f, 0f));

        assertDirection(fixture.camera, -128f / 1024f, 0f);
    }

    @Test
    void activeUpRequiresFreshDownBeforeAnyPointerCanRotate() {
        Fixture fixture = new Fixture();
        fixture.frame(down(ACTIVE_POINTER), down(SECONDARY_POINTER),
                move(ACTIVE_POINTER, 128f, 0f));
        Quaternion before = fixture.camera.getRotation().clone();

        fixture.frame(up(ACTIVE_POINTER), move(SECONDARY_POINTER, 256f, 192f),
                move(ACTIVE_POINTER, 256f, 192f));
        assertRotation(before, fixture.camera);

        fixture.frame(up(SECONDARY_POINTER), down(SECONDARY_POINTER),
                move(SECONDARY_POINTER, 128f, 0f));
        assertDirection(fixture.camera, -256f / 1024f, 0f);
    }

    @Test
    void releasedPointerIdCanBeReusedForANewDrag() {
        Fixture fixture = new Fixture();

        fixture.frame(down(ACTIVE_POINTER), move(ACTIVE_POINTER, 128f, 0f), up(ACTIVE_POINTER));
        fixture.frame(down(ACTIVE_POINTER), move(ACTIVE_POINTER, 128f, 0f));

        assertDirection(fixture.camera, -256f / 1024f, 0f);
    }

    @Test
    void zeroDeltaDoesNotRebuildCameraOrReleasePointer() {
        Fixture fixture = new Fixture();
        Quaternion before = fixture.camera.getRotation().clone();

        int rebuildsBefore = fixture.camera.frameChanges;
        fixture.frame(down(ACTIVE_POINTER), move(ACTIVE_POINTER, 0f, 0f));
        fixture.frame(move(ACTIVE_POINTER, -0f, 0f), move(ACTIVE_POINTER, 0f, -0f));
        assertRotation(before, fixture.camera);
        assertEquals(rebuildsBefore, fixture.camera.frameChanges, "zero deltas must not rebuild camera matrices");

        fixture.frame(move(ACTIVE_POINTER, 128f, 0f));
        assertDirection(fixture.camera, -128f / 1024f, 0f);
        assertTrue(fixture.camera.frameChanges > rebuildsBefore, "the active pointer must still rotate the camera");
    }

    @Test
    void unrelatedTouchGesturesDoNotRotateOrChangeActivePointer() {
        Fixture fixture = new Fixture();
        fixture.frame(down(ACTIVE_POINTER));
        Quaternion before = fixture.camera.getRotation().clone();

        for (TouchEvent.Type type : TouchEvent.Type.values()) {
            if (type != TouchEvent.Type.DOWN && type != TouchEvent.Type.MOVE
                    && type != TouchEvent.Type.UP) {
                fixture.frame(touch(type, ACTIVE_POINTER, 256f, 192f));
                assertRotation(before, fixture.camera);
            }
        }

        fixture.frame(move(ACTIVE_POINTER, 128f, 0f));
        assertDirection(fixture.camera, -128f / 1024f, 0f);
    }

    @Test
    void disableResetsActivePointerEvenWhenUpIsMissed() {
        Fixture fixture = new Fixture();
        fixture.frame(down(ACTIVE_POINTER), move(ACTIVE_POINTER, 128f, 0f));
        Quaternion before = fixture.camera.getRotation().clone();

        fixture.flyCam.setEnabled(false);
        assertFalse(fixture.manager.hasMapping(TOUCH_MAPPING));
        fixture.frame(move(ACTIVE_POINTER, 256f, 192f), down(SECONDARY_POINTER));
        assertRotation(before, fixture.camera);

        fixture.flyCam.setEnabled(true);
        assertTrue(fixture.manager.hasMapping(TOUCH_MAPPING));
        fixture.frame(move(ACTIVE_POINTER, 256f, 192f), move(SECONDARY_POINTER, 256f, 192f));
        assertRotation(before, fixture.camera);

        fixture.frame(down(ACTIVE_POINTER), move(ACTIVE_POINTER, 128f, 0f));
        assertDirection(fixture.camera, -256f / 1024f, 0f);
    }

    @Test
    void registeringWhileDisabledDoesNotAcquireTouchUntilEnabled() {
        Fixture fixture = new Fixture(false);
        fixture.flyCam.setEnabled(false);
        fixture.flyCam.registerWithInput(fixture.manager);
        Quaternion before = fixture.camera.getRotation().clone();

        fixture.frame(down(ACTIVE_POINTER), move(ACTIVE_POINTER, 256f, 192f));
        assertRotation(before, fixture.camera);

        fixture.flyCam.setEnabled(true);
        fixture.frame(move(ACTIVE_POINTER, 256f, 192f));
        assertRotation(before, fixture.camera);
        fixture.frame(down(ACTIVE_POINTER), move(ACTIVE_POINTER, 128f, 0f));
        assertDirection(fixture.camera, -128f / 1024f, 0f);
    }

    @Test
    void unregisterRemovesMappingAndReregisterRequiresFreshDown() {
        Fixture fixture = new Fixture();
        fixture.frame(down(ACTIVE_POINTER), move(ACTIVE_POINTER, 128f, 0f));
        Quaternion before = fixture.camera.getRotation().clone();

        fixture.flyCam.unregisterInput();
        assertFalse(fixture.manager.hasMapping(TOUCH_MAPPING));
        fixture.frame(move(ACTIVE_POINTER, 256f, 192f), down(SECONDARY_POINTER));
        assertRotation(before, fixture.camera);

        fixture.flyCam.registerWithInput(fixture.manager);
        assertTrue(fixture.manager.hasMapping(TOUCH_MAPPING));
        fixture.frame(move(ACTIVE_POINTER, 256f, 192f), move(SECONDARY_POINTER, 256f, 192f));
        assertRotation(before, fixture.camera);

        fixture.frame(down(ACTIVE_POINTER), move(ACTIVE_POINTER, 128f, 0f));
        assertDirection(fixture.camera, -256f / 1024f, 0f);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void nativeTouchRotatesWithoutMouseButtonInEitherDragMode(boolean dragToRotate) {
        Fixture fixture = new Fixture();
        fixture.flyCam.setDragToRotate(dragToRotate);

        fixture.frame(down(ACTIVE_POINTER), move(ACTIVE_POINTER, 128f, 64f));

        assertDirection(fixture.camera, -128f / 1024f, 64f / 1024f);
    }

    @Test
    void nativeTouchDoesNotBypassDragGateForLaterMouseMotion() {
        Fixture fixture = new Fixture();
        fixture.flyCam.setDragToRotate(true);
        fixture.frame(down(ACTIVE_POINTER), move(ACTIVE_POINTER, 128f, 64f));
        Quaternion before = fixture.camera.getRotation().clone();

        fixture.frame(mouseMove(256, 192));
        assertRotation(before, fixture.camera);

        fixture.frame(mouseButton(true), mouseMove(128, 0), mouseButton(false));
        assertDirection(fixture.camera, -256f / 1024f, 64f / 1024f);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void nativeTouchMatchesMouseMotionForBothAxes(boolean invertY) {
        Fixture nativeTouch = new Fixture();
        Fixture mouse = new Fixture();
        nativeTouch.flyCam.setRotationSpeed(1.75f);
        mouse.flyCam.setRotationSpeed(1.75f);
        if (invertY) {
            nativeTouch.invertY();
            mouse.invertY();
        }

        nativeTouch.frame(down(ACTIVE_POINTER));
        int[][] deltas = {{128, 64}, {-64, 96}, {32, -128}, {-96, -32}};
        for (int[] delta : deltas) {
            nativeTouch.frame(move(ACTIVE_POINTER, delta[0], delta[1]));
            mouse.frame(mouseMove(delta[0], delta[1]));
            assertRotation(mouse.camera.getRotation(), nativeTouch.camera);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void emulatedMouseAndNativeMoveTogetherRotateOnlyOnce(boolean dragToRotate) {
        Fixture fixture = new Fixture();
        fixture.flyCam.setDragToRotate(dragToRotate);
        fixture.manager.setSimulateMouse(true);

        fixture.frame(down(ACTIVE_POINTER), mouseButton(true),
                move(ACTIVE_POINTER, 128f, 64f), mouseMove(128, 64));

        assertDirection(fixture.camera, -128f / 1024f, 64f / 1024f);
    }

    @Test
    void nativeMoveIsSuppressedWhenMouseSimulationIsEnabled() {
        Fixture fixture = new Fixture();
        fixture.manager.setSimulateMouse(true);
        Quaternion before = fixture.camera.getRotation().clone();

        fixture.frame(down(ACTIVE_POINTER), move(ACTIVE_POINTER, 256f, 192f));

        assertRotation(before, fixture.camera);
    }

    @Test
    void downDuringMouseSimulationStillLatchesFirstPointer() {
        Fixture fixture = new Fixture();
        fixture.manager.setSimulateMouse(true);
        fixture.frame(down(ACTIVE_POINTER), down(SECONDARY_POINTER));
        Quaternion before = fixture.camera.getRotation().clone();

        fixture.manager.setSimulateMouse(false);
        fixture.frame(move(SECONDARY_POINTER, 256f, 192f));
        assertRotation(before, fixture.camera);
        fixture.frame(move(ACTIVE_POINTER, 128f, 0f));
        assertDirection(fixture.camera, -128f / 1024f, 0f);
    }

    @Test
    void upDuringMouseSimulationClearsPointerWithoutPromotingRemainingFinger() {
        Fixture fixture = new Fixture();
        fixture.frame(down(ACTIVE_POINTER), down(SECONDARY_POINTER));
        fixture.manager.setSimulateMouse(true);
        fixture.frame(up(ACTIVE_POINTER));
        Quaternion before = fixture.camera.getRotation().clone();

        fixture.manager.setSimulateMouse(false);
        fixture.frame(move(ACTIVE_POINTER, 256f, 192f), move(SECONDARY_POINTER, 256f, 192f));
        assertRotation(before, fixture.camera);

        fixture.frame(up(SECONDARY_POINTER), down(SECONDARY_POINTER),
                move(SECONDARY_POINTER, 128f, 0f));
        assertDirection(fixture.camera, -128f / 1024f, 0f);
    }

    @Test
    void secondaryLifecycleDuringMouseSimulationDoesNotStealActivePointer() {
        Fixture fixture = new Fixture();
        fixture.frame(down(ACTIVE_POINTER));
        fixture.manager.setSimulateMouse(true);
        fixture.frame(down(SECONDARY_POINTER), move(SECONDARY_POINTER, 256f, 192f),
                up(SECONDARY_POINTER));

        fixture.manager.setSimulateMouse(false);
        fixture.frame(move(ACTIVE_POINTER, 128f, 0f));

        assertDirection(fixture.camera, -128f / 1024f, 0f);
    }

    @Test
    void activeDragSurvivesSwitchingToMouseSimulationAndBack() {
        Fixture fixture = new Fixture();
        fixture.frame(down(ACTIVE_POINTER), move(ACTIVE_POINTER, 128f, 0f));
        fixture.manager.setSimulateMouse(true);
        fixture.frame(move(ACTIVE_POINTER, 128f, 0f), mouseMove(128, 0));
        assertDirection(fixture.camera, -256f / 1024f, 0f);

        fixture.manager.setSimulateMouse(false);
        fixture.frame(move(ACTIVE_POINTER, 128f, 0f));

        assertDirection(fixture.camera, -384f / 1024f, 0f);
    }

    private static void assertDirection(Camera camera, float yaw, float pitch) {
        // The default camera faces +Z. Positive touch X turns toward -X,
        // while positive touch Y turns toward +Y, just like mouse deltas.
        assertEquals(Math.sin(yaw) * Math.cos(pitch), camera.getDirection().x, EPSILON, "direction X");
        assertEquals(Math.sin(pitch), camera.getDirection().y, EPSILON, "direction Y");
        assertEquals(Math.cos(yaw) * Math.cos(pitch), camera.getDirection().z, EPSILON, "direction Z");
        assertEquals(1f, camera.getDirection().length(), EPSILON, "unit direction");
    }

    private static void assertRotation(Quaternion expected, Camera camera) {
        assertVector(expected.mult(Vector3f.UNIT_X), camera.getLeft());
        assertVector(expected.mult(Vector3f.UNIT_Y), camera.getUp());
        assertVector(expected.mult(Vector3f.UNIT_Z), camera.getDirection());
    }

    private static void assertVector(Vector3f expected, Vector3f actual) {
        assertEquals(expected.x, actual.x, EPSILON, "axis X");
        assertEquals(expected.y, actual.y, EPSILON, "axis Y");
        assertEquals(expected.z, actual.z, EPSILON, "axis Z");
    }

    private static TouchEvent down(int pointer) {
        return touch(TouchEvent.Type.DOWN, pointer, 0f, 0f);
    }

    private static TouchEvent move(int pointer, float deltaX, float deltaY) {
        return touch(TouchEvent.Type.MOVE, pointer, deltaX, deltaY);
    }

    private static TouchEvent up(int pointer) {
        return touch(TouchEvent.Type.UP, pointer, 0f, 0f);
    }

    private static TouchEvent touch(TouchEvent.Type type, int pointer, float deltaX, float deltaY) {
        TouchEvent event = new TouchEvent(type, 320f, 240f, deltaX, deltaY);
        event.setPointerId(pointer);
        return event;
    }

    private static MouseMotionEvent mouseMove(int deltaX, int deltaY) {
        return new MouseMotionEvent(320, 240, deltaX, deltaY, 0, 0);
    }

    private static MouseButtonEvent mouseButton(boolean pressed) {
        return new MouseButtonEvent(MouseInput.BUTTON_LEFT, pressed, 320, 240);
    }

    private static class TrackingCamera extends Camera {

        private int frameChanges;

        TrackingCamera() {
            super(640, 480);
        }

        @Override
        public void onFrameChange() {
            ++frameChanges;
            super.onFrameChange();
        }
    }

    private static class Fixture {

        final TrackingCamera camera = new TrackingCamera();
        final FlyByCamera flyCam = new FlyByCamera(camera);
        final QueuedTouchInput input = new QueuedTouchInput();
        final InputManager manager;

        Fixture() {
            this(true);
        }

        Fixture(boolean register) {
            DummyMouseInput mouse = new DummyMouseInput();
            DummyKeyInput keys = new DummyKeyInput();
            mouse.initialize();
            keys.initialize();
            input.initialize();
            manager = new InputManager(mouse, keys, null, input);
            if (register) {
                flyCam.registerWithInput(manager);
            }
        }

        void frame(InputEvent... events) {
            input.queue(events);
            manager.update(1f / 60f);
        }

        void invertY() {
            manager.addMapping(CameraInput.FLYCAM_INVERTY, new KeyTrigger(KeyInput.KEY_I));
            frame(new KeyInputEvent(KeyInput.KEY_I, 'i', true, false),
                    new KeyInputEvent(KeyInput.KEY_I, 'i', false, false));
        }
    }

    /**
     * Delivers raw events only while InputManager is updating the backend.
     * Simulated mouse events are explicit so tests can exercise native-only,
     * mouse-only, and paired delivery without duplicating production logic.
     */
    private static class QueuedTouchInput extends DummyInput implements TouchInput {

        private final Queue<InputEvent> events = new ArrayDeque<>();
        private RawInputListener listener;
        private boolean simulateMouse;
        private boolean simulateKeyboard;

        void queue(InputEvent... queuedEvents) {
            Collections.addAll(events, queuedEvents);
        }

        @Override
        public void setInputListener(RawInputListener listener) {
            this.listener = listener;
        }

        @Override
        public void update() {
            super.update();
            while (!events.isEmpty()) {
                InputEvent event = events.remove();
                event.setTime(getInputTimeNanos());
                if (event instanceof TouchEvent) {
                    listener.onTouchEvent((TouchEvent) event);
                } else if (event instanceof MouseMotionEvent) {
                    listener.onMouseMotionEvent((MouseMotionEvent) event);
                } else if (event instanceof MouseButtonEvent) {
                    listener.onMouseButtonEvent((MouseButtonEvent) event);
                } else if (event instanceof KeyInputEvent) {
                    listener.onKeyEvent((KeyInputEvent) event);
                } else {
                    throw new IllegalArgumentException("Unsupported queued input event: " + event);
                }
            }
        }

        @Override
        public void setSimulateMouse(boolean simulate) {
            simulateMouse = simulate;
        }

        @Override
        public boolean isSimulateMouse() {
            return simulateMouse;
        }

        @Override
        public void setSimulateKeyboard(boolean simulate) {
            simulateKeyboard = simulate;
        }

        @Override
        public boolean isSimulateKeyboard() {
            return simulateKeyboard;
        }

        @Override
        public void setOmitHistoricEvents(boolean omit) {
            // This backend has no historical events.
        }
    }
}
