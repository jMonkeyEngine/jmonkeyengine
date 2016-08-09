 /*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
package com.jme3.gde.scenecomposer.gizmo.light.shape;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.control.BillboardControl;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * 
 * A debuging shape for a BoundingSphere 
 * Consists of 3 axis aligned circles.
 * 
 * @author nehon
 */
public class ProbeRadiusShape extends Mesh {

    protected int vertCount;
    protected int triCount;
    protected int radialSamples = 256;
    protected boolean useEvenSlices;
    protected boolean interior;
    /**
     * the distance from the center point each point falls on
     */
    public float radius;

    public float getRadius() {
        return radius;
    }

    public ProbeRadiusShape() {
        setGeometryData();
        setIndexData();
    }

    /**
     * builds the vertices based on the radius
     */
    private void setGeometryData() {
        setMode(Mode.Lines);

        FloatBuffer posBuf = BufferUtils.createVector3Buffer((radialSamples + 1));
        FloatBuffer colBuf = BufferUtils.createFloatBuffer((radialSamples + 1) * 4);
        FloatBuffer texBuf = BufferUtils.createVector2Buffer(radialSamples + 1);
        

        setBuffer(Type.Position, 3, posBuf);
        setBuffer(Type.Color, 4, colBuf);
        setBuffer(Type.TexCoord, 2, texBuf);

        // generate geometry
        float fInvRS = 1.0f / radialSamples;

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

        for (int iR = 0; iR <= radialSamples; iR++) {
            posBuf.put(afCos[iR])
                    .put(afSin[iR])
                    .put(0);
            colBuf.put(ColorRGBA.Orange.r)
                    .put(ColorRGBA.Orange.g)
                    .put(ColorRGBA.Orange.b)
                    .put(ColorRGBA.Orange.a);
            texBuf.put(iR % 2f)
                    .put(iR % 2f);

        }

        updateBound();
        setStatic();
    }

    /**
     * sets the indices for rendering the sphere.
     */
    private void setIndexData() {
        // allocate connectivity
        int nbSegments = (radialSamples);// * 3;

        ShortBuffer idxBuf = BufferUtils.createShortBuffer(2 * nbSegments);
        setBuffer(Type.Index, 2, idxBuf);

        int idx = 0;
        int segDone = 0;
        while (segDone < nbSegments) {
            idxBuf.put((short) idx);
            idxBuf.put((short) (idx + 1));
            idx++;
            segDone++;
        }

    }
    

    /**
     * Convenience factory method that creates a debuging bounding sphere geometry
     * @param assetManager the assetManager
     * @return the bounding sphere debug geometry.
     */
    public static Geometry createShape(AssetManager assetManager) {
        ProbeRadiusShape b = new ProbeRadiusShape();
        Geometry geom = new Geometry("BoundingDebug", b);

        Material mat = new Material(assetManager, "com/jme3/gde/scenecomposer/gizmo/light/mat/dashed/dashed.j3md");        
        mat.getAdditionalRenderState().setWireframe(true);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        mat.getAdditionalRenderState().setDepthWrite(false);
        mat.getAdditionalRenderState().setDepthTest(false);  
        mat.setFloat("DashSize", 0.5f);
        geom.setQueueBucket(RenderQueue.Bucket.Transparent);
        geom.addControl(new BillboardControl());
        
        
        geom.setMaterial(mat);
        return geom;

    }
}
