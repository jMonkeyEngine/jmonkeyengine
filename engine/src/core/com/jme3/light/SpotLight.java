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
import com.jme3.export.*;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.io.IOException;

/**
 * Represents a spot light.
 * A spot light emmit a cone of light from a position and in a direction.
 * It can be used to fake torch lights or car's lights.
 * <p>
 * In addition to a position and a direction, spot lights also have a range which 
 * can be used to attenuate the influence of the light depending on the 
 * distance between the light and the effected object.
 * Also the angle of the cone can be tweaked by changing the spot inner angle and the spot outer angle.
 * the spot inner angle determin the cone of light where light has full influence.
 * the spot outer angle determin the cone global cone of light of the spot light.
 * the light intensity slowly decrease between the inner cone and the outer cone.
 *  @author Nehon
 */
public class SpotLight extends Light implements Savable {

    protected Vector3f position = new Vector3f();
    protected Vector3f direction = new Vector3f(0,-1,0);
    protected float spotInnerAngle = FastMath.QUARTER_PI / 8;
    protected float spotOuterAngle = FastMath.QUARTER_PI / 6;
    protected float spotRange = 100;
    protected float invSpotRange = 1 / 100;
    protected float packedAngleCos=0;

    public SpotLight() {
        super();
        computePackedCos();
    }

    private void computePackedCos() {
        float innerCos=FastMath.cos(spotInnerAngle);
        float outerCos=FastMath.cos(spotOuterAngle);
        packedAngleCos=(int)(innerCos*1000);
        packedAngleCos+=outerCos;
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

    public void setDirection(Vector3f direction) {
        this.direction.set(direction);
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f position) {
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
        if (spotRange != 0) {
            this.invSpotRange = 1 / spotRange;
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
     * This angle is the angle between the spot direction axis and the inner border of the cone of influence.
     * @param spotInnerAngle 
     */
    public void setSpotInnerAngle(float spotInnerAngle) {
        this.spotInnerAngle = spotInnerAngle;
        computePackedCos();
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
     * This angle is the angle between the spot direction axis and the outer border of the cone of influence.
     * this should be greater than the inner angle or the result will be unexpected.
     * @param spotOuterAngle 
     */
    public void setSpotOuterAngle(float spotOuterAngle) {
        this.spotOuterAngle = spotOuterAngle;
        computePackedCos();
    }

    /**
     * for internal use only
     * @return the cosines of the inner and outter angle packed in a float
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
        direction = (Vector3f) ic.readSavable("direction", new Vector3f());
        position = (Vector3f) ic.readSavable("position", new Vector3f());
        spotRange = ic.readFloat("spotRange", 100);
        if (spotRange != 0) {
            this.invSpotRange = 1 / spotRange;
        } else {
            this.invSpotRange = 0;
        }
    }
}
