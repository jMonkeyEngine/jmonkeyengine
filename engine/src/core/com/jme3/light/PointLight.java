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
package com.jme3.light;

import com.jme3.bounding.BoundingVolume;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.io.IOException;

/**
 * Represents a point light.
 * A point light emits light from a given position into all directions in space.
 * E.g a lamp or a bright effect. Point light positions are in world space.
 * <p>
 * In addition to a position, point lights also have a radius which 
 * can be used to attenuate the influence of the light depending on the 
 * distance between the light and the effected object.
 * 
 */
public class PointLight extends Light {

    protected Vector3f position = new Vector3f();
    protected float radius = 0;
    protected float invRadius = 0;

    @Override
    public void computeLastDistance(Spatial owner) {
        if (owner.getWorldBound() != null) {
            BoundingVolume bv = owner.getWorldBound();
            lastDistance = bv.distanceSquaredTo(position);
        } else {
            lastDistance = owner.getWorldTranslation().distanceSquared(position);
        }
    }

    /**
     * Returns the world space position of the light.
     * 
     * @return the world space position of the light.
     * 
     * @see PointLight#setPosition(com.jme3.math.Vector3f) 
     */
    public Vector3f getPosition() {
        return position;
    }

    /**
     * Set the world space position of the light.
     * 
     * @param position the world space position of the light.
     */
    public void setPosition(Vector3f position) {
        this.position.set(position);
    }

    /**
     * Returns the radius of the light influence. A radius of 0 means
     * the light has no attenuation.
     * 
     * @return the radius of the light
     */
    public float getRadius() {
        return radius;
    }

    /**
     * Set the radius of the light influence.
     * <p>
     * Setting a non-zero radius indicates the light should use attenuation.
     * If a pixel's distance to this light's position
     * is greater than the light's radius, then the pixel will not be
     * effected by this light, if the distance is less than the radius, then
     * the magnitude of the influence is equal to distance / radius.
     * 
     * @param radius the radius of the light influence.
     * 
     * @throws IllegalArgumentException If radius is negative
     */
    public void setRadius(float radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("Light radius cannot be negative");
        }
        this.radius = radius;
        if(radius!=0){
            this.invRadius = 1 / radius;
        }else{
            this.invRadius = 0;
        }
    }

    /**
     * for internal use only
     * @return the inverse of the radius
     */
    public float getInvRadius() {
        return invRadius;
    }

    @Override
    public Light.Type getType() {
        return Light.Type.Point;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(position, "position", null);
        oc.write(radius, "radius", 0f);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        position = (Vector3f) ic.readSavable("position", null);
        radius = ic.readFloat("radius", 0f);
        if(radius!=0){
            this.invRadius = 1 / radius;
        }else{
            this.invRadius = 0;
        }
    }
}
