/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.terrain.geomipmap;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.terrain.Terrain;
import com.jme3.terrain.geomipmap.lodcalc.LodCalculator;
import java.util.List;

/**
 * Updates grid offsets and cell positions.
 * As well as updating LOD.
 * 
 * @author sploreg
 */
public class TerrainGridLodControl extends TerrainLodControl {
    
    public TerrainGridLodControl(Terrain terrain, Camera camera) {
        super(terrain, camera);
    }
    
    @Override
    protected void updateLOD(List<Vector3f> locations, LodCalculator lodCalculator) {
        TerrainGrid terrainGrid = (TerrainGrid)getSpatial();
        
        // for now, only the first camera is handled.
        // to accept more, there are two ways:
        // 1: every camera has an associated grid, then the location is not enough to identify which camera location has changed
        // 2: grids are associated with locations, and no incremental update is done, we load new grids for new locations, and unload those that are not needed anymore
        Vector3f cam = locations.isEmpty() ? Vector3f.ZERO.clone() : locations.get(0);
        Vector3f camCell = terrainGrid.getCamCell(cam); // get the grid index value of where the camera is (ie. 2,1)
        if (terrainGrid.cellsLoaded > 1) {                  // Check if cells are updated before updating gridoffset.
            terrainGrid.gridOffset[0] = Math.round(camCell.x * (terrainGrid.size / 2));
            terrainGrid.gridOffset[1] = Math.round(camCell.z * (terrainGrid.size / 2));
            terrainGrid.cellsLoaded = 0;
        }
        if (camCell.x != terrainGrid.currentCamCell.x || camCell.z != terrainGrid.currentCamCell.z || !terrainGrid.runOnce) {
            // if the camera has moved into a new cell, load new terrain into the visible 4 center quads
            terrainGrid.updateChildren(camCell);
            for (TerrainGridListener l : terrainGrid.listeners) {
                l.gridMoved(camCell);
            }
        }
        terrainGrid.runOnce = true;
        super.updateLOD(locations, lodCalculator);
    }
}
