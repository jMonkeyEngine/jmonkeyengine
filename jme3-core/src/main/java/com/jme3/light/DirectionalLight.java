/*
 * Copyright (c) 2009-2012, 2015-2016 jMonkeyEngine
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

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;
import java.io.IOException;

/**
 * <code>DirectionalLight</code> is a light coming from a certain direction in world space. 
 * E.g sun or moon light.
 * <p>
 * Directional lights have no specific position in the scene, they always 
 * come from their direction regardless of where an object is placed.
 */
public class DirectionalLight extends Light {

    protected Vector3f direction = new Vector3f(0f, -1f, 0f);

    /**
     * Creates a DirectionalLight
     */
    public DirectionalLight() {
    }

    /**
     * Creates a DirectionalLight with the given direction
     * @param direction the light's direction
     */
    public DirectionalLight(Vector3f direction) {
        setDirection(direction);
    }

    /**
     * Creates a DirectionalLight with the given direction and the given color
     * @param direction the light's direction
     * @param color the light's color
     */
    public DirectionalLight(Vector3f direction, ColorRGBA color) {
        super(color);
        setDirection(direction);
    }

    @Override
    public void computeLastDistance(Spatial owner) {
        // directional lights are after ambient lights
        // but before all other lights.
        lastDistance = -1; 
    }

    /**
     * Returns the direction vector of the light.
     * 
     * @return The direction vector of the light.
     * 
     * @see DirectionalLight#setDirection(com.jme3.math.Vector3f) 
     */
    public Vector3f getDirection() {
        return direction;
    }

    /**
     * Sets the direction of the light.
     * <p>
     * Represents the direction the light is shining.
     * (1, 0, 0) would represent light shining in the +X direction.
     * 
     * @param dir the direction of the light.
     */
    public final void setDirection(Vector3f dir){
        direction.set(dir);
        if (!direction.isUnitVector()) {
            direction.normalizeLocal();
        }
    }

    @Override
    public boolean intersectsBox(BoundingBox box, TempVars vars) {
        return true;
    }
    
    @Override
    public boolean intersectsSphere(BoundingSphere sphere, TempVars vars) {
        return true;
    }

    @Override
    public boolean intersectsFrustum(Camera camera, TempVars vars) {
        return true;
    }
    
    @Override
    public Type getType() {
        return Type.Directional;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(direction, "direction", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        direction = (Vector3f) ic.readSavable("direction", null);
    }

    @Override
    public DirectionalLight clone() {
        DirectionalLight l = (DirectionalLight)super.clone();
        l.direction = direction.clone();
        return l;
    }
}
