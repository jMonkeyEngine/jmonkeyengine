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

package com.jme3.gde.terraineditor;

import com.jme3.asset.AssetManager;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.scene.controller.SceneToolController;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.gde.core.sceneexplorer.nodes.JmeSpatial;
import com.jme3.gde.terraineditor.TerrainEditorTopComponent.TerrainEditButton;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.IntMap.Entry;
import java.util.concurrent.Callable;

/**
 * The controller for the terrain modification tools. It will in turn interact
 * with the TerrainEditorController to actually modify the terrain in the scene.
 *
 * Maintains the edit tool state: what tool is activated and what should be done with it.
 * 
 * @author bowens
 */
public class TerrainToolController extends SceneToolController {

    private JmeSpatial jmeRootNode;
    private TerrainEditButton currentEditButtonState = TerrainEditButton.none;
    private Geometry marker;
    private TerrainEditorController editorController;
    private float heightToolRadius;
    private float heightToolHeight;
    private float heightAmount;
    private float paintAmount;
    private int selectedTextureIndex = -1;

    public TerrainToolController(Node toolsNode, AssetManager manager, JmeNode rootNode) {
        super(toolsNode, manager);
        this.jmeRootNode = rootNode;
    }

    public void setEditorController(TerrainEditorController editorController) {
        this.editorController = editorController;
    }

    /**
     * assumes [0,200]
     */
    public void setHeightToolHeight(float heightToolHeight) {
        this.heightToolHeight = heightToolHeight;
        this.heightAmount = heightToolHeight/100f;
        this.paintAmount = heightToolHeight/200f;
    }

    public void setHeightToolRadius(float radius) {
        this.heightToolRadius = radius;
        setEditToolSize(radius);
    }

    public void setSelectedTextureIndex(int index) {
        this.selectedTextureIndex = index;
    }


    @Override
    protected void initTools() {
        super.initTools();

        marker = new Geometry("edit marker");
        Mesh m = new Sphere(8, 8, 3);
        marker.setMesh(m);
        Material mat = new Material(manager, "Common/MatDefs/Misc/WireColor.j3md");
        mat.setColor("Color", ColorRGBA.Green);
        marker.setMaterial(mat);
        marker.setLocalTranslation(0,0,0);
    }

    protected void setMarkerRadius(float radius) {
        //((Sphere)marker.getMesh()).set;
    }

    public TerrainEditButton getCurrentEditButtonState() {
        return currentEditButtonState;
    }

    public void setTerrainEditButtonState(final TerrainEditButton state) {

        currentEditButtonState = state;
        if (state == TerrainEditButton.none) {
            hideEditTool();
        } else if (state == TerrainEditButton.raiseTerrain || state == TerrainEditButton.lowerTerrain) {
            showEditTool(state);
        } else if (state == TerrainEditButton.paintTerrain || state == TerrainEditButton.eraseTerrain) {
            showEditTool(state);
        }
    }


    public void hideEditTool() {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {
            
            public Object call() throws Exception {
                doHideEditTool();
                return null;
            }
        });
    }

    private void doHideEditTool() {
        marker.removeFromParent();
    }

    public void showEditTool(final TerrainEditButton terrainEditButton) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                doShowEditTool(terrainEditButton);
                return null;
            }
        });
        
    }

    private void doShowEditTool(TerrainEditButton terrainEditButton) {
        //TODO show different tool marker depending on terrainEditButton type
        
        toolsNode.attachChild(marker);

        
    }

    public void setEditToolSize(final float size) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                doSetEditToolSize(size);
                return null;
            }
        });
    }

    private void doSetEditToolSize(float size) {
        for (Entry e: marker.getMesh().getBuffers())
            ((VertexBuffer)e.getValue()).resetObject();
        ((Sphere)marker.getMesh()).updateGeometry(8, 8, size);
    }

    public void doMoveEditTool(Vector3f pos) {
        if (marker != null) {
            marker.setLocalTranslation(pos);
            //System.out.println(marker.getLocalTranslation());
        }
    }

    public Vector3f getMarkerLocation() {
        if (marker != null)
            return marker.getLocalTranslation();
        else
            return null;
    }

    public boolean isTerrainEditButtonEnabled() {
        return getCurrentEditButtonState() != TerrainEditButton.none;
    }

    /**
     * raise/lower/paint the terrain
     */
    public void doTerrainEditToolActivated() {

        if (TerrainEditButton.raiseTerrain == getCurrentEditButtonState() ) {
            editorController.doModifyTerrainHeight(getMarkerLocation(), heightToolRadius, heightAmount);
        }
        else if (TerrainEditButton.lowerTerrain == getCurrentEditButtonState() ) {
            editorController.doModifyTerrainHeight(getMarkerLocation(), heightToolRadius, -heightAmount);
        }
        else if(TerrainEditButton.paintTerrain == getCurrentEditButtonState()) {
            editorController.doPaintTexture(selectedTextureIndex, getMarkerLocation(), heightToolRadius, paintAmount);
        }
        else if (TerrainEditButton.eraseTerrain == getCurrentEditButtonState() ) {
            editorController.doPaintTexture(selectedTextureIndex, getMarkerLocation(), heightToolRadius, -paintAmount);
        }
    }
}
