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

import com.jme3.asset.AssetManager;
import com.jme3.gde.core.sceneexplorer.nodes.AbstractSceneExplorerNode;
import com.jme3.gde.terraineditor.ExtraToolParams;
import com.jme3.input.KeyInput;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;
import org.openide.loaders.DataObject;

/**
 * Generates a slope between two control points.
 * @author Shirkit
 */
public class SlopeTerrainTool extends TerrainTool {

    private Vector3f point1, point2;
    private Geometry markerThird;
    private Node parent;
    private SlopeExtraToolParams toolParams;

    public SlopeTerrainTool() {
        toolHintTextKey = "TerrainEditorTopComponent.toolHint.shirkit";
    }

    @Override
    public void activate(AssetManager manager, Node parent) {
        super.activate(manager, parent);
        addMarkerSecondary(parent);
        addMarkerThird(parent);
        this.parent = parent;
    }

    @Override
    public void hideMarkers() {
        super.hideMarkers();
        if (markerThird != null) {
            markerThird.removeFromParent();
        }
    }
    
    private void addMarkerThird(Node parent) {
        if (markerThird == null) {
            markerThird = new Geometry("edit marker secondary");
            Mesh m2 = new Sphere(8, 8, 0.5f);
            markerThird.setMesh(m2);
            Material mat2 = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat2.getAdditionalRenderState().setWireframe(false);
            markerThird.setMaterial(mat2);
            markerThird.setLocalTranslation(0, 0, 0);
            mat2.setColor("Color", ColorRGBA.Blue);
        }
        parent.attachChild(markerThird);
    }

    @Override
    public void actionPrimary(Vector3f point, int textureIndex, AbstractSceneExplorerNode rootNode, DataObject dataObject) {
        if (point1 != null && point2 != null) {
            SlopeTerrainToolAction action = new SlopeTerrainToolAction(point, point1, point2, radius, weight, toolParams.precision);
            action.actionPerformed(rootNode, dataObject);
        }
    }
    private boolean leftCtrl = false;

    @Override
    public void keyPressed(KeyInputEvent kie) {
        if (kie.getKeyCode() == KeyInput.KEY_LCONTROL) {
            leftCtrl = kie.isPressed();
        }
        switch (kie.getKeyCode()) {
            case KeyInput.KEY_LCONTROL:
                leftCtrl = kie.isPressed();
                break;
            case KeyInput.KEY_C:
                point1 = null;
                point2 = null;
                markerSecondary.removeFromParent();
                markerThird.removeFromParent();
                break;
        }
    }

    @Override
    public void actionSecondary(Vector3f point, int textureIndex, AbstractSceneExplorerNode rootNode, DataObject dataObject) {
        if (leftCtrl) {
            point2 = point;
            if (markerThird.getParent() == null) {
                parent.attachChild(markerThird);
            }
            markerThird.setLocalTranslation(point);
        } else {
            point1 = point;
            if (markerSecondary.getParent() == null) {
                parent.attachChild(markerSecondary);
            }
            markerSecondary.setLocalTranslation(point);
        }
    }

    @Override
    public void setExtraParams(ExtraToolParams params) {
        if (params instanceof SlopeExtraToolParams)
            this.toolParams = (SlopeExtraToolParams) params;
    }
    
    
}
