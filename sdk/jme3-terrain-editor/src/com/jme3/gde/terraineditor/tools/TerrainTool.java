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
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.IntMap.Entry;
import org.openide.loaders.DataObject;

/**
 * Modifies the terrain in some way.
 * It has a primary and secondary action, activated from left and right mouse button respectively.
 * It will also attach tool geometries to the scene so the user can see where they are editing.
 * 
 * @author Brent Owens
 */
public abstract class TerrainTool {
    
    protected AssetManager manager;
    protected Geometry markerPrimary;
    protected Geometry markerSecondary;
    protected float radius;
    protected float weight;
    protected float maxToolSize = 20; // override in sub classes
    
    // the key to load the tool hint text from the resource bundle
    protected String toolHintTextKey = "TerrainEditorTopComponent.toolHint.default";
    
    /**
     * The tool was selected, start showing the marker.
     * @param manager
     * @param parent node that the marker will attach to
     */
    public void activate(AssetManager manager, Node parent) {
        this.manager = manager;
        addMarkerPrimary(parent);
    }
    
    /**
     * The primary action for the tool gets activated
     */
    public abstract void actionPrimary(Vector3f point, int textureIndex, AbstractSceneExplorerNode rootNode, DataObject dataObject);
    
    /**
     * The secondary action for the tool gets activated
     */
    public abstract void actionSecondary(Vector3f point, int textureIndex, AbstractSceneExplorerNode rootNode, DataObject dataObject);
    
    /**
     * Location of the primary editor marker
     */
    public Vector3f getMarkerPrimaryLocation() {
        if (markerPrimary != null)
            return markerPrimary.getLocalTranslation();
        else
            return null;
    }
    
    /**
     * Move the marker to a new location, usually follows the mouse
     * @param newLoc 
     */
    public void markerMoved(Vector3f newLoc) {
        if (markerPrimary != null)
            markerPrimary.setLocalTranslation(newLoc);
    }
    
    /**
     * The radius of the tool has changed, so update the marker
     * @param radius percentage of the max radius
     */
    public void radiusChanged(float radius) {
        this.radius = maxToolSize*radius;
        
        if (markerPrimary != null) {
            for (Entry e: markerPrimary.getMesh().getBuffers())
                ((VertexBuffer)e.getValue()).resetObject();
            ((Sphere)markerPrimary.getMesh()).updateGeometry(8, 8, this.radius);
        }
    }
    
    /**
     * The weight of the tool has changed. Optionally change
     * the marker look.
     * @param weight percent
     */
    public void weightChanged(float weight) {
        this.weight = weight;
    }
    
    /**
     * Create the primary marker mesh, follows the mouse.
     * @param parent it will attach to
     */
    public void addMarkerPrimary(Node parent) {
        if (markerPrimary == null) {
            markerPrimary = new Geometry("edit marker primary");
            Mesh m = new Sphere(8, 8, radius);
            markerPrimary.setMesh(m);
            Material mat = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.getAdditionalRenderState().setWireframe(true);
            markerPrimary.setMaterial(mat);
            markerPrimary.setLocalTranslation(0,0,0);
            mat.setColor("Color", ColorRGBA.LightGray);
        }
        parent.attachChild(markerPrimary);
    }
    
    /**
     * Create the secondary marker mesh, placed
     * with the right mouse button.
     * @param parent it will attach to
     */
    public void addMarkerSecondary(Node parent) {
        if (markerSecondary == null) {
            markerSecondary = new Geometry("edit marker secondary");
            Mesh m2 = new Sphere(8, 8, 0.5f);
            markerSecondary.setMesh(m2);
            Material mat2 = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat2.getAdditionalRenderState().setWireframe(false);
            markerSecondary.setMaterial(mat2);
            markerSecondary.setLocalTranslation(0,0,0);
            mat2.setColor("Color", ColorRGBA.Red);
        }
        parent.attachChild(markerSecondary);
    }
    
    /**
     * Remove the markers from the scene.
     */
    public void hideMarkers() {
        if (markerPrimary != null)
            markerPrimary.removeFromParent();
        if (markerSecondary != null)
            markerSecondary.removeFromParent();
    }

    public String getToolHintTextKey() {
        return toolHintTextKey;
    }

    public void setToolHintTextKey(String toolHintTextKey) {
        this.toolHintTextKey = toolHintTextKey;
    }

    
}
