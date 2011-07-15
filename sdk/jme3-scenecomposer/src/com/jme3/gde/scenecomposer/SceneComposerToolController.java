/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer;

import com.jme3.asset.AssetManager;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.scene.controller.SceneToolController;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.concurrent.Callable;

/**
 *
 * @author Brent Owens
 */
public class SceneComposerToolController extends SceneToolController {

    private JmeNode rootNode;
    private SceneEditTool editTool;
    private SceneEditorController editorController;
    private ComposerCameraController cameraController;
    private Camera overlayCam;
    private ViewPort overlayView;
    private Node onTopToolsNode;
    
    public SceneComposerToolController(Node toolsNode, AssetManager manager, JmeNode rootNode) {
        super(toolsNode, manager);
        this.rootNode = rootNode;
    }

    public SceneComposerToolController(AssetManager manager) {
        super(manager);
    }

    public void setEditorController(SceneEditorController editorController) {
        this.editorController = editorController;
    }

    public void setCameraController(ComposerCameraController cameraController) {
        this.cameraController = cameraController;
        
        // a node in a viewport that will always render on top
        onTopToolsNode = new Node("OverlayNode");
        overlayView = SceneApplication.getApplication().getRenderManager().createMainView("Overlay", this.cameraController.getCamera());
        overlayView.setClearFlags(false, true, false);
        overlayView.attachScene( onTopToolsNode );
    }
    
    @Override
    public void cleanup() {
        super.cleanup();
        SceneApplication.getApplication().getRenderManager().removeMainView(overlayView);
        cameraController = null;
        editorController = null;
        onTopToolsNode.detachAllChildren();
    }
    
    @Override
    public void update(float tpf) {
        super.update(tpf);
        if (onTopToolsNode != null) {
            onTopToolsNode.updateLogicalState(tpf);
            onTopToolsNode.updateGeometricState();
        }
        if (editTool != null)
            editTool.updateToolsTransformation(selected);
        
    }
    
    @Override
    public void render(RenderManager rm) {
        super.render(rm);
    }
    
    public boolean isEditToolEnabled() {
        return editTool != null;
    }
    
    /**
     * If the current tool overrides camera zoom/pan controls
     */
    public boolean isOverrideCameraControl() {
        if (editTool != null)
            return editTool.isOverrideCameraControl();
        else
            return false;
    }
    
    /**
     * Scene composer edit tool activated. Pass in null to remove tools.
     * 
     * @param sceneEditButton pass in null to hide any existing tool markers
     */
    public void showEditTool(final SceneEditTool sceneEditTool) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {
            public Object call() throws Exception {
                doEnableEditTool(sceneEditTool);
                return null;
            }
        });
    }
    
    private void doEnableEditTool(SceneEditTool sceneEditTool) {
        if (editTool != null)
            editTool.hideMarker();
        editTool = sceneEditTool;
        editTool.activate(manager, toolsNode, onTopToolsNode, selected, this);
    }
    
    public void selectedSpatialTransformed() {
        if (editTool != null) {
            SceneApplication.getApplication().enqueue(new Callable<Object>() {
                public Object call() throws Exception {
                    editTool.updateToolsTransformation(selected);
                    return null;
                }
            });
        }
    }
    
    public void setSelected(Spatial selected) {
        this.selected = selected;
    }
    
    public void setNeedsSave(boolean needsSave) {
        editorController.setNeedsSave(needsSave);
    }
    
    /**
     * Primary button activated, send command to the tool
     * for appropriate action.
     */
    public void doEditToolActivatedPrimary(Vector2f mouseLoc, boolean pressed, Camera camera) {
        if (editTool != null){
            editTool.setCamera(camera);
            editTool.actionPrimary(mouseLoc, pressed, rootNode, editorController.getCurrentDataObject());
        }
    }
    
    /**
     * Secondary button activated, send command to the tool
     * for appropriate action.
     */
    public void doEditToolActivatedSecondary(Vector2f mouseLoc, boolean pressed, Camera camera) {
        if (editTool != null){
            editTool.setCamera(camera);
            editTool.actionSecondary(mouseLoc, pressed, rootNode, editorController.getCurrentDataObject());
        }
    }
    
    public void doEditToolMoved(Vector2f mouseLoc, Camera camera) {
        if (editTool != null){
            editTool.setCamera(camera);
            editTool.mouseMoved(mouseLoc);
        }
    }
    
    public void doEditToolDraggedPrimary(Vector2f mouseLoc, boolean pressed, Camera camera) {
        if (editTool != null) {
            editTool.setCamera(camera);
            editTool.draggedPrimary(mouseLoc, pressed, rootNode, editorController.getCurrentDataObject());
        }
    }
    
    public void doEditToolDraggedSecondary(Vector2f mouseLoc, boolean pressed, Camera camera) {
        if (editTool != null){
            editTool.setCamera(camera);
            editTool.draggedSecondary(mouseLoc, pressed, rootNode, editorController.getCurrentDataObject());
        }
    }
}
