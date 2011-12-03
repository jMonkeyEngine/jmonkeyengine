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
package com.jme3.terrain.geomipmap;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import com.jme3.terrain.Terrain;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.terrain.geomipmap.lodcalc.LodCalculator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tells the terrain to update its Level of Detail.
 * It needs the cameras to do this, and there could possibly
 * be several cameras in the scene, so it accepts a list
 * of cameras.
 * NOTE: right now it just uses the first camera passed in,
 * in the future it will use all of them to determine what
 * LOD to set.
 *
 * This control serializes, but it does not save the Camera reference.
 * This camera reference has to be manually added in when you load the
 * terrain to the scene!
 * 
 * @author Brent Owens
 */
public class TerrainLodControl extends AbstractControl {

    private Terrain terrain;
    private List<Camera> cameras;
    private List<Vector3f> cameraLocations = new ArrayList<Vector3f>();
    private LodCalculator lodCalculator;
    private boolean hasResetLod = false; // used when enabled is set to false

    public TerrainLodControl() {
    }

    public TerrainLodControl(Terrain terrain, Camera camera) {
        List<Camera> cams = new ArrayList<Camera>();
        cams.add(camera);
        this.terrain = terrain;
        this.cameras = cams;
        lodCalculator = new DistanceLodCalculator(65, 2.7f); // a default calculator
    }
    
    /**
     * Only uses the first camera right now.
     * @param terrain to act upon (must be a Spatial)
     * @param cameras one or more cameras to reference for LOD calc
     */
    public TerrainLodControl(Terrain terrain, List<Camera> cameras) {
        this.terrain = terrain;
        this.cameras = cameras;
        lodCalculator = new DistanceLodCalculator(65, 2.7f); // a default calculator
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    @Override
    public void update(float tpf) {
        controlUpdate(tpf);
    }
    
    @Override
    protected void controlUpdate(float tpf) {
        //list of cameras for when terrain supports multiple cameras (ie split screen)

        if (lodCalculator == null)
            return;
        
        if (!enabled) {
            if (!hasResetLod) {
                // this will get run once
                hasResetLod = true;
                lodCalculator.turnOffLod();
            }
        }
        
        if (cameras != null) {
            if (cameraLocations.isEmpty() && !cameras.isEmpty()) {
                for (Camera c : cameras) // populate them
                {
                    cameraLocations.add(c.getLocation());
                }
            }
            terrain.update(cameraLocations, lodCalculator);
        }
    }

    public Control cloneForSpatial(Spatial spatial) {
        if (spatial instanceof Terrain) {
            List<Camera> cameraClone = new ArrayList<Camera>();
            if (cameras != null) {
                for (Camera c : cameras) {
                    cameraClone.add(c);
                }
            }
            TerrainLodControl cloned = new TerrainLodControl((Terrain) spatial, cameraClone);
            cloned.setLodCalculator(lodCalculator.clone());
            return cloned;
        }
        return null;
    }

    public void setCamera(Camera camera) {
        List<Camera> cams = new ArrayList<Camera>();
        cams.add(camera);
        setCameras(cams);
    }
    
    public void setCameras(List<Camera> cameras) {
        this.cameras = cameras;
        cameraLocations.clear();
        for (Camera c : cameras) {
            cameraLocations.add(c.getLocation());
        }
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);
        if (spatial instanceof Terrain) {
            this.terrain = (Terrain) spatial;
        }
    }

    public void setTerrain(Terrain terrain) {
        this.terrain = terrain;
    }

    public LodCalculator getLodCalculator() {
        return lodCalculator;
    }

    public void setLodCalculator(LodCalculator lodCalculator) {
        this.lodCalculator = lodCalculator;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            // reset the lod levels to max detail for the terrain
            hasResetLod = false;
        } else {
            hasResetLod = true;
            lodCalculator.turnOnLod();
        }
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write((Node)terrain, "terrain", null);
        oc.write(lodCalculator, "lodCalculator", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        terrain = (Terrain) ic.readSavable("terrain", null);
        lodCalculator = (LodCalculator) ic.readSavable("lodCalculator", new DistanceLodCalculator());
    }

}
