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

// $Id: Torus.java 4131 2009-03-19 20:15:28Z blaine.dev $
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
 * The center is by default the origin.
 * 
 * @author Mark Powell
 * @version $Revision: 4131 $, $Date: 2009-03-19 16:15:28 -0400 (Thu, 19 Mar 2009) $
 */
public class Torus extends Mesh {

    private int circleSamples;

    private int radialSamples;

    private float innerRadius;

    private float outerRadius;

    public Torus() {
    }

    /**
     * Constructs a new Torus. Center is the origin, but the Torus may be
     * transformed.
     * 
     * @param circleSamples
     *            The number of samples along the circles.
     * @param radialSamples
     *            The number of samples along the radial.
     * @param innerRadius
     *            The radius of the inner begining of the Torus.
     * @param outerRadius
     *            The radius of the outter end of the Torus.
     */
    public Torus(int circleSamples, int radialSamples,
            float innerRadius, float outerRadius) {
        super();
        updateGeometry(circleSamples, radialSamples, innerRadius, outerRadius);
    }

    public int getCircleSamples() {
        return circleSamples;
    }

    public float getInnerRadius() {
        return innerRadius;
    }

    public float getOuterRadius() {
        return outerRadius;
    }

    public int getRadialSamples() {
        return radialSamples;
    }

    @Override
    public void read(JmeImporter e) throws IOException {
        super.read(e);
        InputCapsule capsule = e.getCapsule(this);
        circleSamples = capsule.readInt("circleSamples", 0);
        radialSamples = capsule.readInt("radialSamples", 0);
        innerRadius = capsule.readFloat("innerRadius", 0);
        outerRadius = capsule.readFloat("outerRaidus", 0);
    }

    private void setGeometryData() {
        // allocate vertices
        int vertCount = (circleSamples + 1) * (radialSamples + 1);
        FloatBuffer fpb = BufferUtils.createVector3Buffer(vertCount);
        setBuffer(Type.Position, 3, fpb);

        // allocate normals if requested
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
        Vector3f radialAxis = new Vector3f(), torusMiddle = new Vector3f(), tempNormal = new Vector3f();
        for (int circleCount = 0; circleCount < circleSamples; circleCount++) {
            // compute center point on torus circle at specified angle
            float circleFraction = circleCount * inverseCircleSamples;
            float theta = FastMath.TWO_PI * circleFraction;
            float cosTheta = FastMath.cos(theta);
            float sinTheta = FastMath.sin(theta);
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
                tempNormal.set(radialAxis).multLocal(cosPhi);
                tempNormal.z += sinPhi;
                fnb.put(tempNormal.x).put(tempNormal.y).put(
                        tempNormal.z);
       
                tempNormal.multLocal(innerRadius).addLocal(torusMiddle);
                fpb.put(tempNormal.x).put(tempNormal.y).put(
                        tempNormal.z);

                ftb.put(radialFraction).put(circleFraction);
                i++;
            }

            BufferUtils.copyInternalVector3(fpb, iSave, i);
            BufferUtils.copyInternalVector3(fnb, iSave, i);

            ftb.put(1.0f).put(circleFraction);

            i++;
        }

        // duplicate the cylinder ends to form a torus
        for (int iR = 0; iR <= radialSamples; iR++, i++) {
            BufferUtils.copyInternalVector3(fpb, iR, i);
            BufferUtils.copyInternalVector3(fnb, iR, i);
            BufferUtils.copyInternalVector2(ftb, iR, i);
            ftb.put(i * 2 + 1, 1.0f);
        }
    }

    private void setIndexData() {
        // allocate connectivity
        int triCount = 2 * circleSamples * radialSamples;

        ShortBuffer sib = BufferUtils.createShortBuffer(3 * triCount);
        setBuffer(Type.Index, 3, sib);

        int i;
        // generate connectivity
        int connectionStart = 0;
        int index = 0;
        for (int circleCount = 0; circleCount < circleSamples; circleCount++) {
            int i0 = connectionStart;
            int i1 = i0 + 1;
            connectionStart += radialSamples + 1;
            int i2 = connectionStart;
            int i3 = i2 + 1;
            for (i = 0; i < radialSamples; i++, index += 6) {
//                if (true) {
                    sib.put((short)i0++);
                    sib.put((short)i2);
                    sib.put((short)i1);
                    sib.put((short)i1++);
                    sib.put((short)i2++);
                    sib.put((short)i3++);

//                    getIndexBuffer().put(i0++);
//                    getIndexBuffer().put(i2);
//                    getIndexBuffer().put(i1);
//                    getIndexBuffer().put(i1++);
//                    getIndexBuffer().put(i2++);
//                    getIndexBuffer().put(i3++);
//                } else {
//                    getIndexBuffer().put(i0++);
//                    getIndexBuffer().put(i1);
//                    getIndexBuffer().put(i2);
//                    getIndexBuffer().put(i1++);
//                    getIndexBuffer().put(i3++);
//                    getIndexBuffer().put(i2++);
//                }
            }
        }
    }

    /**
     * Rebuilds this torus based on a new set of parameters.
     * 
     * @param circleSamples the number of samples along the circles.
     * @param radialSamples the number of samples along the radial.
     * @param innerRadius the radius of the inner begining of the Torus.
     * @param outerRadius the radius of the outter end of the Torus.
     */
    public void updateGeometry(int circleSamples, int radialSamples, float innerRadius, float outerRadius) {
        this.circleSamples = circleSamples;
        this.radialSamples = radialSamples;
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
        setGeometryData();
        setIndexData();
        updateBound();
        updateCounts();
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(circleSamples, "circleSamples", 0);
        capsule.write(radialSamples, "radialSamples", 0);
        capsule.write(innerRadius, "innerRadius", 0);
        capsule.write(outerRadius, "outerRadius", 0);
    }

}