/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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

// $Id: PQTorus.java 4131 2009-03-19 20:15:28Z blaine.dev $
package com.jme3.scene.shape;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import static com.jme3.util.BufferUtils.*;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * A parameterized torus, also known as a <em>pq</em> torus.
 * 
 * @author Joshua Slack, Eric Woroshow
 * @version $Revision: 4131 $, $Date: 2009-03-19 16:15:28 -0400 (Thu, 19 Mar 2009) $
 */
public class PQTorus extends Mesh {

    private float p, q;

    private float radius, width;

    private int steps, radialSamples;

    public PQTorus() {
    }

    /**
     * Creates a parameterized torus.
     * <p>
     * Steps and radialSamples are both degree of accuracy values.
     * 
     * @param p the x/z oscillation.
     * @param q the y oscillation.
     * @param radius the radius of the PQTorus.
     * @param width the width of the torus.
     * @param steps the steps along the torus.
     * @param radialSamples radial samples for the torus.
     */
    public PQTorus(float p, float q, float radius, float width,
            int steps, int radialSamples) {
        super();
        updateGeometry(p, q, radius, width, steps, radialSamples);
    }

    public float getP() {
        return p;
    }

    public float getQ() {
        return q;
    }

    public int getRadialSamples() {
        return radialSamples;
    }

    public float getRadius() {
        return radius;
    }

    public int getSteps() {
        return steps;
    }

    public float getWidth() {
        return width;
    }

    public void read(JmeImporter e) throws IOException {
        super.read(e);
        InputCapsule capsule = e.getCapsule(this);
        p = capsule.readFloat("p", 0);
        q = capsule.readFloat("q", 0);
        radius = capsule.readFloat("radius", 0);
        width = capsule.readFloat("width", 0);
        steps = capsule.readInt("steps", 0);
        radialSamples = capsule.readInt("radialSamples", 0);
    }

    /**
     * Rebuilds this torus based on a new set of parameters.
     * 
     * @param p the x/z oscillation.
     * @param q the y oscillation.
     * @param radius the radius of the PQTorus.
     * @param width the width of the torus.
     * @param steps the steps along the torus.
     * @param radialSamples radial samples for the torus.
     */
    public void updateGeometry(float p, float q, float radius, float width, int steps, int radialSamples) {
        this.p = p;
        this.q = q;
        this.radius = radius;
        this.width = width;
        this.steps = steps;
        this.radialSamples = radialSamples;

        final float thetaStep = (FastMath.TWO_PI / steps);
        final float betaStep = (FastMath.TWO_PI / radialSamples);
        Vector3f[] torusPoints = new Vector3f[steps];

        // Allocate all of the required buffers
        int vertCount = radialSamples * steps;
        
        FloatBuffer fpb = createVector3Buffer(vertCount);
        FloatBuffer fnb = createVector3Buffer(vertCount);
        FloatBuffer ftb = createVector2Buffer(vertCount);

        Vector3f pointB = new Vector3f(), T = new Vector3f(), N = new Vector3f(), B = new Vector3f();
        Vector3f tempNorm = new Vector3f();
        float r, x, y, z, theta = 0.0f, beta = 0.0f;
        int nvertex = 0;

        // Move along the length of the pq torus
        for (int i = 0; i < steps; i++) {
            theta += thetaStep;
            float circleFraction = ((float) i) / (float) steps;

            // Find the point on the torus
            r = (0.5f * (2.0f + FastMath.sin(q * theta)) * radius);
            x = (r * FastMath.cos(p * theta) * radius);
            y = (r * FastMath.sin(p * theta) * radius);
            z = (r * FastMath.cos(q * theta) * radius);
            torusPoints[i] = new Vector3f(x, y, z);

            // Now find a point slightly farther along the torus
            r = (0.5f * (2.0f + FastMath.sin(q * (theta + 0.01f))) * radius);
            x = (r * FastMath.cos(p * (theta + 0.01f)) * radius);
            y = (r * FastMath.sin(p * (theta + 0.01f)) * radius);
            z = (r * FastMath.cos(q * (theta + 0.01f)) * radius);
            pointB = new Vector3f(x, y, z);

            // Approximate the Frenet Frame
            T = pointB.subtract(torusPoints[i]);
            N = torusPoints[i].add(pointB);
            B = T.cross(N);
            N = B.cross(T);

            // Normalise the two vectors and then use them to create an oriented circle
            N = N.normalize();
            B = B.normalize();
            beta = 0.0f;
            for (int j = 0; j < radialSamples; j++, nvertex++) {
                beta += betaStep;
                float cx = FastMath.cos(beta) * width;
                float cy = FastMath.sin(beta) * width;
                float radialFraction = ((float) j) / radialSamples;
                tempNorm.x = (cx * N.x + cy * B.x);
                tempNorm.y = (cx * N.y + cy * B.y);
                tempNorm.z = (cx * N.z + cy * B.z);
                fnb.put(tempNorm.x).put(tempNorm.y).put(tempNorm.z);
                tempNorm.addLocal(torusPoints[i]);
                fpb.put(tempNorm.x).put(tempNorm.y).put(tempNorm.z);
                ftb.put(radialFraction).put(circleFraction);
            }
        }

        // Update the indices data
        ShortBuffer sib = createShortBuffer(6 * vertCount);
        for (int i = 0; i < vertCount; i++) {
            sib.put(new short[] {
                    (short)(i),
                    (short)(i - radialSamples),
                    (short)(i + 1),
                    (short)(i + 1),
                    (short)(i - radialSamples),
                    (short)(i - radialSamples + 1)
            });
        }
        for (int i = 0, len = sib.capacity(); i < len; i++) {
            int ind = sib.get(i);
            if (ind < 0) {
                ind += vertCount;
                sib.put(i, (short) ind);
            } else if (ind >= vertCount) {
                ind -= vertCount;
                sib.put(i, (short) ind);
            }
        }
        sib.rewind();

        setBuffer(Type.Position, 3, fpb);
        setBuffer(Type.Normal,   3, fnb);
        setBuffer(Type.TexCoord, 2, ftb);
        setBuffer(Type.Index,    3, sib);
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(p, "p", 0);
        capsule.write(q, "q", 0);
        capsule.write(radius, "radius", 0);
        capsule.write(width, "width", 0);
        capsule.write(steps, "steps", 0);
        capsule.write(radialSamples, "radialSamples", 0);
    }

}