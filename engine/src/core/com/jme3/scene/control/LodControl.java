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

package com.jme3.scene.control;

import com.jme3.bounding.BoundingVolume;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import java.io.IOException;

/**
 * Determines what Level of Detail a spatial should be, based on how many pixels
 * on the screen the spatial is taking up. The more pixels covered, the more detailed
 * the spatial should be.
 * It calculates the area of the screen that the spatial covers by using its bounding box.
 * When initializing, it will ask the spatial for how many triangles it has for each LOD.
 * It then uses that, along with the trisPerPixel value to determine what LOD it should be at.
 * It requires the camera to do this.
 * The controlRender method is called each frame and will update the spatial's LOD
 * if the camera has moved by a specified amount.
 */
public class LodControl extends AbstractControl implements Cloneable {

    private float trisPerPixel = 1f;
    private float distTolerance = 1f;
    private float lastDistance = 0f;
    private int lastLevel = 0;
    private int numLevels;
    private int[] numTris;

    /**
     * Creates a new <code>LodControl</code>.
     */
    public LodControl(){
    }

    /**
     * Returns the distance tolerance for changing LOD.
     * 
     * @return the distance tolerance for changing LOD.
     * 
     * @see #setDistTolerance(float) 
     */
    public float getDistTolerance() {
        return distTolerance;
    }

    /**
     * Specifies the distance tolerance for changing the LOD level on the geometry.
     * The LOD level will only get changed if the geometry has moved this 
     * distance beyond the current LOD level.
     * 
     * @param distTolerance distance tolerance for changing LOD
     */
    public void setDistTolerance(float distTolerance) {
        this.distTolerance = distTolerance;
    }

    /**
     * Returns the triangles per pixel value.
     * 
     * @return the triangles per pixel value.
     * 
     * @see #setTrisPerPixel(float) 
     */
    public float getTrisPerPixel() {
        return trisPerPixel;
    }

    /**
     * Sets the triangles per pixel value.
     * The <code>LodControl</code> will use this value as an error metric
     * to determine which LOD level to use based on the geometry's
     * area on the screen.
     * 
     * @param trisPerPixel triangles per pixel
     */
    public void setTrisPerPixel(float trisPerPixel) {
        this.trisPerPixel = trisPerPixel;
    }

    @Override
    public void setSpatial(Spatial spatial){
        if (!(spatial instanceof Geometry))
            throw new IllegalArgumentException("LodControl can only be attached to Geometry!");

        super.setSpatial(spatial);
        Geometry geom = (Geometry) spatial;
        Mesh mesh = geom.getMesh();
        numLevels = mesh.getNumLodLevels();
        numTris = new int[numLevels];
        for (int i = numLevels - 1; i >= 0; i--)
            numTris[i] = mesh.getTriangleCount(i);
    }

    public Control cloneForSpatial(Spatial spatial) {
        try {
            LodControl clone = (LodControl) super.clone();
            clone.lastDistance = 0;
            clone.lastLevel = 0;
            clone.numTris = numTris != null ? numTris.clone() : null;
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    @Override
    protected void controlUpdate(float tpf) {
    }

    protected void controlRender(RenderManager rm, ViewPort vp){
        BoundingVolume bv = spatial.getWorldBound();

        Camera cam = vp.getCamera();
        float atanNH = FastMath.atan(cam.getFrustumNear() * cam.getFrustumTop());
        float ratio = (FastMath.PI / (8f * atanNH));
        float newDistance = bv.distanceTo(vp.getCamera().getLocation()) / ratio;
        int level;

        if (Math.abs(newDistance - lastDistance) <= distTolerance)
            level = lastLevel; // we haven't moved relative to the model, send the old measurement back.
        else if (lastDistance > newDistance && lastLevel == 0)
            level = lastLevel; // we're already at the lowest setting and we just got closer to the model, no need to keep trying.
        else if (lastDistance < newDistance && lastLevel == numLevels - 1)
            level = lastLevel; // we're already at the highest setting and we just got further from the model, no need to keep trying.
        else{
            lastDistance = newDistance;

            // estimate area of polygon via bounding volume
            float area = AreaUtils.calcScreenArea(bv, lastDistance, cam.getWidth());
            float trisToDraw = area * trisPerPixel;
            level = numLevels - 1;
            for (int i = numLevels; --i >= 0;){
                if (trisToDraw - numTris[i] < 0){
                    break;
                }
                level = i;
            }
            lastLevel = level;
        }

        spatial.setLodLevel(level);
    }

    @Override
    public void write(JmeExporter ex) throws IOException{
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(trisPerPixel, "trisPerPixel", 1f);
        oc.write(distTolerance, "distTolerance", 1f);
        oc.write(numLevels, "numLevels", 0);
        oc.write(numTris, "numTris", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException{
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        trisPerPixel = ic.readFloat("trisPerPixel", 1f);
        distTolerance = ic.readFloat("distTolerance", 1f);
        numLevels = ic.readInt("numLevels", 0);
        numTris = ic.readIntArray("numTris", null);
    }

}
