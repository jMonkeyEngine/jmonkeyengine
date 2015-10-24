/*
 * Copyright (c) 2009-2012, 2015 jMonkeyEngine
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
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;

/**
 * An ambient light adds a constant color to the scene.
 * <p>
 * Ambient lights are unaffected by the surface normal, and are constant
 * regardless of the model's location. The material's ambient color is
 * multiplied by the ambient light color to get the final ambient color of
 * an object.
 * 
 * @author Kirill Vainer
 */
public class AmbientLight extends Light {

    public AmbientLight() {
    }

    public AmbientLight(ColorRGBA color) {
        super(color);
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
    public void computeLastDistance(Spatial owner) {
        // ambient lights must always be before directional lights.
        lastDistance = -2;
    }

    @Override
    public Type getType() {
        return Type.Ambient;
    }

}
