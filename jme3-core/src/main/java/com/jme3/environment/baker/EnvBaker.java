/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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

package com.jme3.environment.baker;

import java.util.function.Predicate;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.texture.TextureCubeMap;

/**
 * An environment baker. It bakes the environment.
 *
 * @author Riccardo Balbo
 */
public interface EnvBaker {
    /**
     * Bake the environment
     * 
     * @param scene
     *            The scene to bake
     * @param position
     *            The position of the camera
     * @param frustumNear
     *            The near frustum
     * @param frustumFar
     *            The far frustum
     * @param filter
     *            A filter to select which geometries to bake
     */
    public void bakeEnvironment(Spatial scene, Vector3f position, float frustumNear, float frustumFar, Predicate<Geometry> filter);

    /**
     * Get the environment map
     * 
     * @return The environment map
     */
    public TextureCubeMap getEnvMap();

    /**
     * Clean the environment baker This method should be called when the baker
     * is no longer needed It will clean up all the resources
     */
    public void clean();

    /**
     * Set if textures should be pulled from the GPU
     * 
     * @param v
     */
    public void setTexturePulling(boolean v);

    /**
     * Get if textures should be pulled from the GPU
     * 
     * @return
     */
    public boolean isTexturePulling();
}