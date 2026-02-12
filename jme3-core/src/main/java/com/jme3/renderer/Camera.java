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
package com.jme3.renderer;

import com.jme3.bounding.BoundingVolume;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;
import com.jme3.vulkan.util.SceneStack;

import java.util.function.Consumer;

/**
 * A standalone, purely mathematical class for doing camera-related computations.
 *
 * <p>Given input data such as location, orientation (direction, left, up),
 * and viewport settings, it can compute data necessary to render objects
 * with the graphics library. Two matrices are generated, the view matrix
 * transforms objects from world space into eye space, while the projection
 * matrix transforms objects from eye space into clip space.</p>
 *
 * <p>Another purpose of the camera class is to do frustum culling operations,
 * defined by six planes which define a 3D frustum shape, it is possible to
 * test if an object bounded by a mathematically defined volume is inside
 * the camera frustum, and thus to avoid rendering objects that are outside
 * the frustum
 * </p>
 *
 * A positional entity which defines how geometry is transformed from
 * world space to projection space for rasterization.
 *
 * <p>The primary function of a camera is to produce view and projection transform
 * matrices necessary to perform GPU rasterization. </p>
 *
 * @author Mark Powell
 * @author Joshua Slack
 */
public interface Camera {

    /**
     * The result of a culling check operation.
     * see {@link #contains(BoundingVolume)}
     */
    enum FrustumIntersect {
        /**
         * Defines a constant assigned to spatials that are completely outside
         * of this camera's view frustum.
         */
        Outside,
        /**
         * Defines a constant assigned to spatials that are completely inside
         * the camera's view frustum.
         */
        Inside,
        /**
         * Defines a constant assigned to spatials that are intersecting one of
         * the six planes that define the view frustum.
         */
        Intersects,
    }

    void setLocation(float x, float y, float z);

    Vector3f getLocation();

    void setRotation(Quaternion rotation);

    Quaternion getRotation();

    Matrix4f getViewMatrix();

    Matrix4f getProjectionMatrix();

    Matrix4f getViewProjectionMatrix();

    FrustumIntersect contains(BoundingVolume volume);

    SceneStack<Camera.FrustumIntersect> createCullStack();

    void setNearDistance(float near);

    float getNearDistance();

    void setFarDistance(float far);

    float getFarDistance();

    default void setLocation(Vector3f location) {
        setLocation(location.x, location.y, location.z);
    }

    default void lookAtDirection(Vector3f direction, Vector3f up) {
        setRotation(getRotation().lookAt(direction, up));
    }

    default void lookAt(Vector3f position, Vector3f up) {
        TempVars vars = TempVars.get();
        position.subtract(getLocation(), vars.vect1).normalizeLocal();
        lookAtDirection(vars.vect1, up);
        vars.release();
    }

    default void setAxes(Vector3f left, Vector3f up, Vector3f direction) {
        setRotation(getRotation().fromAxes(left, up, direction));
    }

    default void setFrame(Vector3f location, Quaternion rotation) {
        setLocation(location.x, location.y, location.z);
        setRotation(rotation);
    }

    default Vector3f getLocation(Vector3f store) {
        return Vector3f.storage(store).set(getLocation());
    }

    default Quaternion getRotation(Quaternion store) {
        return Quaternion.storage(store).set(getRotation());
    }

    default Vector3f getDirection() {
        return getDirection(null);
    }

    default Vector3f getDirection(Vector3f store) {
        return getRotation().getRotationColumn(2, store);
    }

    default Vector3f getLeft() {
        return getLeft(null);
    }

    default Vector3f getLeft(Vector3f store) {
        return getRotation().getRotationColumn(0, store);
    }

    default Vector3f getUp() {
        return getUp(null);
    }

    default Vector3f getUp(Vector3f store) {
        return getRotation().getRotationColumn(1, store);
    }

}
