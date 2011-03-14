/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.core.scene.controller;

import com.jme3.app.Application;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.util.DebugShapeFactory;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.debug.WireBox;
import java.util.concurrent.Callable;

/**
 * This class can be used or extended by other plugins to display
 * standard tools in the tools scene e.g. a cursor etc.
 * @author normenhansen
 */
public class SceneToolController implements AppState {

    protected Node toolsNode;
    protected boolean showSelection = false;
    protected boolean showGrid = false;
    protected Node cursor;
    protected Geometry grid;
    protected Spatial selected;
    protected Spatial selectionShape;
    protected AssetManager manager;

    public SceneToolController(AssetManager manager) {
        this.toolsNode = new Node("ToolsNode");
        initTools();
        SceneApplication.getApplication().getStateManager().attach(this);
    }

    public SceneToolController(Node toolsNode, AssetManager manager) {
        this.toolsNode = toolsNode;
        this.manager = manager;
        initTools();
        SceneApplication.getApplication().getStateManager().attach(this);
    }

    protected void initTools() {
        Material redMat = new Material(manager, "Common/MatDefs/Misc/WireColor.j3md");
        redMat.setColor("Color", ColorRGBA.Red);
        Material greenMat = new Material(manager, "Common/MatDefs/Misc/WireColor.j3md");
        greenMat.setColor("Color", ColorRGBA.Green);
        Material blueMat = new Material(manager, "Common/MatDefs/Misc/WireColor.j3md");
        blueMat.setColor("Color", ColorRGBA.Blue);
        Material grayMat = new Material(manager, "Common/MatDefs/Misc/WireColor.j3md");
        grayMat.setColor("Color", ColorRGBA.Gray);

        //cursor
        if (cursor == null) {
            cursor = new Node();
        }
        cursor.detachAllChildren();
        Geometry cursorArrowX = new Geometry("cursorArrowX", new Arrow(Vector3f.UNIT_X));
        Geometry cursorArrowY = new Geometry("cursorArrowY", new Arrow(Vector3f.UNIT_Y));
        Geometry cursorArrowZ = new Geometry("cursorArrowZ", new Arrow(Vector3f.UNIT_Z));
        cursorArrowX.setMaterial(redMat);
        cursorArrowY.setMaterial(greenMat);
        cursorArrowZ.setMaterial(blueMat);
        cursor.attachChild(cursorArrowX);
        cursor.attachChild(cursorArrowY);
        cursor.attachChild(cursorArrowZ);
        toolsNode.attachChild(cursor);

        //grid
        grid = new Geometry("grid", new Grid(20, 20, 1.0f));
        grid.setMaterial(grayMat);
        grid.setLocalTranslation(-10, 0, -10);
    }

    public void updateSelection(final Spatial spat) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                doUpdateSelection(spat);
                return null;
            }
        });
    }

    public void doUpdateSelection(Spatial spat) {
        if (showSelection && spat != null) {
            if (selected != spat) {
                if (selectionShape != null) {
                    detachSelectionShape();
                }
                attachSelectionShape(spat);
            } else {
                if (selectionShape == null) {
                    attachSelectionShape(spat);
                }
            }
        } else {
            if (selectionShape != null) {
                detachSelectionShape();
            }
        }
        selected = spat;
    }

    public void setCursorLocation(final Vector3f location) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                doSetCursorLocation(location);
                return null;
            }
        });
    }

    public void doSetCursorLocation(Vector3f location) {
        cursor.setLocalTranslation(location);
    }

    public void snapCursorToSelection() {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                doSnapCursorToSelection();
                return null;
            }
        });
    }

    public void doSnapCursorToSelection() {
        if (selected != null) {
            cursor.setLocalTranslation(selected.getWorldTranslation());
        }
    }

    protected void attachSelectionShape(Spatial spat) {
        if (selectionShape != null) {
            selectionShape.removeFromParent();
            selectionShape = null;
        }
        if (spat instanceof Geometry) {
            attachGeometrySelection((Geometry) spat);
        } else if (spat.getControl(PhysicsControl.class) != null) {
            attachPhysicsSelection(spat);
        } else {
            attachBoxSelection(spat);
        }
    }

    protected void attachGeometrySelection(Geometry geom) {
        Mesh mesh = geom.getMesh();
        if (mesh == null) {
            return;
        }
        Material mat = new Material(SceneApplication.getApplication().getAssetManager(), "Common/MatDefs/Misc/WireColor.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        Geometry selectionGeometry = new Geometry("selection_geometry_sceneviewer", mesh);
        selectionGeometry.setMaterial(mat);
        selectionGeometry.setLocalTransform(geom.getWorldTransform());
        toolsNode.attachChild(selectionGeometry);
        selectionShape = selectionGeometry;
    }

    protected void attachBoxSelection(Spatial geom) {
        Material mat = new Material(SceneApplication.getApplication().getAssetManager(), "Common/MatDefs/Misc/WireColor.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        BoundingVolume bound = geom.getWorldBound();
        if (bound instanceof BoundingBox) {
            BoundingBox bbox = (BoundingBox) bound;
            Vector3f extent = new Vector3f();
            bbox.getExtent(extent);
            WireBox wireBox=new WireBox();
            wireBox.fromBoundingBox(bbox);
            Geometry selectionGeometry = new Geometry("selection_geometry_sceneviewer", wireBox);
            selectionGeometry.setMaterial(mat);
            selectionGeometry.setLocalTransform(geom.getWorldTransform());
            toolsNode.attachChild(selectionGeometry);
            selectionShape = selectionGeometry;
        }
    }

    protected void attachPhysicsSelection(Spatial geom) {
        PhysicsCollisionObject control = geom.getControl(RigidBodyControl.class);
        if (control == null) {
            control = geom.getControl(VehicleControl.class);
        }
        if (control == null) {
            control = geom.getControl(GhostControl.class);
        }
        if (control == null) {
            control = geom.getControl(CharacterControl.class);
        }
        if (control == null) {
            return;
        }
        Material mat = new Material(SceneApplication.getApplication().getAssetManager(), "Common/MatDefs/Misc/WireColor.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        Spatial selectionGeometry = DebugShapeFactory.getDebugShape(control.getCollisionShape());
        if (selectionGeometry != null) {
            selectionGeometry.setMaterial(mat);
            selectionGeometry.setLocalTransform(geom.getWorldTransform());
            toolsNode.attachChild(selectionGeometry);
            selectionShape = selectionGeometry;
        }
    }

    protected void detachSelectionShape() {
        if (selectionShape != null) {
            selectionShape.removeFromParent();
            selectionShape = null;
        }
    }

    public void cleanup() {
        detachSelectionShape();
        cursor.removeFromParent();
        grid.removeFromParent();
        SceneApplication.getApplication().getStateManager().detach(this);
    }

    //TODO: multithreading!
    public Vector3f getCursorLocation() {
        return cursor.getLocalTranslation();
    }

    public void setShowSelection(final boolean showSelection) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                doSetShowSelection(showSelection);
                return null;
            }
        });
    }

    public void doSetShowSelection(boolean showSelection) {
        this.showSelection = showSelection;
        if (showSelection && selected != null && selectionShape == null) {
            attachSelectionShape(selected);
        } else if (!showSelection && selectionShape != null) {
            detachSelectionShape();
        }
    }

    public void setShowGrid(final boolean showGrid) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                doSetShowGrid(showGrid);
                return null;
            }
        });
    }

    public void doSetShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
        if (showGrid) {
            toolsNode.attachChild(grid);
        } else {
            toolsNode.detachChild(grid);
        }
    }

    /**
     * @return the toolsNode
     */
    public Node getToolsNode() {
        return toolsNode;
    }

    public void initialize(AppStateManager asm, Application aplctn) {
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isInitialized() {
        return true;
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setActive(boolean bln) {
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isActive() {
        return true;
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void stateAttached(AppStateManager asm) {
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void stateDetached(AppStateManager asm) {
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void update(float f) {
        if (selected == null || selectionShape == null) {
            return;
        }
        selectionShape.setLocalTranslation(selected.getWorldTranslation());
        selectionShape.setLocalRotation(selected.getWorldRotation());
    }

    public void render(RenderManager rm) {
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void postRender() {
//        throw new UnsupportedOperationException("Not supported yet.");
    }
}
