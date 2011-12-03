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
// $Id: Sphere.java 4163 2009-03-25 01:14:55Z matt.yellen $
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
import com.jme3.util.TempVars;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * <code>Sphere</code> represents a 3D object with all points equidistance
 * from a center point.
 * 
 * @author Joshua Slack
 * @version $Revision: 4163 $, $Date: 2009-03-24 21:14:55 -0400 (Tue, 24 Mar 2009) $
 */
public class Sphere extends Mesh {

    public enum TextureMode {

        /** 
         * Wrap texture radially and along z-axis 
         */
        Original,
        /** 
         * Wrap texure radially, but spherically project along z-axis 
         */
        Projected,
        /** 
         * Apply texture to each pole.  Eliminates polar distortion,
         * but mirrors the texture across the equator 
         */
        Polar
    }
    protected int vertCount;
    protected int triCount;
    protected int zSamples;
    protected int radialSamples;
    protected boolean useEvenSlices;
    protected boolean interior;
    /** the distance from the center point each point falls on */
    public float radius;
    protected TextureMode textureMode = TextureMode.Original;

    /**
     * Serialization only. Do not use.
     */
    public Sphere() {
    }

    /**
     * Constructs a sphere. All geometry data buffers are updated automatically.
     * Both zSamples and radialSamples increase the quality of the generated
     * sphere.
     * 
     * @param zSamples
     *            The number of samples along the Z.
     * @param radialSamples
     *            The number of samples along the radial.
     * @param radius
     *            The radius of the sphere.
     */
    public Sphere(int zSamples, int radialSamples, float radius) {
        this(zSamples, radialSamples, radius, false, false);
    }

    /**
     * Constructs a sphere. Additional arg to evenly space latitudinal slices
     * 
     * @param zSamples
     *            The number of samples along the Z.
     * @param radialSamples
     *            The number of samples along the radial.
     * @param radius
     *            The radius of the sphere.
     * @param useEvenSlices
     *            Slice sphere evenly along the Z axis
     * @param interior
     *            Not yet documented
     */
    public Sphere(int zSamples, int radialSamples, float radius, boolean useEvenSlices, boolean interior) {
        updateGeometry(zSamples, radialSamples, radius, useEvenSlices, interior);
    }

    public int getRadialSamples() {
        return radialSamples;
    }

    public float getRadius() {
        return radius;
    }

    /**
     * @return Returns the textureMode.
     */
    public TextureMode getTextureMode() {
        return textureMode;
    }

    public int getZSamples() {
        return zSamples;
    }

    /**
     * builds the vertices based on the radius, radial and zSamples.
     */
    private void setGeometryData() {
        // allocate vertices
        vertCount = (zSamples - 2) * (radialSamples + 1) + 2;

        FloatBuffer posBuf = BufferUtils.createVector3Buffer(vertCount);

        // allocate normals if requested
        FloatBuffer normBuf = BufferUtils.createVector3Buffer(vertCount);

        // allocate texture coordinates
        FloatBuffer texBuf = BufferUtils.createVector2Buffer(vertCount);

        setBuffer(Type.Position, 3, posBuf);
        setBuffer(Type.Normal, 3, normBuf);
        setBuffer(Type.TexCoord, 2, texBuf);

        // generate geometry
        float fInvRS = 1.0f / radialSamples;
        float fZFactor = 2.0f / (zSamples - 1);

        // Generate points on the unit circle to be used in computing the mesh
        // points on a sphere slice.
        float[] afSin = new float[(radialSamples + 1)];
        float[] afCos = new float[(radialSamples + 1)];
        for (int iR = 0; iR < radialSamples; iR++) {
            float fAngle = FastMath.TWO_PI * fInvRS * iR;
            afCos[iR] = FastMath.cos(fAngle);
            afSin[iR] = FastMath.sin(fAngle);
        }
        afSin[radialSamples] = afSin[0];
        afCos[radialSamples] = afCos[0];

        TempVars vars = TempVars.get();
        Vector3f tempVa = vars.vect1;
        Vector3f tempVb = vars.vect2;
        Vector3f tempVc = vars.vect3;

        // generate the sphere itself
        int i = 0;
        for (int iZ = 1; iZ < (zSamples - 1); iZ++) {
            float fAFraction = FastMath.HALF_PI * (-1.0f + fZFactor * iZ); // in (-pi/2, pi/2)
            float fZFraction;
            if (useEvenSlices) {
                fZFraction = -1.0f + fZFactor * iZ; // in (-1, 1)
            } else {
                fZFraction = FastMath.sin(fAFraction); // in (-1,1)
            }
            float fZ = radius * fZFraction;

            // compute center of slice
            Vector3f kSliceCenter = tempVb.set(Vector3f.ZERO);
            kSliceCenter.z += fZ;

            // compute radius of slice
            float fSliceRadius = FastMath.sqrt(FastMath.abs(radius * radius
                    - fZ * fZ));

            // compute slice vertices with duplication at end point
            Vector3f kNormal;
            int iSave = i;
            for (int iR = 0; iR < radialSamples; iR++) {
                float fRadialFraction = iR * fInvRS; // in [0,1)
                Vector3f kRadial = tempVc.set(afCos[iR], afSin[iR], 0);
                kRadial.mult(fSliceRadius, tempVa);
                posBuf.put(kSliceCenter.x + tempVa.x).put(
                        kSliceCenter.y + tempVa.y).put(
                        kSliceCenter.z + tempVa.z);

                BufferUtils.populateFromBuffer(tempVa, posBuf, i);
                kNormal = tempVa;
                kNormal.normalizeLocal();
                if (!interior) // allow interior texture vs. exterior
                {
                    normBuf.put(kNormal.x).put(kNormal.y).put(
                            kNormal.z);
                } else {
                    normBuf.put(-kNormal.x).put(-kNormal.y).put(
                            -kNormal.z);
                }

                if (textureMode == TextureMode.Original) {
                    texBuf.put(fRadialFraction).put(
                            0.5f * (fZFraction + 1.0f));
                } else if (textureMode == TextureMode.Projected) {
                    texBuf.put(fRadialFraction).put(
                            FastMath.INV_PI
                            * (FastMath.HALF_PI + FastMath.asin(fZFraction)));
                } else if (textureMode == TextureMode.Polar) {
                    float r = (FastMath.HALF_PI - FastMath.abs(fAFraction)) / FastMath.PI;
                    float u = r * afCos[iR] + 0.5f;
                    float v = r * afSin[iR] + 0.5f;
                    texBuf.put(u).put(v);
                }

                i++;
            }

            BufferUtils.copyInternalVector3(posBuf, iSave, i);
            BufferUtils.copyInternalVector3(normBuf, iSave, i);

            if (textureMode == TextureMode.Original) {
                texBuf.put(1.0f).put(
                        0.5f * (fZFraction + 1.0f));
            } else if (textureMode == TextureMode.Projected) {
                texBuf.put(1.0f).put(
                        FastMath.INV_PI
                        * (FastMath.HALF_PI + FastMath.asin(fZFraction)));
            } else if (textureMode == TextureMode.Polar) {
                float r = (FastMath.HALF_PI - FastMath.abs(fAFraction)) / FastMath.PI;
                texBuf.put(r + 0.5f).put(0.5f);
            }

            i++;
        }

        vars.release();

        // south pole
        posBuf.position(i * 3);
        posBuf.put(0f).put(0f).put(-radius);

        normBuf.position(i * 3);
        if (!interior) {
            normBuf.put(0).put(0).put(-1); // allow for inner
        } // texture orientation
        // later.
        else {
            normBuf.put(0).put(0).put(1);
        }

        texBuf.position(i * 2);

        if (textureMode == TextureMode.Polar) {
            texBuf.put(0.5f).put(0.5f);
        } else {
            texBuf.put(0.5f).put(0.0f);
        }

        i++;

        // north pole
        posBuf.put(0).put(0).put(radius);

        if (!interior) {
            normBuf.put(0).put(0).put(1);
        } else {
            normBuf.put(0).put(0).put(-1);
        }

        if (textureMode == TextureMode.Polar) {
            texBuf.put(0.5f).put(0.5f);
        } else {
            texBuf.put(0.5f).put(1.0f);
        }

        updateBound();
        setStatic();
    }

    /**
     * sets the indices for rendering the sphere.
     */
    private void setIndexData() {
        // allocate connectivity
        triCount = 2 * (zSamples - 2) * radialSamples;
        ShortBuffer idxBuf = BufferUtils.createShortBuffer(3 * triCount);
        setBuffer(Type.Index, 3, idxBuf);

        // generate connectivity
        int index = 0;
        for (int iZ = 0, iZStart = 0; iZ < (zSamples - 3); iZ++) {
            int i0 = iZStart;
            int i1 = i0 + 1;
            iZStart += (radialSamples + 1);
            int i2 = iZStart;
            int i3 = i2 + 1;
            for (int i = 0; i < radialSamples; i++, index += 6) {
                if (!interior) {
                    idxBuf.put((short) i0++);
                    idxBuf.put((short) i1);
                    idxBuf.put((short) i2);
                    idxBuf.put((short) i1++);
                    idxBuf.put((short) i3++);
                    idxBuf.put((short) i2++);
                } else { // inside view
                    idxBuf.put((short) i0++);
                    idxBuf.put((short) i2);
                    idxBuf.put((short) i1);
                    idxBuf.put((short) i1++);
                    idxBuf.put((short) i2++);
                    idxBuf.put((short) i3++);
                }
            }
        }

        // south pole triangles
        for (int i = 0; i < radialSamples; i++, index += 3) {
            if (!interior) {
                idxBuf.put((short) i);
                idxBuf.put((short) (vertCount - 2));
                idxBuf.put((short) (i + 1));
            } else { // inside view
                idxBuf.put((short) i);
                idxBuf.put((short) (i + 1));
                idxBuf.put((short) (vertCount - 2));
            }
        }

        // north pole triangles
        int iOffset = (zSamples - 3) * (radialSamples + 1);
        for (int i = 0; i < radialSamples; i++, index += 3) {
            if (!interior) {
                idxBuf.put((short) (i + iOffset));
                idxBuf.put((short) (i + 1 + iOffset));
                idxBuf.put((short) (vertCount - 1));
            } else { // inside view
                idxBuf.put((short) (i + iOffset));
                idxBuf.put((short) (vertCount - 1));
                idxBuf.put((short) (i + 1 + iOffset));
            }
        }
    }

    /**
     * @param textureMode
     *            The textureMode to set.
     */
    public void setTextureMode(TextureMode textureMode) {
        this.textureMode = textureMode;
        setGeometryData();
    }

    /**
     * Changes the information of the sphere into the given values.
     * 
     * @param zSamples the number of zSamples of the sphere.
     * @param radialSamples the number of radial samples of the sphere.
     * @param radius the radius of the sphere.
     */
    public void updateGeometry(int zSamples, int radialSamples, float radius) {
        updateGeometry(zSamples, radialSamples, radius, false, false);
    }

    public void updateGeometry(int zSamples, int radialSamples, float radius, boolean useEvenSlices, boolean interior) {
        this.zSamples = zSamples;
        this.radialSamples = radialSamples;
        this.radius = radius;
        this.useEvenSlices = useEvenSlices;
        this.interior = interior;
        setGeometryData();
        setIndexData();
    }

    public void read(JmeImporter e) throws IOException {
        super.read(e);
        InputCapsule capsule = e.getCapsule(this);
        zSamples = capsule.readInt("zSamples", 0);
        radialSamples = capsule.readInt("radialSamples", 0);
        radius = capsule.readFloat("radius", 0);
        useEvenSlices = capsule.readBoolean("useEvenSlices", false);
        textureMode = capsule.readEnum("textureMode", TextureMode.class, TextureMode.Original);
        interior = capsule.readBoolean("interior", false);
    }

    public void write(JmeExporter e) throws IOException {
        super.write(e);
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(zSamples, "zSamples", 0);
        capsule.write(radialSamples, "radialSamples", 0);
        capsule.write(radius, "radius", 0);
        capsule.write(useEvenSlices, "useEvenSlices", false);
        capsule.write(textureMode, "textureMode", TextureMode.Original);
        capsule.write(interior, "interior", false);
    }
}
