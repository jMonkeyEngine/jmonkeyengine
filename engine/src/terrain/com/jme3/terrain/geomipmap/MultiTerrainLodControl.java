/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
        removedTerrains.remove(tq);
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
