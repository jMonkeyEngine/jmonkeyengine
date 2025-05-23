/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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

import com.jme3.util.AreaUtils;
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
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * `LodControl` automatically adjusts the Level of Detail (LOD) for a
 * {@link com.jme3.scene.Geometry Geometry} based on its projected screen area.
 * The more pixels a spatial covers on the screen, the higher its detail level
 * will be. This control uses the geometry's bounding volume and the active
 * camera to estimate the screen coverage.
 * <p>
 * Upon attachment to a {@link com.jme3.scene.Geometry Geometry}, it queries
 * the mesh for the triangle counts at each available LOD level. During
 * rendering, it continuously monitors the geometry's screen size and
 * updates the LOD level if the change in screen area (or distance)
 * exceeds a specified tolerance.
 * </p>
 * <p>
 * This control is ideal for optimizing rendering performance by ensuring
 * that objects only use the necessary amount of detail based on their
 * visual prominence on screen.
 * </p>
 */
public class LodControl extends AbstractControl implements JmeCloneable {

    private static final Logger logger = Logger.getLogger(LodControl.class.getName());

    private float trisPerPixel = 1f;
    private float distTolerance = 1f;
    private float lastDistance = 0f;
    private int lastLevel = 0;
    private int numLevels;
    private int[] numTris;

    /**
     * Creates a new `LodControl`.
     * <p>
     * You must attach this control to a {@link com.jme3.scene.Geometry}
     * for it to function correctly.
     * </p>
     */
    public LodControl() {
    }

    /**
     * Returns the distance tolerance for changing LOD. The LOD level will
     * only be changed if the geometry's distance from the camera has changed
     * by more than this tolerance since the last update. This prevents
     * frequent LOD flickering when the camera moves slightly.
     *
     * @return The distance tolerance in world units.
     */
    public float getDistTolerance() {
        return distTolerance;
    }

    /**
     * Specifies the distance tolerance for changing the LOD level on the
     * geometry. The LOD level will only get changed if the geometry has moved
     * this distance beyond the current LOD level. Setting a higher tolerance
     * reduces LOD changes but might make transitions less precise.
     *
     * @param distTolerance The distance tolerance in world units (must be non-negative).
     * @throws IllegalArgumentException if `distTolerance` is negative.
     */
    public void setDistTolerance(float distTolerance) {
        if (distTolerance < 0) {
            throw new IllegalArgumentException("Distance tolerance cannot be negative.");
        }
        this.distTolerance = distTolerance;
    }

    /**
     * Returns the triangles per pixel value.
     *
     * @return the triangles per pixel value.
     */
    public float getTrisPerPixel() {
        return trisPerPixel;
    }

    /**
     * Sets the triangles per pixel value. The `LodControl` uses this value
     * as an error metric to determine which LOD level to use based on the
     * geometry's projected area on the screen. A higher value means the
     * object will appear more detailed for a given screen size.
     *
     * @param trisPerPixel The desired number of triangles per screen pixel (must be positive).
     * @throws IllegalArgumentException if `trisPerPixel` is zero or negative.
     */
    public void setTrisPerPixel(float trisPerPixel) {
        if (trisPerPixel <= 0) {
            throw new IllegalArgumentException("Triangles per pixel must be positive.");
        }
        this.trisPerPixel = trisPerPixel;
    }

    @Override
    public void setSpatial(Spatial spatial) {
        if (spatial != null && !(spatial instanceof Geometry)) {
            throw new IllegalArgumentException("Invalid Spatial type for LodControl. " +
                    "Expected a Geometry, but got: " + spatial.getClass().getSimpleName());
        }

        super.setSpatial(spatial);

        if (spatial != null) {
            Geometry geom = (Geometry) spatial;
            Mesh mesh = geom.getMesh();
            numLevels = mesh.getNumLodLevels();

            logger.log(Level.INFO, "LodControl attached to Geometry ''{0}''. Detected {1} LOD levels.",
                    new Object[]{spatial.getName(), numLevels});

            if (numLevels > 0) {
                numTris = new int[numLevels];
                // Store triangle counts from lowest LOD (highest index) to highest LOD (index 0)
                // This makes the lookup loop in controlRender more efficient.
                for (int i = 0; i < numLevels; i++) {
                    numTris[i] = mesh.getTriangleCount(i);
                }
            } else {
                logger.log(Level.WARNING, "LodControl attached to Geometry {0} but its Mesh has no LOD levels defined. " +
                        "Control will have no effect.", spatial.getName());
            }
        } else {
            numLevels = 0;
            numTris = null;
            lastDistance = 0f;
            lastLevel = 0;
        }
    }

    @Override
    public Object jmeClone() {
        LodControl clone = (LodControl) super.jmeClone();
        clone.lastDistance = 0;
        clone.lastLevel = 0;
        clone.numTris = (numTris != null) ? numTris.clone() : null;
        return clone;
    }

    @Override
    protected void controlUpdate(float tpf) {
        // LOD is determined during controlRender to react to camera changes
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        if (numLevels == 0) {
            return;
        }

        BoundingVolume bv = spatial.getWorldBound();

        Camera cam = vp.getCamera();
        float atanNH = FastMath.atan(cam.getFrustumNear() * cam.getFrustumTop());
        float ratio = (FastMath.PI / (8f * atanNH));
        float currDistance = bv.distanceTo(cam.getLocation()) / ratio;
        int newLevel;

        if (Math.abs(currDistance - lastDistance) <= distTolerance) {
            newLevel = lastLevel; // we haven't moved relative to the model
        } else if (lastDistance > currDistance && lastLevel == 0) {
            newLevel = lastLevel; // Getting closer, already at highest detail (level 0)
        } else if (lastDistance < currDistance && lastLevel == numLevels - 1) {
            newLevel = lastLevel; // Getting further, already at lowest detail
        } else {
            // Update lastDistance for subsequent checks
            lastDistance = currDistance;

            // estimate area of polygon via bounding volume
            float area = AreaUtils.calcScreenArea(bv, lastDistance, cam.getWidth());
            float trisToDraw = area * trisPerPixel;

            // Find the appropriate LOD level
            // Start with the lowest detail (highest index)
            newLevel = numLevels - 1;
            // Iterate from highest detail (index 0) to lowest, breaking when trisToDraw is enough
            for (int i = 0; i < numLevels; i++) {
                if (trisToDraw >= numTris[i]) {
                    newLevel = i;
                    break; // Found the highest detail level that satisfies the criteria
                }
            }
            // Only set LOD if it's actually changing to avoid unnecessary calls
            if (newLevel != lastLevel) {
                spatial.setLodLevel(newLevel);
            }
            lastLevel = newLevel;
        }
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(trisPerPixel, "trisPerPixel", 1f);
        oc.write(distTolerance, "distTolerance", 1f);
        oc.write(numLevels, "numLevels", 0);
        oc.write(numTris, "numTris", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        trisPerPixel = ic.readFloat("trisPerPixel", 1f);
        distTolerance = ic.readFloat("distTolerance", 1f);
        numLevels = ic.readInt("numLevels", 0);
        numTris = ic.readIntArray("numTris", null);
    }
}
