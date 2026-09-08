/*
 * Copyright (c) 2009-2024 jMonkeyEngine
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
package com.jme3.input;

import com.jme3.math.Vector3f;

/**
 * Interface for camera collision detection used by {@link ChaseCamera}.
 * Implementations determine how the chase camera interacts with the scene
 * when there is geometry between the target and the camera.
 *
 * <p>A {@code CameraCollider} receives the target's world position and the
 * desired camera position, and may adjust the camera position in-place to
 * prevent it from clipping through geometry.</p>
 *
 * <p>The built-in implementation is {@link SceneCameraCollider}, which uses
 * ray-casting against one or more scene {@link com.jme3.scene.Node Nodes}.
 * Custom implementations can integrate with physics engines or any other
 * collision system.</p>
 *
 * @see ChaseCamera#setCameraCollider(CameraCollider)
 * @see SceneCameraCollider
 */
public interface CameraCollider {

    /**
     * Adjusts the camera position to avoid passing through scene geometry.
     *
     * <p>Implementations should test for obstructions between
     * {@code targetPosition} and {@code camPosition} and, if any are found,
     * update {@code camPosition} in-place so that the camera stays in front of
     * the obstruction.</p>
     *
     * @param targetPosition the world position of the chase camera's target
     *                       (read-only)
     * @param camPosition    the desired camera position before collision
     *                       adjustment; updated in-place with the adjusted
     *                       position if a collision is detected
     */
    void collide(Vector3f targetPosition, Vector3f camPosition);
}
