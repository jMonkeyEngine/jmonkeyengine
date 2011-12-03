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
// $Id: Dome.java 4131 2009-03-19 20:15:28Z blaine.dev $
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
 * A hemisphere.
 * 
 * @author Peter Andersson
 * @author Joshua Slack (Original sphere code that was adapted)
 * @version $Revision: 4131 $, $Date: 2009-03-19 16:15:28 -0400 (Thu, 19 Mar 2009) $
 */
public class Dome extends Mesh {

    private int planes;
    private int radialSamples;
    /** The radius of the dome */
    private float radius;
    /** The center of the dome */
    private Vector3f center;
    private boolean insideView = true;

    /**
     * Serialization only. Do not use.
     */
    public Dome() {
    }

    /**
     * Constructs a dome for use as a SkyDome. The SkyDome is centered at the origin 
     * and only visible from the inside. 
     * @param planes
     *            The number of planes along the Z-axis. Must be >= 2.
     *            Influences how round the arch of the dome is.
     * @param radialSamples
     *            The number of samples along the radial.
     *            Influences how round the base of the dome is.
     * @param radius
     *            Radius of the dome.
     * @see #Dome(java.lang.String, com.jme.math.Vector3f, int, int, float)
     */
    public Dome(int planes, int radialSamples, float radius) {
        this(new Vector3f(0, 0, 0), planes, radialSamples, radius);
    }

    /**
     * Constructs a dome visible from the inside, e.g. for use as a SkyDome. 
     * All geometry data buffers are updated automatically. <br>
     * For a cone, set planes=2. For a pyramid, set radialSamples=4 and planes=2.
     * Increasing planes and radialSamples increase the quality of the dome.
     * 
     * @param center
     *            Center of the dome.
     * @param planes
     *            The number of planes along the Z-axis. Must be >= 2.
     *            Influences how round the arch of the dome is.
     * @param radialSamples
     *            The number of samples along the radial. 
     *            Influences how round the base of the dome is.
     * @param radius
     *            The radius of the dome.
     */
    public Dome(Vector3f center, int planes, int radialSamples,
            float radius) {
        super();
        updateGeometry(center, planes, radialSamples, radius, true);
    }

    /**
     * Constructs a dome. Use this constructor for half-sphere, pyramids, or cones. 
     * All geometry data buffers are updated automatically. <br>
     * For a cone, set planes=2. For a pyramid, set radialSamples=4 and planes=2.
     * Setting higher values for planes and radialSamples increases 
     * the quality of the half-sphere.
     * 
     * @param center
     *            Center of the dome.
     * @param planes
     *            The number of planes along the Z-axis. Must be >= 2.
     *            Influences how round the arch of the dome is.
     * @param radialSamples
     *            The number of samples along the radial.
     *            Influences how round the base of the dome is.
     * @param radius
     *            The radius of the dome.
     * @param insideView
     *            If true, the dome is only visible from the inside, like a SkyDome.
     *            If false, the dome is only visible from the outside.
     */
    public Dome(Vector3f center, int planes, int radialSamples,
            float radius, boolean insideView) {
        super();
        updateGeometry(center, planes, radialSamples, radius, insideView);
    }

    public Vector3f getCenter() {
        return center;
    }

    /** 
     * Get the number of planar segments along the z-axis of the dome. 
     */
    public int getPlanes() {
        return planes;
    }

    /** 
     * Get the number of samples radially around the main axis of the dome. 
     */
    public int getRadialSamples() {
        return radialSamples;
    }

    /** 
     * Get the radius of the dome. 
     */
    public float getRadius() {
        return radius;
    }

    /**
     * Are the triangles connected in such a way as to present a view out from the dome or not.
     */
    public boolean isInsideView() {
        return insideView;
    }

    /**
     * Rebuilds the dome with a new set of parameters.
     * 
     * @param center the new center of the dome.
     * @param planes the number of planes along the Z-axis.
     * @param radialSamples the new number of radial samples of the dome.
     * @param radius the new radius of the dome.
     * @param insideView should the dome be set up to be viewed from the inside looking out.
     */
    public void updateGeometry(Vector3f center, int planes,
            int radialSamples, float radius, boolean insideView) {
        this.insideView = insideView;
        this.center = center != null ? center : new Vector3f(0, 0, 0);
        this.planes = planes;
        this.radialSamples = radialSamples;
        this.radius = radius;

        int vertCount = ((planes - 1) * (radialSamples + 1)) + 1;

        // Allocate vertices, allocating one extra in each radial to get the
        // correct texture coordinates
//        setVertexCount();
//        setVertexBuffer(createVector3Buffer(getVertexCount()));

        // allocate normals
//        setNormalBuffer(createVector3Buffer(getVertexCount()));

        // allocate texture coordinates
//        getTextureCoords().set(0, new TexCoords(createVector2Buffer(getVertexCount())));

        FloatBuffer vb = BufferUtils.createVector3Buffer(vertCount);
        FloatBuffer nb = BufferUtils.createVector3Buffer(vertCount);
        FloatBuffer tb = BufferUtils.createVector2Buffer(vertCount);
        setBuffer(Type.Position, 3, vb);
        setBuffer(Type.Normal, 3, nb);
        setBuffer(Type.TexCoord, 2, tb);

        // generate geometry
        float fInvRS = 1.0f / radialSamples;
        float fYFactor = 1.0f / (planes - 1);

        // Generate points on the unit circle to be used in computing the mesh
        // points on a dome slice.
        float[] afSin = new float[(radialSamples)];
        float[] afCos = new float[(radialSamples)];
        for (int iR = 0; iR < radialSamples; iR++) {
            float fAngle = FastMath.TWO_PI * fInvRS * iR;
            afCos[iR] = FastMath.cos(fAngle);
            afSin[iR] = FastMath.sin(fAngle);
        }

        TempVars vars = TempVars.get();
        Vector3f tempVc = vars.vect3;
        Vector3f tempVb = vars.vect2;
        Vector3f tempVa = vars.vect1;

        // generate the dome itself
        int i = 0;
        for (int iY = 0; iY < (planes - 1); iY++, i++) {
            float fYFraction = fYFactor * iY; // in (0,1)
            float fY = radius * fYFraction;
            // compute center of slice
            Vector3f kSliceCenter = tempVb.set(center);
            kSliceCenter.y += fY;

            // compute radius of slice
            float fSliceRadius = FastMath.sqrt(FastMath.abs(radius * radius - fY * fY));

            // compute slice vertices
            Vector3f kNormal;
            int iSave = i;
            for (int iR = 0; iR < radialSamples; iR++, i++) {
                float fRadialFraction = iR * fInvRS; // in [0,1)
                Vector3f kRadial = tempVc.set(afCos[iR], 0, afSin[iR]);
                kRadial.mult(fSliceRadius, tempVa);
                vb.put(kSliceCenter.x + tempVa.x).put(
                        kSliceCenter.y + tempVa.y).put(
                        kSliceCenter.z + tempVa.z);

                BufferUtils.populateFromBuffer(tempVa, vb, i);
                kNormal = tempVa.subtractLocal(center);
                kNormal.normalizeLocal();
                if (insideView) {
                    nb.put(kNormal.x).put(kNormal.y).put(kNormal.z);
                } else {
                    nb.put(-kNormal.x).put(-kNormal.y).put(-kNormal.z);
                }

                tb.put(fRadialFraction).put(fYFraction);
            }
            BufferUtils.copyInternalVector3(vb, iSave, i);
            BufferUtils.copyInternalVector3(nb, iSave, i);
            tb.put(1.0f).put(fYFraction);
        }

        vars.release();

        // pole
        vb.put(center.x).put(center.y + radius).put(center.z);
        nb.put(0).put(insideView ? 1 : -1).put(0);
        tb.put(0.5f).put(1.0f);

        // allocate connectivity
        int triCount = (planes - 2) * radialSamples * 2 + radialSamples;
        ShortBuffer ib = BufferUtils.createShortBuffer(3 * triCount);
        setBuffer(Type.Index, 3, ib);

        // generate connectivity
        int index = 0;
        // Generate only for middle planes
        for (int plane = 1; plane < (planes - 1); plane++) {
            int bottomPlaneStart = ((plane - 1) * (radialSamples + 1));
            int topPlaneStart = (plane * (radialSamples + 1));
            for (int sample = 0; sample < radialSamples; sample++, index += 6) {
                if (insideView){
                    ib.put((short) (bottomPlaneStart + sample));
                    ib.put((short) (bottomPlaneStart + sample + 1));
                    ib.put((short) (topPlaneStart + sample));
                    ib.put((short) (bottomPlaneStart + sample + 1));
                    ib.put((short) (topPlaneStart + sample + 1));
                    ib.put((short) (topPlaneStart + sample));
                }else{
                    ib.put((short) (bottomPlaneStart + sample));
                    ib.put((short) (topPlaneStart + sample));
                    ib.put((short) (bottomPlaneStart + sample + 1));
                    ib.put((short) (bottomPlaneStart + sample + 1));
                    ib.put((short) (topPlaneStart + sample));
                    ib.put((short) (topPlaneStart + sample + 1));
                }
            }
        }

        // pole triangles
        int bottomPlaneStart = (planes - 2) * (radialSamples + 1);
        for (int samples = 0; samples < radialSamples; samples++, index += 3) {
            if (insideView){
                ib.put((short) (bottomPlaneStart + samples));
                ib.put((short) (bottomPlaneStart + samples + 1));
                ib.put((short) (vertCount - 1));
            }else{
                ib.put((short) (bottomPlaneStart + samples));
                ib.put((short) (vertCount - 1));
                ib.put((short) (bottomPlaneStart + samples + 1));
            }
        }

        updateBound();
    }

    @Override
    public void read(JmeImporter e) throws IOException {
        super.read(e);
        InputCapsule capsule = e.getCapsule(this);
        planes = capsule.readInt("planes", 0);
        radialSamples = capsule.readInt("radialSamples", 0);
        radius = capsule.readFloat("radius", 0);
        center = (Vector3f) capsule.readSavable("center", Vector3f.ZERO.clone());
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(planes, "planes", 0);
        capsule.write(radialSamples, "radialSamples", 0);
        capsule.write(radius, "radius", 0);
        capsule.write(center, "center", Vector3f.ZERO);
    }
}