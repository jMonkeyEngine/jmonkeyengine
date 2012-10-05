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
