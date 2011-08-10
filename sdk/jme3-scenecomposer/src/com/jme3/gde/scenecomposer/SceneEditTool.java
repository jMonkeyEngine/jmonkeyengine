/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.util.DebugShapeFactory;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.gde.core.undoredo.AbstractUndoableSceneEdit;
import com.jme3.gde.core.undoredo.SceneUndoRedoManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.debug.WireBox;
import com.jme3.scene.shape.Quad;
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
     */
    public void updateToolsTransformation() {

        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                if (toolController.getSelectedSpatial() != null) {
                    axisMarker.setLocalTranslation(toolController.getSelectedSpatial().getWorldTranslation());
                    axisMarker.setLocalRotation(toolController.getSelectedSpatial().getWorldRotation());
                } else {
                    axisMarker.setLocalTranslation(Vector3f.ZERO);
                    axisMarker.setLocalRotation(Quaternion.IDENTITY);
                }
                return null;
            }
        });
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
    public abstract void mouseMoved(Vector2f screenCoord);

    /**
     * Called when the mouse is moved while the primary button is down
     */
    public abstract void draggedPrimary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject);

    /**
     * Called when the mouse is moved while the secondary button is down
     */
    public abstract void draggedSecondary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject);

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
    protected Spatial pickWorldSpatial(Camera cam, Vector2f mouseLoc, JmeNode jmeRootNode) {
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
    protected Vector3f pickWorldLocation(Camera cam, Vector2f mouseLoc, JmeNode jmeRootNode) {
        Node rootNode = jmeRootNode.getLookup().lookup(Node.class);
        return pickWorldLocation(cam, mouseLoc, rootNode);
    }

    protected Vector3f pickWorldLocation(Camera cam, Vector2f mouseLoc, Node rootNode) {
        CollisionResult cr = pick(cam, mouseLoc, rootNode);
        if (cr != null) {
            return cr.getContactPoint();
        } else {
            return null;
        }
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

    private CollisionResult pick(Camera cam, Vector2f mouseLoc, Node node) {
        CollisionResults results = new CollisionResults();
        Ray ray = new Ray();
        Vector3f pos = cam.getWorldCoordinates(mouseLoc, 0).clone();
        Vector3f dir = cam.getWorldCoordinates(mouseLoc, 0.1f).clone();
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
        setDefaultAxisMarkerColors();
        Vector3f picked = pickAxisMarker(camera, screenCoord, axisPickType);
        if (picked == null) {
            return;
        }

        if (picked == ARROW_X) {
            axisMarker.getChild("arrowX").setMaterial(orangeMat);
        } else if (picked == ARROW_Y) {
            axisMarker.getChild("arrowY").setMaterial(orangeMat);
        } else if (picked == ARROW_Z) {
            axisMarker.getChild("arrowZ").setMaterial(orangeMat);
        } else if (picked == QUAD_XY) {
            axisMarker.getChild("quadXY").setMaterial(orangeMat);
        } else if (picked == QUAD_XZ) {
            axisMarker.getChild("quadXZ").setMaterial(orangeMat);
        } else if (picked == QUAD_YZ) {
            axisMarker.getChild("quadYZ").setMaterial(orangeMat);
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
        axisMarker.getChild("arrowY").setMaterial(blueMat);
        axisMarker.getChild("arrowZ").setMaterial(greenMat);
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
}
