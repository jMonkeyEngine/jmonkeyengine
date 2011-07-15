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
import com.jme3.gde.terraineditor.tools.TerrainTool;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
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
    private TerrainTool terrainTool;
    private TerrainEditorController editorController;
    private TerrainCameraController cameraController;
    
    private float toolRadius;
    private float toolWeight;
    private int selectedTextureIndex = -1;
    

    public TerrainToolController(Node toolsNode, AssetManager manager, JmeNode rootNode) {
        super(toolsNode, manager);
        this.jmeRootNode = rootNode;
    }

    public void setEditorController(TerrainEditorController editorController) {
        this.editorController = editorController;
    }

    public void setCameraController(TerrainCameraController cameraController) {
        this.cameraController = cameraController;
    }

    /**
     * @param heightToolHeight percent of the slider
     */
    public void setHeightToolHeight(float weight) {
        this.toolWeight = weight;
        if (terrainTool != null)
            terrainTool.weightChanged(weight);
    }

    /**
     * @param radius  percent of the slider
     */
    public void setHeightToolRadius(float radius) {
        this.toolRadius = radius;
        setEditToolSize(radius);
    }

    public void setSelectedTextureIndex(int index) {
        this.selectedTextureIndex = index;
    }

    public void setTerrainEditButtonState(final TerrainTool tool) {
        showEditTool(tool);
        
    }

    public void showEditTool(final TerrainTool terrainEditButton) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                doShowEditTool(terrainEditButton);
                return null;
            }
        });
        
    }

    /**
     * show different tool marker depending on terrainEditButton type
     */
    private void doShowEditTool(TerrainTool tool) {
        // remove the old tool markers
        if (terrainTool != null)
            terrainTool.hideMarkers();
        
        terrainTool = tool;
        if (terrainTool != null) {
            terrainTool.radiusChanged(toolRadius);
            terrainTool.weightChanged(toolWeight);
            terrainTool.activate(manager, toolsNode);
        }
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
        if (terrainTool != null)
            terrainTool.radiusChanged(size);
    }

    public void doMoveEditTool(Vector3f pos) {
        if (terrainTool != null) {
            terrainTool.markerMoved(pos);
        }
    }

    public Vector3f getMarkerLocation() {
        if (terrainTool != null) {
            return terrainTool.getMarkerPrimaryLocation();
        }
        return null;
    }

    public boolean isTerrainEditButtonEnabled() {
        return terrainTool != null;
    }

    /**
     * Primary mouse button hit.
     * raise/lower/paint... the terrain
     */
    public void doTerrainEditToolActivated() {

        if (terrainTool != null) {
            Vector3f point = getMarkerLocation();
            if (point != null) {
                terrainTool.actionPrimary(point, selectedTextureIndex, jmeRootNode, editorController.getCurrentDataObject());
            }
            
        }
    }

    /**
     * Alternate mouse button hit.
     */
    public void doTerrainEditToolAlternateActivated() {
        
        if (terrainTool != null) {
            Vector3f point = cameraController.getTerrainCollisionPoint();
            if (point != null) {
                terrainTool.actionSecondary(point, selectedTextureIndex, jmeRootNode, editorController.getCurrentDataObject());
            }
            
        }

    }

}
