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

// $Id: Cylinder.java 4131 2009-03-19 20:15:28Z blaine.dev $
package com.jme3.scene.shape;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.BufferUtils;
import static com.jme3.util.BufferUtils.*;
import java.io.IOException;
import java.nio.FloatBuffer;

/**
 * A simple cylinder, defined by it's height and radius.
 * (Ported to jME3)
 *
 * @author Mark Powell
 * @version $Revision: 4131 $, $Date: 2009-03-19 16:15:28 -0400 (Thu, 19 Mar 2009) $
 */
public class Cylinder extends Mesh {

    private int axisSamples;

    private int radialSamples;

    private float radius;
    private float radius2;

    private float height;
    private boolean closed;
    private boolean inverted;

    /**
     * Default constructor for serialization only. Do not use.
     */
    public Cylinder() {
    }

    /**
     * Creates a new Cylinder. By default its center is the origin. Usually, a
     * higher sample number creates a better looking cylinder, but at the cost
     * of more vertex information.
     *
     * @param axisSamples
     *            Number of triangle samples along the axis.
     * @param radialSamples
     *            Number of triangle samples along the radial.
     * @param radius
     *            The radius of the cylinder.
     * @param height
     *            The cylinder's height.
     */
    public Cylinder(int axisSamples, int radialSamples,
            float radius, float height) {
        this(axisSamples, radialSamples, radius, height, false);
    }

    /**
     * Creates a new Cylinder. By default its center is the origin. Usually, a
     * higher sample number creates a better looking cylinder, but at the cost
     * of more vertex information. <br>
     * If the cylinder is closed the texture is split into axisSamples parts:
     * top most and bottom most part is used for top and bottom of the cylinder,
     * rest of the texture for the cylinder wall. The middle of the top is
     * mapped to texture coordinates (0.5, 1), bottom to (0.5, 0). Thus you need
     * a suited distorted texture.
     *
     * @param axisSamples
     *            Number of triangle samples along the axis.
     * @param radialSamples
     *            Number of triangle samples along the radial.
     * @param radius
     *            The radius of the cylinder.
     * @param height
     *            The cylinder's height.
     * @param closed
     *            true to create a cylinder with top and bottom surface
     */
    public Cylinder(int axisSamples, int radialSamples,
            float radius, float height, boolean closed) {
        this(axisSamples, radialSamples, radius, height, closed, false);
    }

    /**
     * Creates a new Cylinder. By default its center is the origin. Usually, a
     * higher sample number creates a better looking cylinder, but at the cost
     * of more vertex information. <br>
     * If the cylinder is closed the texture is split into axisSamples parts:
     * top most and bottom most part is used for top and bottom of the cylinder,
     * rest of the texture for the cylinder wall. The middle of the top is
     * mapped to texture coordinates (0.5, 1), bottom to (0.5, 0). Thus you need
     * a suited distorted texture.
     *
     * @param axisSamples
     *            Number of triangle samples along the axis.
     * @param radialSamples
     *            Number of triangle samples along the radial.
     * @param radius
     *            The radius of the cylinder.
     * @param height
     *            The cylinder's height.
     * @param closed
     *            true to create a cylinder with top and bottom surface
     * @param inverted
     *            true to create a cylinder that is meant to be viewed from the
     *            interior.
     */
    public Cylinder(int axisSamples, int radialSamples,
            float radius, float height, boolean closed, boolean inverted) {
        this(axisSamples, radialSamples, radius, radius, height, closed, inverted);
    }

    public Cylinder(int axisSamples, int radialSamples,
            float radius, float radius2, float height, boolean closed, boolean inverted) {
        super();
        updateGeometry(axisSamples, radialSamples, radius, radius2, height, closed, inverted);
    }

    /**
     * @return the number of samples along the cylinder axis
     */
    public int getAxisSamples() {
        return axisSamples;
    }

    /**
     * @return Returns the height.
     */
    public float getHeight() {
        return height;
    }

    /**
     * @return number of samples around cylinder
     */
    public int getRadialSamples() {
        return radialSamples;
    }

    /**
     * @return Returns the radius.
     */
    public float getRadius() {
        return radius;
    }

    public float getRadius2() {
        return radius2;
    }

    /**
     * @return true if end caps are used.
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * @return true if normals and uvs are created for interior use
     */
    public boolean isInverted() {
        return inverted;
    }

    /**
     * Rebuilds the cylinder based on a new set of parameters.
     *
     * @param axisSamples the number of samples along the axis.
     * @param radialSamples the number of samples around the radial.
     * @param radius the radius of the bottom of the cylinder.
     * @param radius2 the radius of the top of the cylinder.
     * @param height the cylinder's height.
     * @param closed should the cylinder have top and bottom surfaces.
     * @param inverted is the cylinder is meant to be viewed from the inside.
     */
    public void updateGeometry(int axisSamples, int radialSamples,
            float radius, float radius2, float height, boolean closed, boolean inverted) {
        this.axisSamples = axisSamples + (closed ? 2 : 0);
        this.radialSamples = radialSamples;
        this.radius = radius;
        this.radius2 = radius2;
        this.height = height;
        this.closed = closed;
        this.inverted = inverted;

//        VertexBuffer pvb = getBuffer(Type.Position);
//        VertexBuffer nvb = getBuffer(Type.Normal);
//        VertexBuffer tvb = getBuffer(Type.TexCoord);

        // Vertices
        int vertCount = axisSamples * (radialSamples + 1) + (closed ? 2 : 0);

        setBuffer(Type.Position, 3, createVector3Buffer(getFloatBuffer(Type.Position), vertCount));

        // Normals
        setBuffer(Type.Normal, 3, createVector3Buffer(getFloatBuffer(Type.Normal), vertCount));

        // Texture co-ordinates
        setBuffer(Type.TexCoord, 2, createVector2Buffer(vertCount));

        int triCount = ((closed ? 2 : 0) + 2 * (axisSamples - 1)) * radialSamples;
        
        setBuffer(Type.Index, 3, createShortBuffer(getShortBuffer(Type.Index), 3 * triCount));

        // generate geometry
        float inverseRadial = 1.0f / radialSamples;
        float inverseAxisLess = 1.0f / (closed ? axisSamples - 3 : axisSamples - 1);
        float inverseAxisLessTexture = 1.0f / (axisSamples - 1);
        float halfHeight = 0.5f * height;

        // Generate points on the unit circle to be used in computing the mesh
        // points on a cylinder slice.
        float[] sin = new float[radialSamples + 1];
        float[] cos = new float[radialSamples + 1];

        for (int radialCount = 0; radialCount < radialSamples; radialCount++) {
            float angle = FastMath.TWO_PI * inverseRadial * radialCount;
            cos[radialCount] = FastMath.cos(angle);
            sin[radialCount] = FastMath.sin(angle);
        }
        sin[radialSamples] = sin[0];
        cos[radialSamples] = cos[0];

        // calculate normals
        Vector3f[] vNormals = null;
        Vector3f vNormal = Vector3f.UNIT_Z;

        if ((height != 0.0f) && (radius != radius2)) {
            vNormals = new Vector3f[radialSamples];
            Vector3f vHeight = Vector3f.UNIT_Z.mult(height);
            Vector3f vRadial = new Vector3f();

            for (int radialCount = 0; radialCount < radialSamples; radialCount++) {
                vRadial.set(cos[radialCount], sin[radialCount], 0.0f);
                Vector3f vRadius = vRadial.mult(radius);
                Vector3f vRadius2 = vRadial.mult(radius2);
                Vector3f vMantle = vHeight.subtract(vRadius2.subtract(vRadius));
                Vector3f vTangent = vRadial.cross(Vector3f.UNIT_Z);
                vNormals[radialCount] = vMantle.cross(vTangent).normalize();
            }
        }

        FloatBuffer nb = getFloatBuffer(Type.Normal);
        FloatBuffer pb = getFloatBuffer(Type.Position);
        FloatBuffer tb = getFloatBuffer(Type.TexCoord);

        // generate the cylinder itself
        Vector3f tempNormal = new Vector3f();
        for (int axisCount = 0, i = 0; axisCount < axisSamples; axisCount++, i++) {
            float axisFraction;
            float axisFractionTexture;
            int topBottom = 0;
            if (!closed) {
                axisFraction = axisCount * inverseAxisLess; // in [0,1]
                axisFractionTexture = axisFraction;
            } else {
                if (axisCount == 0) {
                    topBottom = -1; // bottom
                    axisFraction = 0;
                    axisFractionTexture = inverseAxisLessTexture;
                } else if (axisCount == axisSamples - 1) {
                    topBottom = 1; // top
                    axisFraction = 1;
                    axisFractionTexture = 1 - inverseAxisLessTexture;
                } else {
                    axisFraction = (axisCount - 1) * inverseAxisLess;
                    axisFractionTexture = axisCount * inverseAxisLessTexture;
                }
            }

            // compute center of slice
            float z = -halfHeight + height * axisFraction;
            Vector3f sliceCenter = new Vector3f(0, 0, z);

            // compute slice vertices with duplication at end point
            int save = i;
            for (int radialCount = 0; radialCount < radialSamples; radialCount++, i++) {
                float radialFraction = radialCount * inverseRadial; // in [0,1)
                tempNormal.set(cos[radialCount], sin[radialCount], 0.0f);

                if (vNormals != null) {
                    vNormal = vNormals[radialCount];
                } else if (radius == radius2) {
                    vNormal = tempNormal;
                }

                if (topBottom == 0) {
                    if (!inverted)
                        nb.put(vNormal.x).put(vNormal.y).put(vNormal.z);
                    else
                        nb.put(-vNormal.x).put(-vNormal.y).put(-vNormal.z);
                } else {
                    nb.put(0).put(0).put(topBottom * (inverted ? -1 : 1));
                }

                tempNormal.multLocal((radius - radius2) * axisFraction + radius2)
                        .addLocal(sliceCenter);
                pb.put(tempNormal.x).put(tempNormal.y).put(tempNormal.z);

                tb.put((inverted ? 1 - radialFraction : radialFraction))
                        .put(axisFractionTexture);
            }

            BufferUtils.copyInternalVector3(pb, save, i);
            BufferUtils.copyInternalVector3(nb, save, i);

            tb.put((inverted ? 0.0f : 1.0f))
                    .put(axisFractionTexture);
        }

        if (closed) {
            pb.put(0).put(0).put(-halfHeight); // bottom center
            nb.put(0).put(0).put(-1 * (inverted ? -1 : 1));
            tb.put(0.5f).put(0);
            pb.put(0).put(0).put(halfHeight); // top center
            nb.put(0).put(0).put(1 * (inverted ? -1 : 1));
            tb.put(0.5f).put(1);
        }

        IndexBuffer ib = getIndexBuffer();
        int index = 0;
        // Connectivity
        for (int axisCount = 0, axisStart = 0; axisCount < axisSamples - 1; axisCount++) {
            int i0 = axisStart;
            int i1 = i0 + 1;
            axisStart += radialSamples + 1;
            int i2 = axisStart;
            int i3 = i2 + 1;
            for (int i = 0; i < radialSamples; i++) {
                if (closed && axisCount == 0) {
                    if (!inverted) {
                        ib.put(index++, i0++);
                        ib.put(index++, vertCount - 2);
                        ib.put(index++, i1++);
                    } else {
                        ib.put(index++, i0++);
                        ib.put(index++, i1++);
                        ib.put(index++, vertCount - 2);
                    }
                } else if (closed && axisCount == axisSamples - 2) {
                    ib.put(index++, i2++);
                    ib.put(index++, inverted ? vertCount - 1 : i3++);
                    ib.put(index++, inverted ? i3++ : vertCount - 1);
                } else {
                    ib.put(index++, i0++);
                    ib.put(index++, inverted ? i2 : i1);
                    ib.put(index++, inverted ? i1 : i2);
                    ib.put(index++, i1++);
                    ib.put(index++, inverted ? i2++ : i3++);
                    ib.put(index++, inverted ? i3++ : i2++);
                }
            }
        }

        updateBound();
    }

    public void read(JmeImporter e) throws IOException {
        super.read(e);
        InputCapsule capsule = e.getCapsule(this);
        axisSamples = capsule.readInt("axisSamples", 0);
        radialSamples = capsule.readInt("radialSamples", 0);
        radius = capsule.readFloat("radius", 0);
        radius2 = capsule.readFloat("radius2", 0);
        height = capsule.readFloat("height", 0);
        closed = capsule.readBoolean("closed", false);
        inverted = capsule.readBoolean("inverted", false);
    }

    public void write(JmeExporter e) throws IOException {
        super.write(e);
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(axisSamples, "axisSamples", 0);
        capsule.write(radialSamples, "radialSamples", 0);
        capsule.write(radius, "radius", 0);
        capsule.write(radius2, "radius2", 0);
        capsule.write(height, "height", 0);
        capsule.write(closed, "closed", false);
        capsule.write(inverted, "inverted", false);
    }


}
