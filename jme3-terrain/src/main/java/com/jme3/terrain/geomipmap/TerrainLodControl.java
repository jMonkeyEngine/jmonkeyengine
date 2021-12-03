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
import com.jme3.terrain.Terrain;
import com.jme3.terrain.executor.TerrainExecutorService;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.terrain.geomipmap.lodcalc.LodCalculator;
import com.jme3.util.SafeArrayList;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.util.ArrayList;
import static java.util.Collections.singletonList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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

    /**
     * The list of cameras for when terrain supports multiple cameras (ie split screen)
     */
    protected SafeArrayList<Camera> cameras;
    protected SafeArrayList<Vector3f> cameraLocations;
    protected SafeArrayList<Vector3f> lastCameraLocations;

    protected AtomicBoolean lodCalcRunning;

    /**
     * The previous location of {@link #camera}.
     */
    protected Vector3f previousCameraLocation;

    /**
     * The camera from render view port.
     */
    protected Camera camera;

    protected Terrain terrain;
    protected LodCalculator lodCalculator;
    protected Future<HashMap<String, UpdatedTerrainPatch>> indexer;

    private int lodOffCount;

    /**
     * The flag of using a camera from render viewport instead cameras from {@link #cameras}.
     */
    protected boolean useRenderCamera;

    protected boolean forceUpdate;
    protected boolean hasResetLod; // used when enabled is set to false

    public TerrainLodControl() {
        hasResetLod = false;
        forceUpdate = true;
        previousCameraLocation = new Vector3f();
        cameras = new SafeArrayList<>(Camera.class);
        cameraLocations = new SafeArrayList<>(Vector3f.class);
        lastCameraLocations = new SafeArrayList<>(Vector3f.class);
        lodCalcRunning = new AtomicBoolean(false);
        lodOffCount = 0;
        lodCalculator = makeLodCalculator(); // a default calculator
    }

    protected DistanceLodCalculator makeLodCalculator() {
        return new DistanceLodCalculator(65, 2.7f);
    }

    public TerrainLodControl(final Terrain terrain) {
        this();
        this.terrain = terrain;
    }

    public TerrainLodControl(final Camera camera) {
        this();
        setCamera(camera);
    }

    public TerrainLodControl(final Terrain terrain, final Camera camera) {
        this(terrain);
        setCamera(camera);
    }

    /**
     * Only uses the first camera right now.
     * @param terrain to act upon (must be a Spatial)
     * @param cameras one or more cameras to reference for LOD calc
     */
    public TerrainLodControl(final Terrain terrain, final List<Camera> cameras) {
        this(terrain);
        setCameras(cameras);
    }

    /**
     * @param useRenderCamera true to use camera from the render viewport
     */
    public void setUseRenderCamera(final boolean useRenderCamera) {
        this.useRenderCamera = useRenderCamera;
    }

    /**
     * @return true to use camera from the render viewport
     */
    public boolean isUseRenderCamera() {
        return useRenderCamera;
    }

    @Override
    protected void controlRender(final RenderManager rm, final ViewPort vp) {

        if (!isUseRenderCamera()) {
            return;
        } else if (camera == vp.getCamera()) {
            return;
        }

        camera = vp.getCamera();
        previousCameraLocation.set(camera.getLocation());
    }

    @Override
    protected void controlUpdate(float tpf) {

        if (lodCalculator == null) {
            return;
        }

        if (!enabled) {
            if (!hasResetLod) {
                // this will get run once
                hasResetLod = true;
                lodCalculator.turnOffLod();
            }
        }

        // if we use a camera from render
        if (isUseRenderCamera()) {
            updateLOD(lodCalculator);
        }
        // if we use set cameras
        else if (!cameras.isEmpty()) {

            // need to have count of positions the same with count of cameras
            if (cameraLocations.size() != cameras.size()) {
                cameraLocations.clear();
                for (int i = 0; i < cameras.size(); i++) {
                    cameraLocations.add(new Vector3f());
                }
            }

            // we need to update current camera positions
            for (int i = 0; i < cameras.size(); i++) {
                cameraLocations.get(i).set(cameras.get(i).getLocation());
            }

            updateLOD(cameraLocations, lodCalculator);
        }
    }

    /**
     * Call this when you remove the terrain or this control from the scene.
     * It will clear up any threads it had.
     */
    public void detachAndCleanUpControl() {

        if (indexer != null) {
            indexer.cancel(true);
            indexer = null;
        }

        getSpatial().removeControl(this);
    }

    // Do all the LOD calculations.
    protected void updateLOD(final LodCalculator lodCalculator) {

        if (getSpatial() == null || camera == null) {
            return;
        }

        // update any existing ones that need updating
        updateQuadLODs();

        if (updateLodOffCount(lodCalculator)) {
            return;
        }

        final Vector3f currentLocation = camera.getLocation();

        if (!forceUpdate && previousCameraLocation.equals(currentLocation) && !lodCalculator.isLodOff()) {
            return; // don't update if in same spot
        } else {
            previousCameraLocation.set(currentLocation);
        }

        forceUpdate = false;

        if (!lodCalcRunning.compareAndSet(false, true)) {
            return;
        }

        prepareTerrain();

        final TerrainExecutorService executorService = TerrainExecutorService.getInstance();
        indexer = executorService.submit(createLodUpdateTask(singletonList(currentLocation), lodCalculator));
    }

    // Do all the LOD calculations.
    protected void updateLOD(final SafeArrayList<Vector3f> locations, final LodCalculator lodCalculator) {

        if (getSpatial() == null || locations.isEmpty()) {
            return;
        }

        // update any existing ones that need updating
        updateQuadLODs();

        if (updateLodOffCount(lodCalculator)) {
            return;
        }

        if (!forceUpdate && locations.equals(lastCameraLocations) && !lodCalculator.isLodOff()) {
            return; // don't update if in same spot
        } else {

            // need to have count of last camera locations the same with count of locations
            if (lastCameraLocations.size() != locations.size()) {
                lastCameraLocations.clear();
                for (int i = 0; i < locations.size(); i++) {
                    lastCameraLocations.add(new Vector3f());
                }
            }

            // we need to update last camera locations to current
            for (int i = 0; i < locations.size(); i++) {
                lastCameraLocations.get(i).set(locations.get(i));
            }
        }

        forceUpdate = false;

        if (!lodCalcRunning.compareAndSet(false, true)) {
            return;
        }

        prepareTerrain();

        final TerrainExecutorService executorService = TerrainExecutorService.getInstance();
        indexer = executorService.submit(createLodUpdateTask(cloneVectorList(locations), lodCalculator));
    }

    protected boolean updateLodOffCount(final LodCalculator lodCalculator) {

        if (lodCalculator.isLodOff()) {
            // we want to calculate the base lod at least once
            if (lodOffCount == 1) {
                return true;
            } else {
                lodOffCount++;
            }
        } else {
            lodOffCount = 0;
        }

        return false;
    }

    /**
     * Force the LOD to update instantly, does not wait for the camera to move.
     * It will reset once it has updated.
     */
    public void forceUpdate() {
        this.forceUpdate = true;
    }

    protected void prepareTerrain() {
        TerrainQuad terrain = (TerrainQuad) getSpatial();
        // cache the terrain's world transforms so they can be accessed on the separate thread safely
        terrain.cacheTerrainTransforms();
    }

    protected UpdateLOD createLodUpdateTask(final List<Vector3f> locations, final LodCalculator lodCalculator) {
        return new UpdateLOD(locations, lodCalculator);
    }

    /**
     * Back on the ogl thread: update the terrain patch geometries
     */
    private void updateQuadLODs() {

        if (indexer == null || !indexer.isDone()) {
            return;
        }

        try {

            final HashMap<String, UpdatedTerrainPatch> updated = indexer.get();
            if (updated != null) {
                // do the actual geometry update here
                for (final UpdatedTerrainPatch utp : updated.values()) {
                    utp.updateAll();
                }
            }

        } catch (final InterruptedException | ExecutionException ex) {
            Logger.getLogger(TerrainLodControl.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            indexer = null;
        }
    }

    private List<Vector3f> cloneVectorList(SafeArrayList<Vector3f> locations) {

        final List<Vector3f> cloned = new ArrayList<>(locations.size());

        for (final Vector3f location : locations.getArray()) {
            cloned.add(location.clone());
        }

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
    public void cloneFields(final Cloner cloner, final Object original) {
        super.cloneFields(cloner, original);

        this.lodCalculator = cloner.clone(lodCalculator);
        this.cameras = new SafeArrayList<>(Camera.class, cameras);
        this.cameraLocations = new SafeArrayList<>(Vector3f.class);
        this.lastCameraLocations = new SafeArrayList<>(Vector3f.class);
        this.lodCalcRunning = new AtomicBoolean();
        this.previousCameraLocation = new Vector3f();
    }

    public void setCamera(final Camera camera) {
        this.cameras.clear();
        this.cameras.add(camera);
    }

    public void setCameras(final List<Camera> cameras) {
        this.cameras.clear();
        this.cameras.addAll(cameras);
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

    public void setLodCalculator(final LodCalculator lodCalculator) {
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
    protected class UpdateLOD implements Callable<HashMap<String, UpdatedTerrainPatch>> {

        protected final List<Vector3f> camLocations;
        protected final LodCalculator lodCalculator;

        protected UpdateLOD(final List<Vector3f> camLocations, final LodCalculator lodCalculator) {
            this.camLocations = camLocations;
            this.lodCalculator = lodCalculator;
        }

        @Override
        public HashMap<String, UpdatedTerrainPatch> call() throws Exception {

            TerrainQuad terrainQuad = (TerrainQuad) getSpatial();

            // go through each patch and calculate its LOD based on camera distance
            HashMap<String, UpdatedTerrainPatch> updated = new HashMap<>();
            // 'updated' gets populated here
            boolean lodChanged = terrainQuad.calculateLod(camLocations, updated, lodCalculator);

            if (!lodChanged) {
                // not worth updating anything else since no one's LOD changed
                lodCalcRunning.set(false);
                return null;
            }

            // then calculate its neighbour LOD values for seaming in the shader
            terrainQuad.findNeighboursLod(updated);
            // 'updated' can get added to here
            terrainQuad.fixEdges(updated);
            terrainQuad.reIndexPages(updated, lodCalculator.usesVariableLod());

            //setUpdateQuadLODs(updated); // set back to main ogl thread

            lodCalcRunning.set(false);

            return updated;
        }
    }

    @Override
    public void write(final JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write((Node)terrain, "terrain", null);
        oc.write(lodCalculator, "lodCalculator", null);
        oc.write(useRenderCamera, "useRenderCamera", false);
    }

    @Override
    public void read(final JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        terrain = (Terrain) ic.readSavable("terrain", null);
        lodCalculator = (LodCalculator) ic.readSavable("lodCalculator", new DistanceLodCalculator());
        useRenderCamera = ic.readBoolean("useRenderCamera", false);
    }
}
