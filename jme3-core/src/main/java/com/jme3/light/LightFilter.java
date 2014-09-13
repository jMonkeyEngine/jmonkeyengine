/*
 * Copyright (c) 2009-2014 jMonkeyEngine
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
package com.jme3.light;

import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;

/**
 * <code>LightFilter</code> is used to determine which lights should be
 * rendered for a particular {@link Geometry} + {@link Camera} combination.
 * 
 * @author Kirill Vainer
 */
public interface LightFilter {
    
    /**
     * Sets the camera for which future filtering is to be done against in
     * {@link #filterLights(com.jme3.scene.Geometry, com.jme3.light.LightList)}.
     * 
     * @param camera The camera to perform light filtering against.
     */
    public void setCamera(Camera camera);
    
    /**
     * Determine which lights on the {@link Geometry#getWorldLightList() world
     * light list} are to be rendered.
     * <p>
     * The simplest implementation (e.g. one that performs no filtering) would
     * simply copy the contents of {@link Geometry#getWorldLightList()} to
     * {@code filteredLightList}.
     * <p>
     * An advanced implementation would determine if the light intersects
     * the {@link Geometry#getWorldBound() geometry's bounding volume} and if
     * the light intersects the frustum of the camera set in 
     * {@link #setCamera(com.jme3.renderer.Camera)} as well as sort the lights
     * according to some "influence" criteria - this will then provide
     * an optimal set of lights that should be used for rendering.
     * 
     * @param geometry The geometry for which the light filtering is performed.
     * @param filteredLightList The results are to be stored here. 
     */
    public void filterLights(Geometry geometry, LightList filteredLightList);
}
