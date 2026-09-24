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

import com.jme3.input.event.TouchEvent;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FlyByCameraTest {

    @Test
    void touchDragRotatesCamera() {
        Camera camera = new Camera(640, 480);
        FlyByCamera flyCam = new FlyByCamera(camera);
        Vector3f initialDirection = camera.getDirection().clone();

        flyCam.onTouch("FLYCAM_Touch",
                new TouchEvent(TouchEvent.Type.MOVE, 0f, 0f, 128f, 0f), 0f);

        assertFalse(initialDirection.equals(camera.getDirection()));
    }

    @Test
    void touchDragRotatesCameraWhenDragToRotateIsEnabled() {
        Camera camera = new Camera(640, 480);
        FlyByCamera flyCam = new FlyByCamera(camera);
        flyCam.setDragToRotate(true);
        Vector3f initialDirection = camera.getDirection().clone();

        flyCam.onTouch("FLYCAM_Touch",
                new TouchEvent(TouchEvent.Type.MOVE, 0f, 0f, 128f, 0f), 0f);

        assertFalse(initialDirection.equals(camera.getDirection()));
    }

    @Test
    void disabledFlyByCameraIgnoresTouchDrag() {
        Camera camera = new Camera(640, 480);
        FlyByCamera flyCam = new FlyByCamera(camera);
        flyCam.setEnabled(false);
        Vector3f initialDirection = camera.getDirection().clone();

        flyCam.onTouch("FLYCAM_Touch",
                new TouchEvent(TouchEvent.Type.MOVE, 0f, 0f, 128f, 0f), 0f);

        assertEquals(initialDirection, camera.getDirection());
    }
}
