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

package com.jme3.math;

import com.jme3.export.*;
import java.io.IOException;


/**
 * <code>Ring</code> defines a flat ring or disk within three dimensional
 * space that is specified via the ring's center point, an up vector, an inner
 * radius, and an outer radius.
 * 
 * @author Andrzej Kapolka
 * @author Joshua Slack
 */

public final class Ring implements Savable, Cloneable, java.io.Serializable {

    static final long serialVersionUID = 1;
    
    private Vector3f center, up;
    private float innerRadius, outerRadius;
    private transient static Vector3f b1 = new Vector3f(), b2 = new Vector3f();

    /**
     * Constructor creates a new <code>Ring</code> lying on the XZ plane,
     * centered at the origin, with an inner radius of zero and an outer radius
     * of one (a unit disk).
     */
    public Ring() {
        center = new Vector3f();
        up = Vector3f.UNIT_Y.clone();
        innerRadius = 0f;
        outerRadius = 1f;
    }

    /**
     * Constructor creates a new <code>Ring</code> with defined center point,
     * up vector, and inner and outer radii.
     * 
     * @param center
     *            the center of the ring.
     * @param up
     *            the unit up vector defining the ring's orientation.
     * @param innerRadius
     *            the ring's inner radius.
     * @param outerRadius
     *            the ring's outer radius.
     */
    public Ring(Vector3f center, Vector3f up, float innerRadius,
            float outerRadius) {
        this.center = center;
        this.up = up;
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
    }

    /**
     * <code>getCenter</code> returns the center of the ring.
     * 
     * @return the center of the ring.
     */
    public Vector3f getCenter() {
        return center;
    }

    /**
     * <code>setCenter</code> sets the center of the ring.
     * 
     * @param center
     *            the center of the ring.
     */
    public void setCenter(Vector3f center) {
        this.center = center;
    }

    /**
     * <code>getUp</code> returns the ring's up vector.
     * 
     * @return the ring's up vector.
     */
    public Vector3f getUp() {
        return up;
    }

    /**
     * <code>setUp</code> sets the ring's up vector.
     * 
     * @param up
     *            the ring's up vector.
     */
    public void setUp(Vector3f up) {
        this.up = up;
    }

    /**
     * <code>getInnerRadius</code> returns the ring's inner radius.
     * 
     * @return the ring's inner radius.
     */
    public float getInnerRadius() {
        return innerRadius;
    }

    /**
     * <code>setInnerRadius</code> sets the ring's inner radius.
     * 
     * @param innerRadius
     *            the ring's inner radius.
     */
    public void setInnerRadius(float innerRadius) {
        this.innerRadius = innerRadius;
    }

    /**
     * <code>getOuterRadius</code> returns the ring's outer radius.
     * 
     * @return the ring's outer radius.
     */
    public float getOuterRadius() {
        return outerRadius;
    }

    /**
     * <code>setOuterRadius</code> sets the ring's outer radius.
     * 
     * @param outerRadius
     *            the ring's outer radius.
     */
    public void setOuterRadius(float outerRadius) {
        this.outerRadius = outerRadius;
    }

    /**
     * 
     * <code>random</code> returns a random point within the ring.
     * 
     * @return a random point within the ring.
     */
    public Vector3f random() {
        return random(null);
    }

    /**
     * 
     * <code>random</code> returns a random point within the ring.
     * 
     * @param result Vector to store result in
     * @return a random point within the ring.
     */
    public Vector3f random(Vector3f result) {
        if (result == null) {
            result = new Vector3f();
        }
        
        // compute a random radius according to the ring area distribution
        float inner2 = innerRadius * innerRadius, outer2 = outerRadius
                * outerRadius, r = FastMath.sqrt(inner2
                + FastMath.nextRandomFloat() * (outer2 - inner2)), theta = FastMath
                .nextRandomFloat()
                * FastMath.TWO_PI;
        up.cross(Vector3f.UNIT_X, b1);
        if (b1.lengthSquared() < FastMath.FLT_EPSILON) {
            up.cross(Vector3f.UNIT_Y, b1);
        }
        b1.normalizeLocal();
        up.cross(b1, b2);
        result.set(b1).multLocal(r * FastMath.cos(theta)).addLocal(center);
        result.scaleAdd(r * FastMath.sin(theta), b2, result);
        return result;
    }

    public void write(JmeExporter e) throws IOException {
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(center, "center", Vector3f.ZERO);
        capsule.write(up, "up", Vector3f.UNIT_Z);
        capsule.write(innerRadius, "innerRadius", 0f);
        capsule.write(outerRadius, "outerRadius", 1f);
    }

    public void read(JmeImporter e) throws IOException {
        InputCapsule capsule = e.getCapsule(this);
        center = (Vector3f) capsule.readSavable("center",
                Vector3f.ZERO.clone());
        up = (Vector3f) capsule
                .readSavable("up", Vector3f.UNIT_Z.clone());
        innerRadius = capsule.readFloat("innerRadius", 0f);
        outerRadius = capsule.readFloat("outerRadius", 1f);
    }

    @Override
    public Ring clone() {
        try {
            Ring r = (Ring) super.clone();
            r.center = center.clone();
            r.up = up.clone();
            return r;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}