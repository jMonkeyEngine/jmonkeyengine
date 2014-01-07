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

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.terrain.geomipmap.lodcalc.LodCalculator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * An extension of the TerrainLodControl that handles
 * multiple terrains at once. This is to be used if you 
 * have your own tiling/paging terrain system, such as
 * TerrainGrid.
 * 
 * @author Brent Owens
 */
public class MultiTerrainLodControl extends TerrainLodControl {
    
    List<TerrainQuad> terrains = new ArrayList<TerrainQuad>();
    private List<TerrainQuad> addedTerrains = new ArrayList<TerrainQuad>();
    private List<TerrainQuad> removedTerrains = new ArrayList<TerrainQuad>();

    public MultiTerrainLodControl(List<Camera> cameras) {
        this.cameras = cameras;
        lodCalculator = new DistanceLodCalculator(65, 2.7f);
    }

    public MultiTerrainLodControl(Camera camera) {
        List<Camera> cams = new ArrayList<Camera>();
        cams.add(camera);
        this.cameras = cams;
        lodCalculator = new DistanceLodCalculator(65, 2.7f);
    }
    
    /**
     * Add a terrain that will have its LOD handled by this control.
     * It will be added next update run. You should only call this from
     * the render thread.
     */
    public void addTerrain(TerrainQuad tq) {
        addedTerrains.add(tq);
    }
    
    /**
     * Add a terrain that will no longer have its LOD handled by this control.
     * It will be removed next update run. You should only call this from
     * the render thread.
     */
    public void removeTerrain(TerrainQuad tq) {
        removedTerrains.add(tq);
    }
    
    @Override
    protected UpdateLOD getLodThread(List<Vector3f> locations, LodCalculator lodCalculator) {
        return new UpdateMultiLOD(locations, lodCalculator);
    }
    
    @Override
    protected void prepareTerrain() {
        if (!addedTerrains.isEmpty()) {
            for (TerrainQuad t : addedTerrains) {
                if (!terrains.contains(t))
                    terrains.add(t);
            }
            addedTerrains.clear();
        }
        
        if (!removedTerrains.isEmpty()) {
            terrains.removeAll(removedTerrains);
            removedTerrains.clear();
        }
        
        for (TerrainQuad terrain : terrains)
            terrain.cacheTerrainTransforms();// cache the terrain's world transforms so they can be accessed on the separate thread safely
    }
    
    /**
     * Overrides the parent UpdateLOD runnable to process
     * multiple terrains.
     */
    protected class UpdateMultiLOD extends UpdateLOD {
        
        
        protected UpdateMultiLOD(List<Vector3f> camLocations, LodCalculator lodCalculator) {
            super(camLocations, lodCalculator);
        }
        
        @Override
        public HashMap<String, UpdatedTerrainPatch> call() throws Exception {
            
            setLodCalcRunning(true);
            
            HashMap<String,UpdatedTerrainPatch> updated = new HashMap<String,UpdatedTerrainPatch>();
            
            for (TerrainQuad terrainQuad : terrains) {
                // go through each patch and calculate its LOD based on camera distance
                terrainQuad.calculateLod(camLocations, updated, lodCalculator); // 'updated' gets populated here
            }
            
            for (TerrainQuad terrainQuad : terrains) {
                // then calculate the neighbour LOD values for seaming
                terrainQuad.findNeighboursLod(updated);
            }
            
            for (TerrainQuad terrainQuad : terrains) {
                // check neighbour quads that need their edges seamed
                terrainQuad.fixEdges(updated);
            }
            
            for (TerrainQuad terrainQuad : terrains) {
                // perform the edge seaming, if it requires it
                terrainQuad.reIndexPages(updated, lodCalculator.usesVariableLod());
            }
            
            //setUpdateQuadLODs(updated); // set back to main ogl thread
            setLodCalcRunning(false);
            
            return updated;
        }
    }
}
