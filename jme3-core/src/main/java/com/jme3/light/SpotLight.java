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
import com.jme3.export.*;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Plane;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;
import java.io.IOException;

/**
 * Represents a spot light.
 * A spot light emits a cone of light from a position and in a direction.
 * It can be used to fake torch lights or car's lights.
 * <p>
 * In addition to a position and a direction, spot lights also have a range which 
 * can be used to attenuate the influence of the light depending on the 
 * distance between the light and the affected object.
 * Also the angle of the cone can be tweaked by changing the spot inner angle and the spot outer angle.
 * the spot inner angle determines the cone of light where light has full influence.
 * the spot outer angle determines the cone global cone of light of the spot light.
 * the light intensity slowly decreases between the inner cone and the outer cone.
 *  @author Nehon
 */
public class SpotLight extends Light {

    protected Vector3f position = new Vector3f();
    protected Vector3f direction = new Vector3f(0, -1, 0);
    protected float spotInnerAngle = FastMath.QUARTER_PI / 8;
    protected float spotOuterAngle = FastMath.QUARTER_PI / 6;
    protected float spotRange = 100;
    protected float invSpotRange = 1f / 100;
    protected float packedAngleCos = 0;
    
    protected float outerAngleCosSqr, outerAngleSinSqr;
    protected float outerAngleSinRcp, outerAngleSin, outerAngleCos;
    
    /**
     * Creates a SpotLight.
     */
    public SpotLight() {
        super();
        computeAngleParameters();
    }

    /**
     * Creates a SpotLight at the given position and with the given direction.
     * @param position the position in world space.
     * @param direction the direction of the light.
     */
    public SpotLight(Vector3f position, Vector3f direction) {
        this();
        setPosition(position);
        setDirection(direction);
    }
    
    /**
     * Creates a SpotLight at the given position, with the given direction, and the
     * given range.
     * @param position the position in world space.
     * @param direction the direction of the light.
     * @param range the spot light range
     */
    public SpotLight(Vector3f position, Vector3f direction, float range) {
        this();
        setPosition(position);
        setDirection(direction);
        setSpotRange(range);
    }

    /**
     * Creates a SpotLight at the given position, with the given direction and
     * the given color.
     * @param position the position in world space.
     * @param direction the direction of the light.
     * @param color the light's color.
     */
    public SpotLight(Vector3f position, Vector3f direction, ColorRGBA color) {
        super(color);
        computeAngleParameters();
        setPosition(position);
        setDirection(direction);
    }
    
    
    /**
     * Creates a SpotLight at the given position, with the given direction,
     * the given range and the given color.
     * @param position the position in world space.
     * @param direction the direction of the light.
     * @param range the spot light range
     * @param color the light's color.
     */
    public SpotLight(Vector3f position, Vector3f direction, float range, ColorRGBA color) {
        super(color);
        computeAngleParameters();
        setPosition(position);
        setDirection(direction);
        setSpotRange(range);
    }
    
    /**
     * Creates a SpotLight at the given position, with the given direction,
     * the given color and the given inner and outer angles 
     * (controls the falloff of the light)
     * 
     * @param position the position in world space.
     * @param direction the direction of the light.
     * @param range the spot light range
     * @param color the light's color.
     * @param innerAngle the inner angle of the spot light.
     * @param outerAngle the outer angle of the spot light.
     * 
     * @see SpotLight#setSpotInnerAngle(float) 
     * @see SpotLight#setSpotOuterAngle(float) 
     */
    public SpotLight(Vector3f position, Vector3f direction, float range, ColorRGBA color, float innerAngle, float outerAngle) {
        super(color);
        this.spotInnerAngle = innerAngle;
        this.spotOuterAngle = outerAngle;
        computeAngleParameters();
        setPosition(position);
        setDirection(direction);
        setSpotRange(range);
    }  
    

    private void computeAngleParameters() {
        float innerCos = FastMath.cos(spotInnerAngle);
        outerAngleCos = FastMath.cos(spotOuterAngle);
        packedAngleCos = (int) (innerCos * 1000);
        
        //due to approximations, very close angles can give the same cos
        //here we make sure outer cos is bellow inner cos.
        if (((int) packedAngleCos) == ((int) (outerAngleCos * 1000))) {
            outerAngleCos -= 0.001f;
        }
        packedAngleCos += outerAngleCos;

        if (packedAngleCos == 0.0f) {
            throw new IllegalArgumentException("Packed angle cosine is invalid");
        }
        
        // compute parameters needed for cone vs sphere check.
        outerAngleSin    = FastMath.sin(spotOuterAngle);
        outerAngleCosSqr = outerAngleCos * outerAngleCos;
        outerAngleSinSqr = outerAngleSin * outerAngleSin;
        outerAngleSinRcp = 1.0f / outerAngleSin;
    }

    @Override
    public boolean intersectsBox(BoundingBox box, TempVars vars) {
        if (this.spotRange > 0f) {
            // Check spot range first.
            // Sphere v. box collision
            if (!Intersection.intersect(box, position, spotRange)) {
                return false;
            }
        }
        
        Vector3f otherCenter = box.getCenter();
        Vector3f radVect = vars.vect4;
        radVect.set(box.getXExtent(), box.getYExtent(), box.getZExtent());
        float otherRadiusSquared = radVect.lengthSquared();
        float otherRadius = FastMath.sqrt(otherRadiusSquared);
        
        // Check if sphere is within spot angle.
        // Cone v. sphere collision.
        Vector3f E = direction.mult(otherRadius * outerAngleSinRcp, vars.vect1);
        Vector3f U = position.subtract(E, vars.vect2);
        Vector3f D = otherCenter.subtract(U, vars.vect3);

        float dsqr = D.dot(D);
        float e = direction.dot(D);

        if (e > 0f && e * e >= dsqr * outerAngleCosSqr) {
            D = otherCenter.subtract(position, vars.vect3);
            dsqr = D.dot(D);
            e = -direction.dot(D);

            if (e > 0f && e * e >= dsqr * outerAngleSinSqr) {
                return dsqr <= otherRadiusSquared;
            } else {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public boolean intersectsSphere(BoundingSphere sphere, TempVars vars) {
        if (this.spotRange > 0f) {
            // Check spot range first.
            // Sphere v. sphere collision
            if (!Intersection.intersect(sphere, position, spotRange)) {
                return false;
            }
        }

        float otherRadiusSquared = FastMath.sqr(sphere.getRadius());
        float otherRadius = sphere.getRadius();

        // Check if sphere is within spot angle.
        // Cone v. sphere collision.
        Vector3f E = direction.mult(otherRadius * outerAngleSinRcp, vars.vect1);
        Vector3f U = position.subtract(E, vars.vect2);
        Vector3f D = sphere.getCenter().subtract(U, vars.vect3);

        float dsqr = D.dot(D);
        float e = direction.dot(D);

        if (e > 0f && e * e >= dsqr * outerAngleCosSqr) {
            D = sphere.getCenter().subtract(position, vars.vect3);
            dsqr = D.dot(D);
            e = -direction.dot(D);

            if (e > 0f && e * e >= dsqr * outerAngleSinSqr) {
                return dsqr <= otherRadiusSquared;
            } else {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public boolean intersectsFrustum(Camera cam, TempVars vars) {
        if (spotRange == 0) {
            // The algorithm below does not support infinite spot range.
            return true;
        }
        Vector3f farPoint = vars.vect1.set(position).addLocal(vars.vect2.set(direction).multLocal(spotRange));
        for (int i = 5; i >= 0; i--) {
            //check origin against the plane
            Plane plane = cam.getWorldPlane(i);
            float dot = plane.pseudoDistance(position);
            if(dot < 0){                
                // outside, check the far point against the plane   
                dot = plane.pseudoDistance(farPoint);
                if(dot < 0){                   
                    // outside, check the projection of the far point along the normal of the plane to the base disc perimeter of the cone
                    //computing the radius of the base disc
                    float farRadius = (spotRange / outerAngleCos) * outerAngleSin;                    
                    //computing the projection direction : perpendicular to the light direction and coplanar with the direction vector and the normal vector
                    Vector3f perpDirection = vars.vect2.set(direction).crossLocal(plane.getNormal()).normalizeLocal().crossLocal(direction);
                    //projecting the far point on the base disc perimeter
                    Vector3f projectedPoint = vars.vect3.set(farPoint).addLocal(perpDirection.multLocal(farRadius));
                    //checking against the plane
                    dot = plane.pseudoDistance(projectedPoint);
                    if(dot < 0){                        
                        // Outside, the light can be culled
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    @Override
    protected void computeLastDistance(Spatial owner) {
        if (owner.getWorldBound() != null) {
            BoundingVolume bv = owner.getWorldBound();
            lastDistance = bv.distanceSquaredTo(position);
        } else {
            lastDistance = owner.getWorldTranslation().distanceSquared(position);
        }
    }

    @Override
    public Type getType() {
        return Type.Spot;
    }

    public Vector3f getDirection() {
        return direction;
    }

    public final void setDirection(Vector3f direction) {
        this.direction.set(direction);
    }

    public Vector3f getPosition() {
        return position;
    }

    public final void setPosition(Vector3f position) {
        this.position.set(position);
    }

    public float getSpotRange() {
        return spotRange;
    }

    /**
     * Set the range of the light influence.
     * <p>
     * Setting a non-zero range indicates the light should use attenuation.
     * If a pixel's distance to this light's position
     * is greater than the light's range, then the pixel will not be
     * effected by this light, if the distance is less than the range, then
     * the magnitude of the influence is equal to distance / range.
     * 
     * @param spotRange the range of the light influence.
     * 
     * @throws IllegalArgumentException If spotRange is negative
     */
    public void setSpotRange(float spotRange) {
        if (spotRange < 0) {
            throw new IllegalArgumentException("SpotLight range cannot be negative");
        }
        this.spotRange = spotRange;
        if (spotRange != 0f) {
            this.invSpotRange = 1f / spotRange;
        } else {
            this.invSpotRange = 0;
        }
    }

    /**
     * for internal use only
     * @return the inverse of the spot range
     */
    public float getInvSpotRange() {
        return invSpotRange;
    }

    /**
     * returns the spot inner angle
     * @return the spot inner angle
     */
    public float getSpotInnerAngle() {        
        return spotInnerAngle;
    }

    /**
     * Sets the inner angle of the cone of influence.
     * <p>
     * Must be between 0 and pi/2.
     * <p>
     * This angle is the angle between the spot direction axis and the inner border of the cone of influence.
     * @param spotInnerAngle 
     */
    public void setSpotInnerAngle(float spotInnerAngle) {
        if (spotInnerAngle < 0f || spotInnerAngle >= FastMath.HALF_PI) {
            throw new IllegalArgumentException("spot angle must be between 0 and pi/2");
        }
        this.spotInnerAngle = spotInnerAngle;
        computeAngleParameters();
    }

    /**
     * returns the spot outer angle
     * @return the spot outer angle
     */
    public float getSpotOuterAngle() {
        return spotOuterAngle;
    }

    /**
     * Sets the outer angle of the cone of influence.
     * <p>
     * Must be between 0 and pi/2.
     * <p>
     * This angle is the angle between the spot direction axis and the outer border of the cone of influence.
     * this should be greater than the inner angle or the result will be unexpected.
     * @param spotOuterAngle 
     */
    public void setSpotOuterAngle(float spotOuterAngle) {
        if (spotOuterAngle < 0f || spotOuterAngle >= FastMath.HALF_PI) {
            throw new IllegalArgumentException("spot angle must be between 0 and pi/2");
        }
        this.spotOuterAngle = spotOuterAngle;
        computeAngleParameters();
    }

    /**
     * for internal use only
     * @return the cosines of the inner and outer angle packed in a float
     */
    public float getPackedAngleCos() {
        return packedAngleCos;
    }
    
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(direction, "direction", new Vector3f());
        oc.write(position, "position", new Vector3f());
        oc.write(spotInnerAngle, "spotInnerAngle", FastMath.QUARTER_PI / 8);
        oc.write(spotOuterAngle, "spotOuterAngle", FastMath.QUARTER_PI / 6);
        oc.write(spotRange, "spotRange", 100);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        spotInnerAngle = ic.readFloat("spotInnerAngle", FastMath.QUARTER_PI / 8);
        spotOuterAngle = ic.readFloat("spotOuterAngle", FastMath.QUARTER_PI / 6);
        computeAngleParameters();
        direction = (Vector3f) ic.readSavable("direction", new Vector3f());
        position = (Vector3f) ic.readSavable("position", new Vector3f());
        spotRange = ic.readFloat("spotRange", 100);
        if (spotRange != 0) {
            this.invSpotRange = 1 / spotRange;
        } else {
            this.invSpotRange = 0;
        }
    }

    @Override
    public SpotLight clone() {
        SpotLight s = (SpotLight)super.clone();
        s.direction = direction.clone();
        s.position = position.clone();
        return s;
    }
}

