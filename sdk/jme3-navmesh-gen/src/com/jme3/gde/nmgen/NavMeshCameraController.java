/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.nmgen;

import com.jme3.app.Application;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.scene.controller.AbstractCameraController;
import com.jme3.renderer.Camera;

/**
 *
 * @author sploreg
 */
public class NavMeshCameraController extends AbstractCameraController {

    private NavMeshToolController toolController;
    private NavMeshController editorController;
    private Application app;
    
    public NavMeshCameraController(Camera cam) {
        super(cam, SceneApplication.getApplication().getInputManager());
        app = SceneApplication.getApplication();
    }

    public void setEditorController(NavMeshController editorController) {
        this.editorController = editorController;
    }

    public void setToolController(NavMeshToolController toolController) {
        this.toolController = toolController;
    }
    
    @Override
    public boolean useCameraControls() {
        return true;
    }

    @Override
    protected void checkClick(int button, boolean pressed) {
        //TODO
    }
    
}
