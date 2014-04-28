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
import com.jme3.gde.terraineditor.tools.TerrainTool.Meshes;
import com.jme3.math.Plane;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.terrain.Terrain;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Shirkit
 */
public class SlopeTerrainToolAction extends AbstractTerrainToolAction {

    private final Vector3f current;
    private Vector3f point1;
    private Vector3f point2;
    private final float radius;
    private final float weight;
    private List<Vector2f> undoLocs;
    private List<Float> undoHeights;
    private final boolean precise;
    private final boolean lock;
    private final Meshes mesh;

    public SlopeTerrainToolAction(Vector3f current, Vector3f point1, Vector3f point2, float radius, float weight, boolean precise, boolean lock, Meshes mesh) {
        this.current = current.clone();
        this.point1 = point1;
        this.point2 = point2;
        this.radius = radius;
        this.weight = weight;
        this.precise = precise;
        this.lock = lock;
        this.mesh = mesh;
        name = "Slope terrain";
    }

    @Override
    protected Object doApplyTool(AbstractSceneExplorerNode rootNode) {
        Terrain terrain = getTerrain(rootNode.getLookup().lookup(Node.class));
        if (terrain == null)
            return null;

        modifyHeight(terrain, point1, point2, current, radius, weight, precise, lock, mesh);

        return terrain;
    }

    @Override
    protected void doUndoTool(AbstractSceneExplorerNode rootNode, Object undoObject) {
        if (undoObject == null)
            return;

        if (undoLocs == null || undoHeights == null)
            return;

        resetHeight((Terrain) undoObject, undoLocs, undoHeights, precise);
    }

    private void modifyHeight(Terrain terrain, Vector3f point1, Vector3f point2, Vector3f current, float radius, float weight, boolean precise, boolean lock, Meshes mesh) {
        // Make sure we go for the right direction, or we could be creating a slope to the oposite side
        if (point1.y > point2.y) {
            Vector3f temp = point1;
            point1 = point2;
            point2 = temp;
        }

        Vector3f subtract = point2.subtract(point1);

        int radiusStepsX = (int) (radius / ((Node) terrain).getLocalScale().x);
        int radiusStepsZ = (int) (radius / ((Node) terrain).getLocalScale().z);

        float xStepAmount = ((Node) terrain).getLocalScale().x;
        float zStepAmount = ((Node) terrain).getLocalScale().z;

        List<Vector2f> locs = new ArrayList<Vector2f>();
        List<Float> heights = new ArrayList<Float>();
        undoHeights = new ArrayList<Float>();

        Plane p1 = new Plane();
        Plane p2 = new Plane();
        p1.setOriginNormal(point1, point1.subtract(point2).normalize());
        p2.setOriginNormal(point2, point1.subtract(point2).normalize());

        for (int z = -radiusStepsZ; z < radiusStepsZ; z++)
            for (int x = -radiusStepsX; x < radiusStepsX; x++) {

                float locX = current.x + (x * xStepAmount);
                float locZ = current.z + (z * zStepAmount);

                if (ToolUtils.isInMesh(locX - current.x, locZ - current.z, radius, mesh)) { // see if it is in the radius of the tool
                    Vector2f terrainLoc = new Vector2f(locX, locZ);

                    // adjust height based on radius of the tool
                    float terrainHeightAtLoc = terrain.getHeightmapHeight(terrainLoc) * ((Node) terrain).getWorldScale().y;
                    float distance = point1.distance(new Vector3f(locX, terrainHeightAtLoc, locZ).subtractLocal(point1).project(subtract).addLocal(point1));
                    float desiredHeight = point1.y + (point2.y - point1.y) * distance;
                    if (!lock || (lock && p1.whichSide(new Vector3f(locX, 0f, locZ)) != p2.whichSide(new Vector3f(locX, 0f, locZ))))
                        if (!precise) {
                            float epsilon = 0.1f * weight; // rounding error for snapping

                            float adj = 0;
                            if (terrainHeightAtLoc < desiredHeight)
                                adj = 1;
                            else if (terrainHeightAtLoc > desiredHeight)
                                adj = -1;

                            adj *= weight;
                            
                            if (mesh.equals(Meshes.Sphere))
                                adj *= ToolUtils.calculateRadiusPercent(radius, locX - current.x, locZ - current.z);
                            
                            // test if adjusting too far and then cap it
                            if (adj > 0 && ToolUtils.floatGreaterThan((terrainHeightAtLoc + adj), desiredHeight, epsilon))
                                adj = desiredHeight - terrainHeightAtLoc;
                            else
                                if (adj < 0 && ToolUtils.floatLessThan((terrainHeightAtLoc + adj), desiredHeight, epsilon))
                                    adj = terrainHeightAtLoc - desiredHeight;

                            if (!ToolUtils.floatEquals(adj, 0, 0.001f)) {
                                locs.add(terrainLoc);
                                heights.add(adj);
                            }
                        } else {
                            locs.add(terrainLoc);
                            heights.add(desiredHeight / ((Node) terrain).getLocalScale().y);
                            undoHeights.add(terrainHeightAtLoc / ((Node) terrain).getLocalScale().y);
                        }


                }
            }
        undoLocs = locs;
        if (!precise)
            undoHeights = heights;

        // do the actual height adjustment
        if (precise)
            terrain.setHeight(locs, heights);
        else
            terrain.adjustHeight(locs, heights);

        ((Node) terrain).updateModelBound(); // or else we won't collide with it where we just edited
    }

    private void resetHeight(Terrain terrain, List<Vector2f> undoLocs, List<Float> undoHeights, boolean precise) {
        if (precise)
            terrain.setHeight(undoLocs, undoHeights);
        else {
            List<Float> neg = new ArrayList<Float>();
            for (Float f : undoHeights) {
                neg.add(f * -1f);
            }
            terrain.adjustHeight(undoLocs, neg);
        }
        ((Node) terrain).updateModelBound();
    }
}
