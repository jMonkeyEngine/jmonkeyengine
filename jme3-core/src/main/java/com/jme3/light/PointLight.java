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
import com.jme3.bounding.BoundingVolume;
import com.jme3.bounding.Intersection;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;
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

    /**
     * Creates a PointLight
     */
    public PointLight() {
    }

    /**
     * Creates a PointLight at the given position
     * @param position the position in world space
     */
    public PointLight(Vector3f position) {
        setPosition(position);
    }

    /**
     * Creates a PointLight at the given position and with the given color
     * @param position the position in world space
     * @param color the light color
     */
    public PointLight(Vector3f position, ColorRGBA color) {
        super(color);
        setPosition(position);
    }
    
    /**
     * Creates a PointLight at the given position, with the given color and the 
     * given radius
     * @param position the position in world space
     * @param color the light color
     * @param radius the light radius
     */
    public PointLight(Vector3f position, ColorRGBA color, float radius) {
        this(position, color);
        setRadius(radius);
    }
    
    /**
     * Creates a PointLight at the given position, with the given radius
     * @param position the position in world space
     * @param radius the light radius
     */
    public PointLight(Vector3f position, float radius) {
        this(position);
        setRadius(radius);
    }

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
    public final void setPosition(Vector3f position) {
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
    public final void setRadius(float radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("Light radius cannot be negative");
        }
        this.radius = radius;
        if (radius != 0f) {
            this.invRadius = 1f / radius;
        } else {
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
    public boolean intersectsBox(BoundingBox box, TempVars vars) {
        if (this.radius == 0) {
            return true;
        } else {
            // Sphere v. box collision
            return Intersection.intersect(box, position, radius);
        }
    }
    
    @Override
    public boolean intersectsSphere(BoundingSphere sphere, TempVars vars) {
        if (this.radius == 0) {
            return true;
        } else {
            // Sphere v. sphere collision
            return Intersection.intersect(sphere, position, radius);
        }
    }
    
    @Override
    public boolean intersectsFrustum(Camera camera, TempVars vars) {
        if (this.radius == 0) {
            return true;
        } else {
            for (int i = 5; i >= 0; i--) {
                if (camera.getWorldPlane(i).pseudoDistance(position) <= -radius) {
                    return false;
                }
            }
            return true;
        }
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

    @Override
    public PointLight clone() {
        PointLight p = (PointLight)super.clone();
        p.position = position.clone();
        return p;
    }
}
