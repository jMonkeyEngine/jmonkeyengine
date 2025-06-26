/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
package com.jme3.scene.shape;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * An ordinary (single holed) torus.
 * <p>
 * The torus is centered at the origin by default, but its position and
 * orientation can be transformed.
 *
 * @author Mark Powell
 * @version $Revision: 4131 $, $Date: 2009-03-19 16:15:28 -0400 (Thu, 19 Mar 2009) $
 */
public class Torus extends Mesh {

    /**
     * The number of samples around the circular cross-section of the torus.
     */
    private int circleSamples;
    /**
     * The number of samples along the major radius of the torus.
     */
    private int radialSamples;
    /**
     * The minor radius of the torus.
     */
    private float innerRadius;
    /**
     * The major radius of the torus.
     */
    private float outerRadius;

    /**
     * Serialization-only constructor. Do not use.
     */
    public Torus() {
    }

    /**
     * Constructs a new `Torus` mesh centered at the origin.
     *
     * @param circleSamples The number of samples around the circular cross-section.
     * Higher values result in a smoother tube.
     * @param radialSamples The number of samples along the major radius (around the hole).
     * Higher values result in a smoother overall torus shape.
     * @param innerRadius   The minor radius of the torus (radius of the circular cross-section).
     * @param outerRadius   The major radius of the torus (distance from the center of the hole to the center of the tube).
     * @throws IllegalArgumentException if `circleSamples` or `radialSamples` are less than 3,
     * or if `innerRadius` or `outerRadius` are negative.
     */
    public Torus(int circleSamples, int radialSamples, float innerRadius, float outerRadius) {
        super();
        updateGeometry(circleSamples, radialSamples, innerRadius, outerRadius);
    }

    private void setGeometryData() {
        // allocate vertices
        int vertCount = (circleSamples + 1) * (radialSamples + 1);
        FloatBuffer fpb = BufferUtils.createVector3Buffer(vertCount);
        setBuffer(Type.Position, 3, fpb);

        // allocate normals
        FloatBuffer fnb = BufferUtils.createVector3Buffer(vertCount);
        setBuffer(Type.Normal, 3, fnb);

        // allocate texture coordinates
        FloatBuffer ftb = BufferUtils.createVector2Buffer(vertCount);
        setBuffer(Type.TexCoord, 2, ftb);

        // generate geometry
        float inverseCircleSamples = 1.0f / circleSamples;
        float inverseRadialSamples = 1.0f / radialSamples;
        int i = 0;
        // generate the cylinder itself
        Vector3f radialAxis = new Vector3f();
        Vector3f torusMiddle = new Vector3f();
        Vector3f tempNormal = new Vector3f();

        for (int circleCount = 0; circleCount < circleSamples; circleCount++) {
            // compute center point on torus circle at specified angle
            float circleFraction = circleCount * inverseCircleSamples;
            float theta = FastMath.TWO_PI * circleFraction;
            float cosTheta = FastMath.cos(theta);
            float sinTheta = FastMath.sin(theta);

            // Calculate the center point of the current circular cross-section on the major radius
            radialAxis.set(cosTheta, sinTheta, 0);
            radialAxis.mult(outerRadius, torusMiddle);

            // compute slice vertices with duplication at end point
            int iSave = i;
            for (int radialCount = 0; radialCount < radialSamples; radialCount++) {
                float radialFraction = radialCount * inverseRadialSamples;
                // in [0,1)
                float phi = FastMath.TWO_PI * radialFraction;
                float cosPhi = FastMath.cos(phi);
                float sinPhi = FastMath.sin(phi);

                // Calculate normal vector
                tempNormal.set(radialAxis).multLocal(cosPhi);
                tempNormal.z += sinPhi;
                fnb.put(tempNormal.x).put(tempNormal.y).put(tempNormal.z);

                // Calculate vertex position
                tempNormal.multLocal(innerRadius).addLocal(torusMiddle);
                fpb.put(tempNormal.x).put(tempNormal.y).put(tempNormal.z);

                // Calculate texture coordinates
                ftb.put(radialFraction).put(circleFraction);
                i++;
            }

            BufferUtils.copyInternalVector3(fpb, iSave, i);
            BufferUtils.copyInternalVector3(fnb, iSave, i);

            ftb.put(1.0f).put(circleFraction);
            i++;
        }
    }

    private void setIndexData() {
        // Each quad forms two triangles, and there are radialSamples * circleSamples quads.
        int triCount = 2 * circleSamples * radialSamples;
        ShortBuffer indexBuffer = BufferUtils.createShortBuffer(3 * triCount);
        setBuffer(Type.Index, 3, indexBuffer);

        int currentQuadStartIndex = 0;
        for (int circleIter = 0; circleIter < circleSamples; circleIter++) {
            for (int radialIter = 0; radialIter < radialSamples; radialIter++) {
                int i0 = currentQuadStartIndex + radialIter;
                int i1 = i0 + 1;
                int i2 = i0 + (radialSamples + 1);
                int i3 = i2 + 1;

                // First triangle of the quad
                indexBuffer.put((short) i0);
                indexBuffer.put((short) i2);
                indexBuffer.put((short) i1);

                // Second triangle of the quad
                indexBuffer.put((short) i1);
                indexBuffer.put((short) i2);
                indexBuffer.put((short) i3);
            }
            currentQuadStartIndex += (radialSamples + 1);
        }
    }

    /**
     * Rebuilds the torus mesh based on a new set of parameters.
     * This method updates the internal parameters and then regenerates
     * the vertex data, index data, updates the bounding volume, and counts.
     *
     * @param circleSamples The number of samples around the circular cross-section.
     * @param radialSamples The number of samples along the major radius.
     * @param innerRadius   The minor radius of the torus.
     * @param outerRadius   The major radius of the torus.
     * @throws IllegalArgumentException if `circleSamples` or `radialSamples` are less than 3,
     * or if `innerRadius` or `outerRadius` are negative.
     */
    public void updateGeometry(int circleSamples, int radialSamples, float innerRadius, float outerRadius) {
        if (circleSamples < 3) {
            throw new IllegalArgumentException("circleSamples must be at least 3.");
        }
        if (radialSamples < 3) {
            throw new IllegalArgumentException("radialSamples must be at least 3.");
        }
        if (innerRadius < 0) {
            throw new IllegalArgumentException("innerRadius cannot be negative.");
        }
        if (outerRadius < 0) {
            throw new IllegalArgumentException("outerRadius cannot be negative.");
        }

        this.circleSamples = circleSamples;
        this.radialSamples = radialSamples;
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
        setGeometryData();
        setIndexData();
        updateBound();
        updateCounts();
    }

    /**
     * Returns the number of samples around the circular cross-section of the torus.
     *
     * @return The number of circle samples.
     */
    public int getCircleSamples() {
        return circleSamples;
    }

    /**
     * Returns the minor radius of the torus.
     *
     * @return The inner radius.
     */
    public float getInnerRadius() {
        return innerRadius;
    }

    /**
     * Returns the major radius of the torus.
     *
     * @return The outer radius.
     */
    public float getOuterRadius() {
        return outerRadius;
    }

    /**
     * Returns the number of samples along the major radius of the torus.
     *
     * @return The number of radial samples.
     */
    public int getRadialSamples() {
        return radialSamples;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(circleSamples, "circleSamples", 0);
        oc.write(radialSamples, "radialSamples", 0);
        oc.write(innerRadius, "innerRadius", 0);
        oc.write(outerRadius, "outerRadius", 0);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        circleSamples = ic.readInt("circleSamples", 0);
        radialSamples = ic.readInt("radialSamples", 0);
        innerRadius = ic.readFloat("innerRadius", 0);
        outerRadius = ic.readFloat("outerRadius", 0);
    }

}
