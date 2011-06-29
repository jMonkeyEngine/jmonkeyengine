/*
 * Copyright (c) 2009-2011 jMonkeyEngine
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
package com.jme3.gde.terraineditor.tools;

import com.jme3.gde.core.sceneexplorer.nodes.AbstractSceneExplorerNode;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.terrain.Terrain;
import java.util.ArrayList;
import java.util.List;

/**
 * Level the terrain to a desired height, executed from the OpenGL thread.
 * It will pull down or raise terrain towards the desired height, still
 * using the radius of the tool and the weight. There are some slight rounding
 * errors that are corrected with float epsilon testing.
 * 
 * @author Brent Owens
 */
public class LevelTerrainToolAction extends AbstractTerrainToolAction {
    
    private Vector3f worldLoc;
    private float radius;
    private float height;
    private Vector3f levelTerrainLocation;
    
    List<Vector2f> undoLocs;
    List<Float> undoHeights;

    public LevelTerrainToolAction(Vector3f markerLocation, float radius, float height, Vector3f levelTerrainLocation) {
        this.worldLoc = markerLocation.clone();
        this.radius = radius;
        this.height = height;
        this.levelTerrainLocation = levelTerrainLocation;
        name = "Level terrain";
    }

    @Override
    protected Object doApplyTool(AbstractSceneExplorerNode rootNode) {
        Terrain terrain = getTerrain(rootNode.getLookup().lookup(Node.class));
        if (terrain == null)
            return null;
        modifyHeight(terrain, radius, height);
        return terrain;
    }
    
    @Override
    protected void doUndoTool(AbstractSceneExplorerNode rootNode, Object undoObject) {
        if (undoObject == null)
            return;
        if (undoLocs == null || undoHeights == null)
            return;
        resetHeight((Terrain)undoObject, undoLocs, undoHeights);
    }

    private void modifyHeight(Terrain terrain, float radius, float height) {
        if (levelTerrainLocation == null)
            return;

        float desiredHeight = levelTerrainLocation.y;

        int radiusStepsX = (int)(radius / ((Node)terrain).getLocalScale().x);
        int radiusStepsZ = (int)(radius / ((Node)terrain).getLocalScale().z);

        float xStepAmount = ((Node)terrain).getLocalScale().x;
        float zStepAmount = ((Node)terrain).getLocalScale().z;

        List<Vector2f> locs = new ArrayList<Vector2f>();
        List<Float> heights = new ArrayList<Float>();

        for (int z=-radiusStepsZ; z<radiusStepsZ; z++) {
            for (int x=-radiusStepsZ; x<radiusStepsX; x++) {

                float locX = worldLoc.x + (x*xStepAmount);
                float locZ = worldLoc.z + (z*zStepAmount);
                
                // see if it is in the radius of the tool
                if (ToolUtils.isInRadius(locX-worldLoc.x,locZ-worldLoc.z,radius)) {

                    Vector2f terrainLoc = new Vector2f(locX, locZ);
                    // adjust height based on radius of the tool
                    float terrainHeightAtLoc = terrain.getHeightmapHeight(terrainLoc)*((Node)terrain).getWorldScale().y;
                    float radiusWeight = ToolUtils.calculateRadiusPercent(radius, locX-worldLoc.x, locZ-worldLoc.z);

                    float epsilon = 0.1f*height; // rounding error for snapping
                    
                    float adj = 0;
                    if (terrainHeightAtLoc < desiredHeight)
                        adj = 1;
                    else if (terrainHeightAtLoc > desiredHeight)
                        adj = -1;
                            
                    adj *= radiusWeight * height;

                    // test if adjusting too far and then cap it
                    if (adj > 0 && ToolUtils.floatGreaterThan((terrainHeightAtLoc + adj), desiredHeight, epsilon))
                        adj = desiredHeight - terrainHeightAtLoc;
                    else if (adj < 0 && ToolUtils.floatLessThan((terrainHeightAtLoc + adj), desiredHeight, epsilon))
                        adj = terrainHeightAtLoc - desiredHeight;
  
                    if (!ToolUtils.floatEquals(adj, 0, 0.001f)) {
                        locs.add(terrainLoc);
                        heights.add(adj);
                    }
                    
                }
            }
        }
        undoLocs = locs;
        undoHeights = heights;
        
        // do the actual height adjustment
        terrain.adjustHeight(locs, heights);
        
        ((Node)terrain).updateModelBound(); // or else we won't collide with it where we just edited

    }

    
    private void resetHeight(Terrain terrain, List<Vector2f> undoLocs, List<Float> undoHeights) {
        List<Float> neg = new ArrayList<Float>();
        for (Float f : undoHeights)
            neg.add( f * -1f );
        
        terrain.adjustHeight(undoLocs, neg);
        ((Node)terrain).updateModelBound();
    }
    
}
