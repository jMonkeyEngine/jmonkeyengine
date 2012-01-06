/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.nmgen;

import com.jme3.asset.AssetManager;
import com.jme3.gde.core.scene.controller.SceneToolController;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.gde.core.sceneexplorer.nodes.JmeSpatial;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;

/**
 *
 * @author sploreg
 */
public class NavMeshToolController extends SceneToolController {
    
    private NavMeshController editorController;
    private NavMeshCameraController cameraController;
    private JmeSpatial jmeRootNode;
    private Geometry navGeom;
    private Material navMaterial;
    
    public NavMeshToolController(Node toolsNode, AssetManager manager, JmeNode rootNode) {
        super(toolsNode, manager);
        this.jmeRootNode = rootNode;
    }

    public void setEditorController(NavMeshController editorController) {
        this.editorController = editorController;
    }

    public void setCameraController(NavMeshCameraController cameraController) {
        this.cameraController = cameraController;
    }

    private Material getNavMaterial() {
        if (navMaterial != null)
            return navMaterial;
        navMaterial = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
        navMaterial.setColor("Color", ColorRGBA.Green);
        navMaterial.getAdditionalRenderState().setWireframe(true);
        return navMaterial;
    }
    
    /**
     * Render the new nav mesh
     */
    protected void attachNavMesh(Mesh navMesh) {
        if (navMesh == null)
            return;
        if (navGeom == null)
            navGeom = new Geometry("NavMesh");
        navGeom.setMesh(navMesh);
        navGeom.setMaterial(getNavMaterial());
        toolsNode.attachChild(navGeom);
    }
}
