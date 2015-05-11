/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.gde.core.sceneexplorer.nodes.JmeSpatial;
import com.jme3.gde.core.undoredo.AbstractUndoableSceneEdit;
import com.jme3.gde.core.undoredo.SceneUndoRedoManager;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.shape.Quad;
import java.util.Iterator;
import java.util.concurrent.Callable;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;

/**
 *
 * @author Brent Owens
 */
public abstract class SceneEditTool {

    protected static Vector3f ARROW_X = new Vector3f(1, 0, 0);
    protected static Vector3f ARROW_Y = new Vector3f(0, 1, 0);
    protected static Vector3f ARROW_Z = new Vector3f(0, 0, 1);
    protected static Vector3f QUAD_XY = new Vector3f(1, 1, 0);
    protected static Vector3f QUAD_XZ = new Vector3f(1, 0, 1);
    protected static Vector3f QUAD_YZ = new Vector3f(0, 1, 1);
    protected SceneComposerToolController toolController;
    protected AssetManager manager;
    protected Camera camera;
    private boolean overrideCameraControl = false; // if true, you cannot pan/zoom unless you hold SHIFT
    // the key to load the tool hint text from the resource bundle
    protected String toolHintTextKey = "SceneComposerTopComponent.toolHint.default"; // not used yet
    protected Node toolNode;
    protected Node onTopToolNode;
    protected Node axisMarker;
    protected Material redMat, blueMat, greenMat, yellowMat, cyanMat, magentaMat, orangeMat;
    protected Geometry quadXY, quadXZ, quadYZ;
    protected SceneComposerToolController.TransformationType transformType;

    protected enum AxisMarkerPickType {

        axisOnly, planeOnly, axisAndPlane
    };
    protected AxisMarkerPickType axisPickType;

    /**
     * The tool was selected, start showing the marker.
     * @param manager
     * @param toolNode: parent node that the marker will attach to
     */
    public void activate(AssetManager manager, Node toolNode, Node onTopToolNode, Spatial selectedSpatial, SceneComposerToolController toolController) {
        this.manager = manager;
        this.toolController = toolController;
        this.setTransformType(toolController.getTransformationType());
        //this.selectedSpatial = selectedSpatial;
        addMarker(toolNode, onTopToolNode);
    }

    protected void addMarker(Node toolNode, Node onTopToolNode) {
        this.toolNode = toolNode;
        this.onTopToolNode = onTopToolNode;

        if (axisMarker == null) {
            axisMarker = createAxisMarker();
        }
        axisMarker.removeFromParent();
        this.onTopToolNode.attachChild(axisMarker);
        setDefaultAxisMarkerColors();


        if (toolController.getSelectionShape() != null) {
            axisMarker.setLocalTranslation(toolController.getSelectedSpatial().getWorldTranslation());
        }

    }

    /**
     * Remove the marker from it's parent (the tools node)
     */
    public void hideMarker() {
        if (axisMarker != null) {
            axisMarker.removeFromParent();
        }
    }

    public boolean isOverrideCameraControl() {
        return overrideCameraControl;
    }

    public void setOverrideCameraControl(boolean overrideCameraControl) {
        this.overrideCameraControl = overrideCameraControl;
    }

    /**
     * Called when the selected spatial has been modified
     * outside of the tool.
     * TODO: why? just move the tool where the object is each frame?
     */
    public void updateToolsTransformation() {

        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                doUpdateToolsTransformation();
                return null;
            }
        });
    }

    public void doUpdateToolsTransformation() {
        if (toolController.getSelectedSpatial() != null) {
            axisMarker.setLocalTranslation(toolController.getSelectedSpatial().getWorldTranslation());
            switch (transformType) {
                case local:
                    axisMarker.setLocalRotation(toolController.getSelectedSpatial().getLocalRotation());
                    break;
                case global:
                    axisMarker.setLocalRotation(Quaternion.IDENTITY);
                    break;
                case camera:
                    if(camera != null){
                        axisMarker.setLocalRotation(camera.getRotation());
                    }
                    break;
            }
            setAxisMarkerScale(toolController.getSelectedSpatial());
        } else {
            axisMarker.setLocalTranslation(Vector3f.ZERO);
            axisMarker.setLocalRotation(Quaternion.IDENTITY);
        }
    }

    /**
     * Adjust the scale of the marker so it is relative to the size of the
     * selected spatial. It will have a minimum scale of 2.
     */
    private void setAxisMarkerScale(Spatial selected) {
        if (selected != null) {
            if (selected.getWorldBound() instanceof BoundingBox) {
                BoundingBox bbox = (BoundingBox) selected.getWorldBound();
                float smallest = Math.min(Math.min(bbox.getXExtent(), bbox.getYExtent()), bbox.getZExtent());
                float scale = Math.max(1, smallest / 2f);
                axisMarker.setLocalScale(new Vector3f(scale, scale, scale));
            }
        } else {
            axisMarker.setLocalScale(new Vector3f(2, 2, 2));
        }
    }

    /**
     * The primary action for the tool gets activated
     */
    public abstract void actionPrimary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject dataObject);

    /**
     * The secondary action for the tool gets activated
     */
    public abstract void actionSecondary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject dataObject);

    /**
     * Called when the mouse is moved but not dragged (ie no buttons are pressed)
     */
    public abstract void mouseMoved(Vector2f screenCoord, JmeNode rootNode, DataObject dataObject, JmeSpatial selectedSpatial);

    /**
     * Called when the mouse is moved while the primary button is down
     */
    public abstract void draggedPrimary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject);

    /**
     * Called when the mouse is moved while the secondary button is down
     */
    public abstract void draggedSecondary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject);

    public void keyPressed(KeyInputEvent kie) {
    }

    /**
     * Call when an action is performed that requires the scene to be saved
     * and an undo can be performed
     * @param undoer your implementation, probably with a begin and end state for undoing
     */
    protected void actionPerformed(AbstractUndoableSceneEdit undoer) {
        Lookup.getDefault().lookup(SceneUndoRedoManager.class).addEdit(this, undoer);
        toolController.setNeedsSave(true);
    }

    /**
     * Given the mouse coordinates, pick the geometry that is closest to the camera.
     * @param jmeRootNode to pick from
     * @return the selected spatial, or null if nothing
     */
    public static Spatial pickWorldSpatial(Camera cam, Vector2f mouseLoc, JmeNode jmeRootNode) {
        Node rootNode = jmeRootNode.getLookup().lookup(Node.class);
        CollisionResult cr = pick(cam, mouseLoc, rootNode);
        if (cr != null) {
            return cr.getGeometry();
        } else {
            return null;
        }
    }

    /**
     * Given the mouse coordinate, pick the world location where the mouse intersects
     * a geometry.
     * @param jmeRootNode to pick from
     * @return the location of the pick, or null if nothing collided with the mouse
     */
    public static Vector3f pickWorldLocation(Camera cam, Vector2f mouseLoc, JmeNode jmeRootNode) {
        Node rootNode = jmeRootNode.getLookup().lookup(Node.class);
        return pickWorldLocation(cam, mouseLoc, rootNode, null);
    }

    /**
     * Pick anything except the excluded spatial
     * @param excludeSpat to not pick
     */
    public static Vector3f pickWorldLocation(Camera cam, Vector2f mouseLoc, JmeNode jmeRootNode, JmeSpatial excludeSpat) {
        Node rootNode = jmeRootNode.getLookup().lookup(Node.class);
        Spatial exclude = excludeSpat.getLookup().lookup(Spatial.class);
        return pickWorldLocation(cam, mouseLoc, rootNode, exclude);
    }

    public static Vector3f pickWorldLocation(Camera cam, Vector2f mouseLoc, Node rootNode, Spatial exclude) {
        CollisionResult cr = doPick(cam, mouseLoc, rootNode, exclude);
        if (cr != null) {
            return cr.getContactPoint();
        } else {
            return null;
        }
    }

    private static CollisionResult doPick(Camera cam, Vector2f mouseLoc, Node node, Spatial exclude) {
        CollisionResults results = new CollisionResults();
        Ray ray = new Ray();
        Vector3f pos = cam.getWorldCoordinates(mouseLoc, 0).clone();
        Vector3f dir = cam.getWorldCoordinates(mouseLoc, 0.3f).clone();
        dir.subtractLocal(pos).normalizeLocal();
        ray.setOrigin(pos);
        ray.setDirection(dir);
        node.collideWith(ray, results);
        CollisionResult result = null;
        if (exclude == null) {
            result = results.getClosestCollision();
        } else {
            Iterator<CollisionResult> it = results.iterator();
            while (it.hasNext()) {
                CollisionResult cr = it.next();
                if (isExcluded(cr.getGeometry(), exclude)) {
                    continue;
                } else {
                    return cr;
                }
            }

        }
        return result;
    }

    /**
     * Is the selected spatial the one we want to exclude from the picking?
     * Recursively looks up the parents to find out.
     */
    private static boolean isExcluded(Spatial s, Spatial exclude) {
        if (s.equals(exclude)) {
            return true;
        }

        if (s.getParent() != null) {
            return isExcluded(s.getParent(), exclude);
        }
        return false;
    }

    /**
     * Pick a part of the axis marker. The result is a Vector3f that represents
     * what part of the axis was selected.
     * For example if  (1,0,0) is returned, then the X-axis pole was selected.
     * If (0,1,1) is returned, then the Y-Z plane was selected.
     *
     * @return null if it did not intersect the marker
     */
    protected Vector3f pickAxisMarker(Camera cam, Vector2f mouseLoc, AxisMarkerPickType pickType) {
        if (axisMarker == null) {
            return null;
        }

        CollisionResult cr = pick(cam, mouseLoc, axisMarker);
        if (cr == null || cr.getGeometry() == null) {
            return null;
        }

        if (pickType == AxisMarkerPickType.planeOnly) {
            if ("quadXY".equals(cr.getGeometry().getName())) {
                return QUAD_XY;
            } else if ("quadXZ".equals(cr.getGeometry().getName())) {
                return QUAD_XZ;
            } else if ("quadYZ".equals(cr.getGeometry().getName())) {
                return QUAD_YZ;
            }
        } else if (pickType == AxisMarkerPickType.axisOnly) {
            if ("arrowX".equals(cr.getGeometry().getName())) {
                return ARROW_X;
            } else if ("arrowY".equals(cr.getGeometry().getName())) {
                return ARROW_Y;
            } else if ("arrowZ".equals(cr.getGeometry().getName())) {
                return ARROW_Z;
            }
        } else if (pickType == AxisMarkerPickType.axisAndPlane) {
            if ("arrowX".equals(cr.getGeometry().getName())) {
                return ARROW_X;
            } else if ("arrowY".equals(cr.getGeometry().getName())) {
                return ARROW_Y;
            } else if ("arrowZ".equals(cr.getGeometry().getName())) {
                return ARROW_Z;
            } else if ("quadXY".equals(cr.getGeometry().getName())) {
                return QUAD_XY;
            } else if ("quadXZ".equals(cr.getGeometry().getName())) {
                return QUAD_XZ;
            } else if ("quadYZ".equals(cr.getGeometry().getName())) {
                return QUAD_YZ;
            }
        }
        return null;
    }

    private static CollisionResult pick(Camera cam, Vector2f mouseLoc, Node node) {
        CollisionResults results = new CollisionResults();
        Ray ray = new Ray();
        Vector3f pos = cam.getWorldCoordinates(mouseLoc, 0).clone();
        Vector3f dir = cam.getWorldCoordinates(mouseLoc, 0.125f).clone();
        dir.subtractLocal(pos).normalizeLocal();
        ray.setOrigin(pos);
        ray.setDirection(dir);
        node.collideWith(ray, results);
        CollisionResult result = results.getClosestCollision();
        return result;
    }

    /**
     * Show what axis or plane the mouse is currently over and will affect.
     * @param axisMarkerPickType
     */
    protected void highlightAxisMarker(Camera camera, Vector2f screenCoord, AxisMarkerPickType axisMarkerPickType) {
        highlightAxisMarker(camera, screenCoord, axisMarkerPickType, false);
    }

    /**
     * Show what axis or plane the mouse is currently over and will affect.
     * @param axisMarkerPickType
     * @param colorAll highlight all parts of the marker when only one is selected
     */
    protected void highlightAxisMarker(Camera camera, Vector2f screenCoord, AxisMarkerPickType axisMarkerPickType, boolean colorAll) {
        setDefaultAxisMarkerColors();
        Vector3f picked = pickAxisMarker(camera, screenCoord, axisMarkerPickType);
        if (picked == null) {
            return;
        }

        if (picked == ARROW_X) {
            axisMarker.getChild("arrowX").setMaterial(orangeMat);
        } else if (picked == ARROW_Y) {
            axisMarker.getChild("arrowY").setMaterial(orangeMat);
        } else if (picked == ARROW_Z) {
            axisMarker.getChild("arrowZ").setMaterial(orangeMat);
        } else {

            if (picked == QUAD_XY || colorAll) {
                axisMarker.getChild("quadXY").setMaterial(orangeMat);
            }
            if (picked == QUAD_XZ || colorAll) {
                axisMarker.getChild("quadXZ").setMaterial(orangeMat);
            }
            if (picked == QUAD_YZ || colorAll) {
                axisMarker.getChild("quadYZ").setMaterial(orangeMat);
            }
        }
    }

    /**
     * Create the axis marker that is selectable
     */
    protected Node createAxisMarker() {
        float size = 2;
        float arrowSize = size;
        float planeSize = size * 0.7f;

        Quaternion YAW090 = new Quaternion().fromAngleAxis(-FastMath.PI / 2, new Vector3f(0, 1, 0));
        Quaternion PITCH090 = new Quaternion().fromAngleAxis(FastMath.PI / 2, new Vector3f(1, 0, 0));

        redMat = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
        redMat.getAdditionalRenderState().setWireframe(true);
        redMat.setColor("Color", ColorRGBA.Red);
        //redMat.getAdditionalRenderState().setDepthTest(false);
        greenMat = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
        greenMat.getAdditionalRenderState().setWireframe(true);
        greenMat.setColor("Color", ColorRGBA.Green);
        //greenMat.getAdditionalRenderState().setDepthTest(false);
        blueMat = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
        blueMat.getAdditionalRenderState().setWireframe(true);
        blueMat.setColor("Color", ColorRGBA.Blue);
        //blueMat.getAdditionalRenderState().setDepthTest(false);
        yellowMat = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
        yellowMat.getAdditionalRenderState().setWireframe(false);
        yellowMat.setColor("Color", new ColorRGBA(1f, 1f, 0f, 0.25f));
        yellowMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        yellowMat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        //yellowMat.getAdditionalRenderState().setDepthTest(false);
        cyanMat = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
        cyanMat.getAdditionalRenderState().setWireframe(false);
        cyanMat.setColor("Color", new ColorRGBA(0f, 1f, 1f, 0.25f));
        cyanMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        cyanMat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        //cyanMat.getAdditionalRenderState().setDepthTest(false);
        magentaMat = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
        magentaMat.getAdditionalRenderState().setWireframe(false);
        magentaMat.setColor("Color", new ColorRGBA(1f, 0f, 1f, 0.25f));
        magentaMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        magentaMat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        //magentaMat.getAdditionalRenderState().setDepthTest(false);

        orangeMat = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
        orangeMat.getAdditionalRenderState().setWireframe(false);
        orangeMat.setColor("Color", new ColorRGBA(251f / 255f, 130f / 255f, 0f, 0.4f));
        orangeMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        orangeMat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);

        Node axis = new Node();

        // create arrows
        Geometry arrowX = new Geometry("arrowX", new Arrow(new Vector3f(arrowSize, 0, 0)));
        Geometry arrowY = new Geometry("arrowY", new Arrow(new Vector3f(0, arrowSize, 0)));
        Geometry arrowZ = new Geometry("arrowZ", new Arrow(new Vector3f(0, 0, arrowSize)));
        axis.attachChild(arrowX);
        axis.attachChild(arrowY);
        axis.attachChild(arrowZ);

        // create planes
        quadXY = new Geometry("quadXY", new Quad(planeSize, planeSize));
        quadXZ = new Geometry("quadXZ", new Quad(planeSize, planeSize));
        quadXZ.setLocalRotation(PITCH090);
        quadYZ = new Geometry("quadYZ", new Quad(planeSize, planeSize));
        quadYZ.setLocalRotation(YAW090);
//        axis.attachChild(quadXY);
//        axis.attachChild(quadXZ);
//        axis.attachChild(quadYZ);

        axis.setModelBound(new BoundingBox());
        axis.updateModelBound();
        return axis;
    }

    protected void displayPlanes() {
        axisMarker.attachChild(quadXY);
        axisMarker.attachChild(quadXZ);
        axisMarker.attachChild(quadYZ);
    }

    protected void hidePlanes() {
        quadXY.removeFromParent();
        quadXZ.removeFromParent();
        quadYZ.removeFromParent();

    }

    protected void setDefaultAxisMarkerColors() {
        axisMarker.getChild("arrowX").setMaterial(redMat);
        axisMarker.getChild("arrowY").setMaterial(greenMat);
        axisMarker.getChild("arrowZ").setMaterial(blueMat);
        quadXY.setMaterial(yellowMat);
        quadXZ.setMaterial(magentaMat);
        quadYZ.setMaterial(cyanMat);
    }

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public SceneComposerToolController.TransformationType getTransformType() {
        return transformType;
    }

    public void setTransformType(SceneComposerToolController.TransformationType transformType) {
        this.transformType = transformType;
    }
}
