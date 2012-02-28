/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

package com.jme3.shader;

public enum UniformBinding {

    /**
     * The world matrix. Converts Model space to World space.
     * Type: mat4
     */
    WorldMatrix,

    /**
     * The view matrix. Converts World space to View space.
     * Type: mat4
     */
    ViewMatrix,

    /**
     * The projection matrix. Converts View space to Clip/Projection space.
     * Type: mat4
     */
    ProjectionMatrix,

    /**
     * The world view matrix. Converts Model space to View space.
     * Type: mat4
     */
    WorldViewMatrix,

    /**
     * The normal matrix. The inverse transpose of the worldview matrix.
     * Converts normals from model space to view space.
     * Type: mat3
     */
    NormalMatrix,

    /**
     * The world view projection matrix. Converts Model space to Clip/Projection
     * space.
     * Type: mat4
     */
    WorldViewProjectionMatrix,

    /**
     * The view projection matrix. Converts World space to Clip/Projection
     * space.
     * Type: mat4
     */
    ViewProjectionMatrix,

    /**
     * The world matrix inverse transpose. Converts a normals from Model space
     * to world space.
     * Type: mat3
     */
    WorldMatrixInverseTranspose,      



    WorldMatrixInverse,
    ViewMatrixInverse,
    ProjectionMatrixInverse,
    ViewProjectionMatrixInverse,
    WorldViewMatrixInverse,
    NormalMatrixInverse,
    WorldViewProjectionMatrixInverse,

    /**
     * Contains the four viewport parameters in this order:
     * X = Left,
     * Y = Top,
     * Z = Right,
     * W = Bottom.
     * Type: vec4
     */
    ViewPort,

    /**
     * The near and far values for the camera frustum.
     * X = Near
     * Y = Far.
     * Type: vec2
     */
    FrustumNearFar,
    
    /**
     * The width and height of the camera.
     * Type: vec2
     */
    Resolution,
    
    /**
     * The inverse of the resolution, 1/width and 1/height. 
     * Type: vec2
     */
    ResolutionInverse,

    /**
     * Aspect ratio of the resolution currently set. Width/Height.
     * Type: float
     */
    Aspect,

    /**
     * Camera position in world space.
     * Type: vec3
     */
    CameraPosition,

    /**
     * Direction of the camera.
     * Type: vec3
     */
    CameraDirection,

    /**
     * Left vector of the camera.
     * Type: vec3
     */
    CameraLeft,

    /**
     * Up vector of the camera.
     * Type: vec3
     */
    CameraUp,

    /**
     * Time in seconds since the application was started.
     * Type: float
     */
    Time,

    /**
     * Time in seconds that the last frame took.
     * Type: float
     */
    Tpf,

    /**
     * Frames per second.
     * Type: float
     */
    FrameRate,
}
