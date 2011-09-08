/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.scene.controller.SceneToolController;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.control.Control;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
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
    private ViewPort overlayView;
    private Node onTopToolsNode;
    private Node nonSpatialMarkersNode;
    private Material lightMarkerMaterial;
    private Material audioMarkerMaterial;

    public SceneComposerToolController(Node toolsNode, AssetManager manager, JmeNode rootNode) {
        super(toolsNode, manager);
        this.rootNode = rootNode;
        nonSpatialMarkersNode = new Node("lightMarkersNode");
        toolsNode.attachChild(nonSpatialMarkersNode);
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
        overlayView.attachScene(onTopToolsNode);
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
        if (editTool != null) {
            editTool.updateToolsTransformation();
        }

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
        if (editTool != null) {
            return editTool.isOverrideCameraControl();
        } else {
            return false;
        }
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
        if (editTool != null) {
            editTool.hideMarker();
        }
        editTool = sceneEditTool;
        editTool.activate(manager, toolsNode, onTopToolsNode, selected, this);
    }

    public void selectedSpatialTransformed() {
        if (editTool != null) {
            SceneApplication.getApplication().enqueue(new Callable<Object>() {

                public Object call() throws Exception {
                    editTool.updateToolsTransformation();
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
        if (editTool != null) {
            editTool.setCamera(camera);
            editTool.actionPrimary(mouseLoc, pressed, rootNode, editorController.getCurrentDataObject());
        }
    }

    /**
     * Secondary button activated, send command to the tool
     * for appropriate action.
     */
    public void doEditToolActivatedSecondary(Vector2f mouseLoc, boolean pressed, Camera camera) {
        if (editTool != null) {
            editTool.setCamera(camera);
            editTool.actionSecondary(mouseLoc, pressed, rootNode, editorController.getCurrentDataObject());
        }
    }

    public void doEditToolMoved(Vector2f mouseLoc, Camera camera) {
        if (editTool != null) {
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
        if (editTool != null) {
            editTool.setCamera(camera);
            editTool.draggedSecondary(mouseLoc, pressed, rootNode, editorController.getCurrentDataObject());
        }
    }
    
    /**
     * Adds a marker for the light to the scene if it does not exist yet
     */
    public void addLightMarker(Light light) {
        if (!(light instanceof PointLight) && !(light instanceof SpotLight))
            return; // only handle point and spot lights
        
        Spatial s = nonSpatialMarkersNode.getChild(light.getName());
        if (s != null) {
            // update location maybe? Remove old and replace with new?
            return;
        }
        
        LightMarker lm = new LightMarker(light);
        nonSpatialMarkersNode.attachChild(lm);
    }
    
    public void addAudioMarker(AudioNode audio) {
        
        Spatial s = nonSpatialMarkersNode.getChild(audio.getName());
        if (s != null) {
            // update location maybe? Remove old and replace with new?
            return;
        }
        
        AudioMarker am = new AudioMarker(audio);
        nonSpatialMarkersNode.attachChild(am);
    }
    
    /**
     * Removes a light marker from the scene's tool node
     */
    public void removeLightMarker(Light light) {
        Spatial s = nonSpatialMarkersNode.getChild(light.getName());
        s.removeFromParent();
    }
    
    private Material getLightMarkerMaterial() {
        if (lightMarkerMaterial == null) {
            Material mat = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
            Texture tex = manager.loadTexture("com/jme3/gde/scenecomposer/lightbulb32.png");
            mat.setTexture("ColorMap", tex);
            mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
            lightMarkerMaterial = mat;
        }
        return lightMarkerMaterial;
    }
    
    private Material getAudioMarkerMaterial() {
        if (audioMarkerMaterial == null) {
            Material mat = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
            Texture tex = manager.loadTexture("com/jme3/gde/scenecomposer/audionode.gif");
            mat.setTexture("ColorMap", tex);
            mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
            audioMarkerMaterial = mat;
        }
        return audioMarkerMaterial;
    }

    protected void refreshNonSpatialMarkers() {
        nonSpatialMarkersNode.detachAllChildren();
        addMarkers(rootNode.getLookup().lookup(Node.class));
    }
    
    private void addMarkers(Node parent) {
        
        for (Light light : parent.getLocalLightList())
            addLightMarker(light);
        
        if (parent instanceof AudioNode) {
            addAudioMarker((AudioNode)parent);
        }
        
        for (Spatial s : parent.getChildren()) {
            if (s instanceof Node)
                addMarkers((Node)s);
            else {
                //TODO later if we include other types of non-spatial markers
            }
        }
    }
    
    /**
     * A marker on the screen that shows where a point light or
     * a spot light is. This marker is not part of the scene,
     * but is part of the tools node.
     */
    protected class LightMarker extends Geometry {
        private Light light;
        
        protected LightMarker() {}
    
        protected LightMarker(Light light) {
            this.light = light;
            Quad q = new Quad(0.5f, 0.5f);
            this.setMesh(q);
            this.setMaterial(getLightMarkerMaterial());
            this.addControl(new LightMarkerControl());
            this.setQueueBucket(Bucket.Transparent);
        }
        
        protected Light getLight() {
            return light;
        }
        
        @Override
        public void setLocalTranslation(Vector3f location) {
            super.setLocalTranslation(location);
            if (light instanceof PointLight)
                ((PointLight)light).setPosition(location);
            else if (light instanceof SpotLight)
                ((SpotLight)light).setPosition(location);
        }
        
        @Override
        public void setLocalTranslation(float x, float y, float z) {
            super.setLocalTranslation(x, y, z);
            if (light instanceof PointLight)
                ((PointLight)light).setPosition(new Vector3f(x,y,z));
            else if (light instanceof SpotLight)
                ((SpotLight)light).setPosition(new Vector3f(x,y,z));
        }
    }
    
    /**
     * Updates the marker's position whenever the light has moved.
     * It is also a BillboardControl, so this marker always faces
     * the camera
     */
    protected class LightMarkerControl extends BillboardControl {

        LightMarkerControl(){
            super();
        }
        
        @Override
        protected void controlUpdate(float f) {
            super.controlUpdate(f);
            LightMarker marker = (LightMarker) getSpatial();
            if (marker != null) {
                if (marker.getLight() instanceof PointLight) {
                    marker.setLocalTranslation(((PointLight)marker.getLight()).getPosition());
                } else if (marker.getLight() instanceof SpotLight) {
                    marker.setLocalTranslation(((SpotLight)marker.getLight()).getPosition());
                }
            }
        }

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp) {
            super.controlRender(rm, vp);
        }

        @Override
        public Control cloneForSpatial(Spatial sptl) {
            LightMarkerControl c = new LightMarkerControl();
            c.setSpatial(sptl);
            //TODO this isn't correct, none of BillboardControl is copied over
            return c;
        }
        
    }
    
    /**
     * A marker on the screen that shows where an audio node is. 
     * This marker is not part of the scene, but is part of the tools node.
     */
    protected class AudioMarker extends Geometry {
        private AudioNode audio;
        
        protected AudioMarker() {}
    
        protected AudioMarker(AudioNode audio) {
            this.audio = audio;
            Quad q = new Quad(0.5f, 0.5f);
            this.setMesh(q);
            this.setMaterial(getAudioMarkerMaterial());
            this.addControl(new AudioMarkerControl());
            this.setQueueBucket(Bucket.Transparent);
        }
        
        protected AudioNode getAudioNode() {
            return audio;
        }
        
        @Override
        public void setLocalTranslation(Vector3f location) {
            super.setLocalTranslation(location);
            audio.setLocalTranslation(location);
        }
        
        @Override
        public void setLocalTranslation(float x, float y, float z) {
            super.setLocalTranslation(x, y, z);
            audio.setLocalTranslation(x, y, z);
        }
    }
    
    /**
     * Updates the marker's position whenever the audio node has moved.
     * It is also a BillboardControl, so this marker always faces
     * the camera
     */
    protected class AudioMarkerControl extends BillboardControl {

        AudioMarkerControl(){
            super();
        }
        
        @Override
        protected void controlUpdate(float f) {
            super.controlUpdate(f);
            AudioMarker marker = (AudioMarker) getSpatial();
            if (marker != null) {
                marker.setLocalTranslation(marker.getAudioNode().getWorldTranslation());
            }
        }

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp) {
            super.controlRender(rm, vp);
        }

        @Override
        public Control cloneForSpatial(Spatial sptl) {
            AudioMarkerControl c = new AudioMarkerControl();
            c.setSpatial(sptl);
            //TODO this isn't correct, none of BillboardControl is copied over
            return c;
        }
        
    }
    
}
