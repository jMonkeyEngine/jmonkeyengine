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
import com.jme3.util.clone.Cloner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

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
 * When the control or the terrain are removed from the scene, you should call
 * TerrainLodControl.detachAndCleanUpControl() to remove any threads it created
 * to handle the LOD processing. If you supply your own executor service, then
 * you have to handle its thread termination yourself.
 *
 * @author Brent Owens
 */
public class TerrainLodControl extends AbstractControl {

    private Terrain terrain;
    protected List<Camera> cameras;
    private List<Vector3f> cameraLocations = new ArrayList<Vector3f>();
    protected LodCalculator lodCalculator;
    private boolean hasResetLod = false; // used when enabled is set to false

    private HashMap<String,UpdatedTerrainPatch> updatedPatches;
    private final Object updatePatchesLock = new Object();

    protected List<Vector3f> lastCameraLocations; // used for LOD calc
    private AtomicBoolean lodCalcRunning = new AtomicBoolean(false);
    private int lodOffCount = 0;

    protected ExecutorService executor;
    protected Future<HashMap<String, UpdatedTerrainPatch>> indexer;
    private boolean forceUpdate = true;

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

    /**
     * Set your own custom executor to be used. The control will use
     * this instead of creating its own.
     */
    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    protected ExecutorService createExecutorService() {
        return Executors.newSingleThreadExecutor(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread th = new Thread(r);
                th.setName("jME3 Terrain Thread");
                th.setDaemon(true);
                return th;
            }
        });
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
            cameraLocations.clear();
            for (Camera c : cameras) // populate them
            {
                cameraLocations.add(c.getLocation());
            }
            updateLOD(cameraLocations, lodCalculator);
        }
    }

    /**
     * Call this when you remove the terrain or this control from the scene.
     * It will clear up any threads it had.
     */
    public void detachAndCleanUpControl() {
        if (executor != null)
            executor.shutdownNow();
        getSpatial().removeControl(this);
    }

    // do all of the LOD calculations
    protected void updateLOD(List<Vector3f> locations, LodCalculator lodCalculator) {
        if(getSpatial() == null){
            return;
        }

        // update any existing ones that need updating
        updateQuadLODs();

        if (lodCalculator.isLodOff()) {
            // we want to calculate the base lod at least once
            if (lodOffCount == 1)
                return;
            else
                lodOffCount++;
        } else
            lodOffCount = 0;

        if (lastCameraLocations != null) {
            if (!forceUpdate && lastCameraLocationsTheSame(locations) && !lodCalculator.isLodOff())
                return; // don't update if in same spot
            else
                lastCameraLocations = cloneVectorList(locations);
            forceUpdate = false;
        }
        else {
            lastCameraLocations = cloneVectorList(locations);
            return;
        }

        if (isLodCalcRunning()) {
            return;
        }
        setLodCalcRunning(true);

        if (executor == null)
            executor = createExecutorService();

        prepareTerrain();

        UpdateLOD updateLodThread = getLodThread(locations, lodCalculator);
        indexer = executor.submit(updateLodThread);
    }

    /**
     * Force the LOD to update instantly, does not wait for the camera to move.
     * It will reset once it has updated.
     */
    public void forceUpdate() {
        this.forceUpdate = true;
    }

    protected void prepareTerrain() {
        TerrainQuad terrain = (TerrainQuad)getSpatial();
        terrain.cacheTerrainTransforms();// cache the terrain's world transforms so they can be accessed on the separate thread safely
    }

    protected UpdateLOD getLodThread(List<Vector3f> locations, LodCalculator lodCalculator) {
        return new UpdateLOD(locations, lodCalculator);
    }

    /**
     * Back on the ogl thread: update the terrain patch geometries
     */
    private void updateQuadLODs() {
        if (indexer != null) {
            if (indexer.isDone()) {
                try {

                    HashMap<String, UpdatedTerrainPatch> updated = indexer.get();
                    if (updated != null) {
                        // do the actual geometry update here
                        for (UpdatedTerrainPatch utp : updated.values()) {
                            utp.updateAll();
                        }
                    }

                } catch (InterruptedException ex) {
                    Logger.getLogger(TerrainLodControl.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ExecutionException ex) {
                    Logger.getLogger(TerrainLodControl.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    indexer = null;
                }
            }
        }
    }

    private boolean lastCameraLocationsTheSame(List<Vector3f> locations) {
        boolean theSame = true;
        for (Vector3f l : locations) {
            for (Vector3f v : lastCameraLocations) {
                if (!v.equals(l) ) {
                    theSame = false;
                    return false;
                }
            }
        }
        return theSame;
    }

    protected synchronized boolean isLodCalcRunning() {
        return lodCalcRunning.get();
    }

    protected synchronized void setLodCalcRunning(boolean running) {
        lodCalcRunning.set(running);
    }

    private List<Vector3f> cloneVectorList(List<Vector3f> locations) {
        List<Vector3f> cloned = new ArrayList<Vector3f>();
        for(Vector3f l : locations)
            cloned.add(l.clone());
        return cloned;
    }

    @Override
    public Object jmeClone() {
        try {
            return super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void cloneFields( Cloner cloner, Object original ) {
        super.cloneFields(cloner, original);

        this.lodCalculator = cloner.clone(lodCalculator);
        try {
            // Not deep clone of the cameras themselves
            this.cameras = cloner.javaClone(cameras);
        } catch( CloneNotSupportedException e ) {
            throw new RuntimeException("Error cloning", e);
        }
    }


    @Override
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


    /**
     * Calculates the LOD of all child terrain patches.
     */
    protected class UpdateLOD implements Callable<HashMap<String,UpdatedTerrainPatch>> {
        protected List<Vector3f> camLocations;
        protected LodCalculator lodCalculator;

        protected UpdateLOD(List<Vector3f> camLocations, LodCalculator lodCalculator) {
            this.camLocations = camLocations;
            this.lodCalculator = lodCalculator;
        }

        public HashMap<String, UpdatedTerrainPatch> call() throws Exception {
            //long start = System.currentTimeMillis();
            //if (isLodCalcRunning()) {
            //    return null;
            //}
            setLodCalcRunning(true);

            TerrainQuad terrainQuad = (TerrainQuad)getSpatial();

            // go through each patch and calculate its LOD based on camera distance
            HashMap<String,UpdatedTerrainPatch> updated = new HashMap<String,UpdatedTerrainPatch>();
            boolean lodChanged = terrainQuad.calculateLod(camLocations, updated, lodCalculator); // 'updated' gets populated here

            if (!lodChanged) {
                // not worth updating anything else since no one's LOD changed
                setLodCalcRunning(false);
                return null;
            }


            // then calculate its neighbour LOD values for seaming in the shader
            terrainQuad.findNeighboursLod(updated);

            terrainQuad.fixEdges(updated); // 'updated' can get added to here

            terrainQuad.reIndexPages(updated, lodCalculator.usesVariableLod());

            //setUpdateQuadLODs(updated); // set back to main ogl thread

            setLodCalcRunning(false);

            return updated;
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
