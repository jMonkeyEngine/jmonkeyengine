/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package com.jme3.gde.scenecomposer.tools;

import com.jme3.gde.core.sceneexplorer.SceneExplorerTopComponent;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.gde.core.sceneexplorer.nodes.JmeSpatial;
import com.jme3.gde.core.sceneviewer.SceneViewerTopComponent;
import com.jme3.gde.core.undoredo.AbstractUndoableSceneEdit;
import com.jme3.gde.scenecomposer.SceneEditTool;
import com.jme3.input.KeyInput;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.terrain.Terrain;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;

/**
 * This duplicates the Blender manipulate tool.
 * It supports quick access to Grab, Rotate, and Scale operations
 * by typing one of the following keys: 'g', 'r', or 's'
 * Those keys can be followed by an axis key to specify what axis
 * to perform the transformation: x, y, z
 * Then, after the operation and axis are selected, you can type in a
 * number and then hit 'enter' to complete the transformation.
 * 
 * Ctrl+Shift+D will duplicate an object
 * X will delete an object
 * 
 * ITEMS TO FINISH:
 * 1) fixed scale and rotation values by holding Ctrl and dragging mouse
 * BUGS:
 * 1) window always needs focus from primary click when it should focus from secondary and middle mouse
 * 
 * @author Brent Owens
 */
public class SelectTool extends SceneEditTool {

    private enum State {

        translate, rotate, scale
    };
    private State currentState = null;
    private Vector3f currentAxis = Vector3f.UNIT_XYZ;
    private StringBuilder numberBuilder = new StringBuilder(); // gets appended with numbers
    private Quaternion startRot;
    private Vector3f startTrans;
    private Vector3f startScale;
    private boolean wasDraggingL = false;
    private boolean wasDraggingR = false;
    private boolean wasDownR = false;
    private boolean ctrlDown = false;
    private boolean shiftDown = false;
    private boolean altDown = false;
    private MoveManager.MoveUndo moving;
    private ScaleUndo scaling;
    private RotateUndo rotating;
    private Vector2f startMouseCoord; // for scaling and rotation
    private Vector2f startSelectedCoord; // for scaling and rotation
    private float lastRotAngle; // used for rotation

    /**
     * This is stateful:
     * First it checks for a command (rotate, translate, delete, etc..)
     * Then it checks for an axis (x,y,z)
     * Then it checks for a number (user typed a number
     * Then, finally, it checks if Enter was hit.
     * 
     * If either of the commands was actioned, the preceeding states/axis/amount
     * will be reset. For example if the user types: G Y 2 R
     * Then it will:
     * 1) Set state as 'Translate' for the G (grab)
     * 2) Set the axis as 'Y'; it will translate along the Y axis
     * 3) Distance will be 2, when the 2 key is hit
     * 4) Distance, Axis, and state are then reset because a new state was set: Rotate
     * it won't actually translate because 'Enter' was not hit and 'R' reset the state.
     * 
     */
    @Override
    public void keyPressed(KeyInputEvent kie) {

        checkModificatorKeys(kie); // alt,shift,ctrl
        Spatial selected = toolController.getSelectedSpatial();

        if (selected == null) {
            return; // only do anything if a spatial is selected
        }
        // key released
        if (kie.isReleased()) {
            boolean commandUsed = checkCommandKey(kie);
            boolean stateChange = checkStateKey(kie);
            boolean axisChange = checkAxisKey(kie);
            boolean numberChange = checkNumberKey(kie);
            boolean enterHit = checkEnterHit(kie);
            boolean escHit = checkEscHit(kie);

            if (commandUsed) {
                return; // commands take priority
            }
            if (stateChange) {
                currentAxis = Vector3f.UNIT_XYZ;
                numberBuilder = new StringBuilder();
                recordInitialState(selected);
            } else if (axisChange) {
            } else if (numberChange) {
            } else if (enterHit) {
                if (currentState != null && numberBuilder.length() > 0) {
                    applyKeyedChangeState(selected);
                    clearState(false);
                }
            }


            // -----------------------
            // reset conditions below:

            if (escHit) {
                if (moving != null) {
                    moving.sceneUndo();
                }

                moving = null;
                clearState();
            }

            if (!stateChange && !axisChange && !numberChange && !enterHit && !escHit) {
                // nothing valid was hit, reset the state
                //clearState(); // this will be 
            }
        }
    }

    /**
     * Abort any manipulations
     */
    private void clearState() {
        clearState(true);
    }

    private void clearState(boolean resetSelected) {
        Spatial selected = toolController.getSelectedSpatial();
        if (resetSelected && selected != null) {
            // reset the transforms
            if (startRot != null) {
                selected.setLocalRotation(startRot);
            }
            if (startTrans != null) {
                selected.setLocalTranslation(startTrans);
            }
            if (startScale != null) {
                selected.setLocalScale(startScale);
            }
        }
        currentState = null;
        currentAxis = Vector3f.UNIT_XYZ;
        numberBuilder = new StringBuilder();
        startRot = null;
        startTrans = null;
        startScale = null;
        startMouseCoord = null;
        startSelectedCoord = null;
        lastRotAngle = 0;
    }

    private void recordInitialState(Spatial selected) {
        startRot = selected.getLocalRotation().clone();
        startTrans = selected.getLocalTranslation().clone();
        startScale = selected.getLocalScale().clone();
    }

    /**
     * Applies the changes entered by a number, not by mouse.
     * Translate: adds the value to the current local translation
     * Rotate: rotates by X degrees
     * Scale: scale the current scale by X amount
     */
    private void applyKeyedChangeState(Spatial selected) {
        Float value = null;
        try {
            value = new Float(numberBuilder.toString());
        } catch (NumberFormatException e) {
            return;
        }

        if (currentState == State.translate) {
            MoveManager moveManager = Lookup.getDefault().lookup(MoveManager.class);
            moveManager.moveAcross(currentAxis, value, toolController.isSnapToGrid());
            moving.setAfter(selected.getLocalTranslation());
            actionPerformed(moving);
            moving = null;
        } else if (currentState == State.scale) {
            float x = 1, y = 1, z = 1;
            if (currentAxis == Vector3f.UNIT_X) {
                x = value;
            } else if (currentAxis == Vector3f.UNIT_Y) {
                y = value;
            } else if (currentAxis == Vector3f.UNIT_Z) {
                z = value;
            } else if (currentAxis == Vector3f.UNIT_XYZ) {
                x = value;
                y = value;
                z = value;
            }
            Vector3f before = selected.getLocalScale().clone();
            Vector3f after = selected.getLocalScale().multLocal(x, y, z);
            selected.setLocalScale(after);
            actionPerformed(new ScaleUndo(selected, before, after));
        } else if (currentState == State.rotate) {
            float x = 0, y = 0, z = 0;
            if (currentAxis == Vector3f.UNIT_X) {
                x = 1;
            } else if (currentAxis == Vector3f.UNIT_Y) {
                y = 1;
            } else if (currentAxis == Vector3f.UNIT_Z) {
                z = 1;
            }
            Vector3f axis = new Vector3f(x, y, z);
            Quaternion initialRot = selected.getLocalRotation().clone();
            Quaternion rot = new Quaternion();
            rot = rot.fromAngleAxis(value * FastMath.DEG_TO_RAD, axis);
            selected.setLocalRotation(selected.getLocalRotation().mult(rot));
            RotateUndo undo = new RotateUndo(selected, initialRot, rot);
            actionPerformed(undo);
            toolController.updateSelection(null);// force a re-draw of the bbox shape
            toolController.updateSelection(selected);

        }
        clearState(false);
    }

    private void checkModificatorKeys(KeyInputEvent kie) {
        if (kie.getKeyCode() == KeyInput.KEY_LCONTROL || kie.getKeyCode() == KeyInput.KEY_RCONTROL) {
            ctrlDown = kie.isPressed();
        }

        if (kie.getKeyCode() == KeyInput.KEY_LSHIFT || kie.getKeyCode() == KeyInput.KEY_RSHIFT) {
            shiftDown = kie.isPressed();
        }

        if (kie.getKeyCode() == KeyInput.KEY_LMENU || kie.getKeyCode() == KeyInput.KEY_RMENU) {
            altDown = kie.isPressed();
        }
    }

    private boolean checkCommandKey(KeyInputEvent kie) {
        if (kie.getKeyCode() == KeyInput.KEY_D) {
            if (shiftDown) {
                duplicateSelected();
                return true;
            }
        }
        // X will only delete if the user isn't already transforming
        if (currentState == null && kie.getKeyCode() == KeyInput.KEY_X) {
            if (!ctrlDown && !shiftDown) {
                deleteSelected();
                return true;
            }
        }
        return false;
    }

    private boolean checkStateKey(KeyInputEvent kie) {
        Spatial selected = toolController.getSelectedSpatial();
        if (kie.getKeyCode() == KeyInput.KEY_G) {
            currentState = State.translate;
            MoveManager moveManager = Lookup.getDefault().lookup(MoveManager.class);
            moveManager.reset();
            Quaternion rot = camera.getRotation().mult(new Quaternion().fromAngleAxis(FastMath.PI, Vector3f.UNIT_Y));
            moveManager.initiateMove(selected, rot, false);
            moving = moveManager.makeUndo();
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_R) {
            currentState = State.rotate;
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_S) {
            currentState = State.scale;
            return true;
        }
        return false;
    }

    private boolean checkAxisKey(KeyInputEvent kie) {
        if (kie.getKeyCode() == KeyInput.KEY_X) {
            currentAxis = Vector3f.UNIT_X;
            checkMovePlane(MoveManager.XY, MoveManager.XZ);
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_Y) {
            currentAxis = Vector3f.UNIT_Y;
            checkMovePlane(MoveManager.XY, MoveManager.YZ);
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_Z) {
            currentAxis = Vector3f.UNIT_Z;
            checkMovePlane(MoveManager.XZ, MoveManager.YZ);
            return true;
        }
        return false;
    }

    private void checkMovePlane(Quaternion rot1, Quaternion rot2) {
        if (currentState == State.translate) {
            MoveManager moveManager = Lookup.getDefault().lookup(MoveManager.class);
            Quaternion rot = camera.getRotation().mult(new Quaternion().fromAngleAxis(FastMath.PI, Vector3f.UNIT_Y));
            Quaternion planRot = null;
            if (rot.dot(rot1) < rot.dot(rot2)) {
                planRot = rot1;
            } else {
                planRot = rot2;
            }
            moveManager.updatePlaneRotation(planRot);
        }
    }

    private boolean checkNumberKey(KeyInputEvent kie) {
        if (kie.getKeyCode() == KeyInput.KEY_MINUS) {
            if (numberBuilder.length() > 0) {
                if (numberBuilder.charAt(0) == '-') {
                    numberBuilder.replace(0, 1, "");
                } else {
                    numberBuilder.insert(0, '-');
                }
            } else {
                numberBuilder.append('-');
            }
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_0 || kie.getKeyCode() == KeyInput.KEY_NUMPAD0) {
            numberBuilder.append('0');
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_1 || kie.getKeyCode() == KeyInput.KEY_NUMPAD1) {
            numberBuilder.append('1');
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_2 || kie.getKeyCode() == KeyInput.KEY_NUMPAD2) {
            numberBuilder.append('2');
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_3 || kie.getKeyCode() == KeyInput.KEY_NUMPAD3) {
            numberBuilder.append('3');
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_4 || kie.getKeyCode() == KeyInput.KEY_NUMPAD4) {
            numberBuilder.append('4');
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_5 || kie.getKeyCode() == KeyInput.KEY_NUMPAD5) {
            numberBuilder.append('5');
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_6 || kie.getKeyCode() == KeyInput.KEY_NUMPAD6) {
            numberBuilder.append('6');
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_7 || kie.getKeyCode() == KeyInput.KEY_NUMPAD7) {
            numberBuilder.append('7');
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_8 || kie.getKeyCode() == KeyInput.KEY_NUMPAD8) {
            numberBuilder.append('8');
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_9 || kie.getKeyCode() == KeyInput.KEY_NUMPAD9) {
            numberBuilder.append('9');
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_PERIOD) {
            if (numberBuilder.indexOf(".") == -1) { // if it doesn't exist yet
                if (numberBuilder.length() == 0
                        || (numberBuilder.length() == 1 && numberBuilder.charAt(0) == '-')) {
                    numberBuilder.append("0.");
                } else {
                    numberBuilder.append(".");
                }
            }
            return true;
        }

        return false;
    }

    private boolean checkEnterHit(KeyInputEvent kie) {
        if (kie.getKeyCode() == KeyInput.KEY_RETURN) {
            return true;
        }
        return false;
    }

    private boolean checkEscHit(KeyInputEvent kie) {
        if (kie.getKeyCode() == KeyInput.KEY_ESCAPE) {
            return true;
        }
        return false;
    }

    @Override
    public void actionPrimary(Vector2f screenCoord, boolean pressed, final JmeNode rootNode, DataObject dataObject) {
        if (!pressed) {
            Spatial selected = toolController.getSelectedSpatial();
            // left mouse released
            if (!wasDraggingL) {
                // left mouse pressed
                if (currentState != null) {
                    // finish manipulating the spatial
                    if (moving != null) {
                        moving.setAfter(selected.getLocalTranslation());
                        actionPerformed(moving);
                        moving = null;
                        clearState(false);
                    } else if (scaling != null) {
                        scaling.after = selected.getLocalScale().clone();
                        actionPerformed(scaling);
                        scaling = null;
                        clearState(false);
                        toolController.rebuildSelectionBox();
                    } else if (rotating != null) {
                        rotating.after = selected.getLocalRotation().clone();
                        actionPerformed(rotating);
                        rotating = null;
                        clearState(false);
                    }
                } else {
                    // mouse released and wasn't dragging, place cursor
                    final Vector3f result = pickWorldLocation(getCamera(), screenCoord, rootNode);
                    if (result != null) {
                        if (toolController.isSnapToGrid()) {
                            result.set(Math.round(result.x), result.y, Math.round(result.z));
                        }
                        toolController.doSetCursorLocation(result);
                    }
                }
            }
            wasDraggingL = false;
        }
    }

    @Override
    public void actionSecondary(final Vector2f screenCoord, boolean pressed, final JmeNode rootNode, DataObject dataObject) {
        if (pressed) {
            Spatial selected = toolController.getSelectedSpatial();
            // mouse down

            if (moving != null) {
                moving.sceneUndo();
                moving = null;
                clearState();
            } else if (scaling != null) {
                scaling.sceneUndo();
                scaling = null;
                clearState();
            } else if (rotating != null) {
                rotating.sceneUndo();
                rotating = null;
                clearState();
            } else if (!wasDraggingR && !wasDownR) { // wasn't dragging and was not down already
                // pick on the spot
                Spatial s = pickWorldSpatial(camera, screenCoord, rootNode);
                if (!toolController.selectTerrain() && isTerrain(s)) {
                    // only select non-terrain
                    selected = null;
                    return;
                } else {

                    // climb up and find the Model Node (parent) and select that, don't select the geom
                    if (!toolController.isSelectGeometries()) {
                        Spatial linkNodeParent = findModelNodeParent(s);
                        if (linkNodeParent != null) {
                            s = linkNodeParent;
                        } else {
                            return;
                        }
                    }
                    final Spatial selec = s;
                    selected = selec;
                    java.awt.EventQueue.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            if (selec != null) {
                                doSelect();
                            }
                        }

                        private void doSelect() {
                            //in case of  linked assets the selected nod ein the viewer is not necessarily in the explorer.
                            JmeSpatial n = rootNode.getChild(selec);
                            if (n != null) {
                                SceneViewerTopComponent.findInstance().setActivatedNodes(new org.openide.nodes.Node[]{n});
                                SceneExplorerTopComponent.findInstance().setSelectedNode(n);
                            }
                        }
                    });
                }

                toolController.updateSelection(selected);
            }
            wasDownR = true;
        } else {
            // mouse up, stop everything
            wasDownR = false;
            wasDraggingR = false;
        }
    }

    /**
     * Climb up the spatial until we find the first node parent.
     * TODO: use userData to determine the actual model's parent.
     */
    private Spatial findModelNodeParent(Spatial child) {
        if (child == null) {
            return null;
        }

        if (child instanceof Node) {
            return child;
        }

        if (child.getParent() != null) {
            return findModelNodeParent(child.getParent());
        }

        return null;
    }

    @Override
    public void mouseMoved(Vector2f screenCoord, JmeNode rootNode, DataObject currentDataObject, JmeSpatial selectedSpatial) {
        if (currentState != null) {
            handleMouseManipulate(screenCoord, currentState, currentAxis, rootNode, currentDataObject, selectedSpatial);
        }
    }

    @Override
    public void draggedPrimary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject) {
        wasDraggingL = pressed;
    }

    @Override
    public void draggedSecondary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject) {
        wasDraggingR = pressed;
    }

    /**
     * Manipulate the spatial
     */
    private void handleMouseManipulate(Vector2f screenCoord,
            State state,
            Vector3f axis,
            JmeNode rootNode,
            DataObject currentDataObject,
            JmeSpatial selectedSpatial) {
        if (state == State.translate) {
            doMouseTranslate(axis, screenCoord, rootNode, selectedSpatial);
        } else if (state == State.scale) {
            doMouseScale(axis, screenCoord, rootNode, selectedSpatial);
        } else if (state == State.rotate) {
            doMouseRotate(axis, screenCoord, rootNode, selectedSpatial);
        }

    }

    private void doMouseTranslate(Vector3f axis, Vector2f screenCoord, JmeNode rootNode, JmeSpatial selectedSpatial) {
        MoveManager moveManager = Lookup.getDefault().lookup(MoveManager.class);
        if (toolController.isSnapToScene()) {
            moveManager.setAlternativePickTarget(rootNode.getLookup().lookup(Node.class));
        }
        // free form translation
        moveManager.move(camera, screenCoord, axis, toolController.isSnapToGrid());
    }

    private void doMouseScale(Vector3f axis, Vector2f screenCoord, JmeNode rootNode, JmeSpatial selectedSpatial) {
        Spatial selected = toolController.getSelectedSpatial();
        // scale based on the original mouse position and original model-to-screen position
        // and compare that to the distance from the new mouse position and the original distance
        if (startMouseCoord == null) {
            startMouseCoord = screenCoord.clone();
        }
        if (startSelectedCoord == null) {
            Vector3f screen = getCamera().getScreenCoordinates(selected.getWorldTranslation());
            startSelectedCoord = new Vector2f(screen.x, screen.y);
        }

        if (scaling == null) {
            scaling = new ScaleUndo(selected, selected.getLocalScale().clone(), null);
        }

        float origDist = startMouseCoord.distanceSquared(startSelectedCoord);
        float newDist = screenCoord.distanceSquared(startSelectedCoord);
        if (origDist == 0) {
            origDist = 1;
        }
        float ratio = newDist / origDist;
        Vector3f prev = selected.getLocalScale();
        if (axis == Vector3f.UNIT_X) {
            selected.setLocalScale(ratio, prev.y, prev.z);
        } else if (axis == Vector3f.UNIT_Y) {
            selected.setLocalScale(prev.x, ratio, prev.z);
        } else if (axis == Vector3f.UNIT_Z) {
            selected.setLocalScale(prev.x, prev.y, ratio);
        } else {
            selected.setLocalScale(ratio, ratio, ratio);
        }
    }

    private void doMouseRotate(Vector3f axis, Vector2f screenCoord, JmeNode rootNode, JmeSpatial selectedSpatial) {
        Spatial selected = toolController.getSelectedSpatial();
        if (startMouseCoord == null) {
            startMouseCoord = screenCoord.clone();
        }
        if (startSelectedCoord == null) {
            Vector3f screen = getCamera().getScreenCoordinates(selected.getWorldTranslation());
            startSelectedCoord = new Vector2f(screen.x, screen.y);
        }

        if (rotating == null) {
            rotating = new RotateUndo(selected, selected.getLocalRotation().clone(), null);
        }

        Vector2f origRot = startMouseCoord.subtract(startSelectedCoord);
        Vector2f newRot = screenCoord.subtract(startSelectedCoord);
        float newRotAngle = origRot.angleBetween(newRot);
        float temp = newRotAngle;

        if (lastRotAngle != 0) {
            newRotAngle -= lastRotAngle;
        }

        lastRotAngle = temp;

        Quaternion rotate = new Quaternion();
        if (axis != Vector3f.UNIT_XYZ) {
            rotate = rotate.fromAngleAxis(newRotAngle, selected.getWorldRotation().inverse().mult(axis));
        } else {
            rotate = rotate.fromAngleAxis(newRotAngle, selected.getWorldRotation().inverse().mult(getCamera().getDirection().mult(-1).normalizeLocal()));
        }
        selected.setLocalRotation(selected.getLocalRotation().mult(rotate));


    }

    private void duplicateSelected() {
        Spatial selected = toolController.getSelectedSpatial();
        if (selected == null) {
            return;
        }
        Spatial clone = selected.clone();
        clone.move(1, 0, 1);

        selected.getParent().attachChild(clone);
        actionPerformed(new DuplicateUndo(clone, selected.getParent()));
        selected = clone;
        final Spatial cloned = clone;
        final JmeNode rootNode = toolController.getRootNode();
        refreshSelected(rootNode, selected.getParent());

        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (cloned != null) {
                    SceneViewerTopComponent.findInstance().setActivatedNodes(new org.openide.nodes.Node[]{rootNode.getChild(cloned)});
                    SceneExplorerTopComponent.findInstance().setSelectedNode(rootNode.getChild(cloned));
                }
            }
        });

        // set to automatically 'grab'/'translate' the new cloned model
        toolController.updateSelection(selected);
        currentState = State.translate;
        currentAxis = Vector3f.UNIT_XYZ;
    }

    private void deleteSelected() {
        Spatial selected = toolController.getSelectedSpatial();
        if (selected == null) {
            return;
        }
        Node parent = selected.getParent();
        selected.removeFromParent();
        actionPerformed(new DeleteUndo(selected, parent));

        selected = null;
        toolController.updateSelection(selected);

        final JmeNode rootNode = toolController.getRootNode();
        refreshSelected(rootNode, parent);
    }

    private void refreshSelected(final JmeNode jmeRootNode, final Node parent) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                jmeRootNode.getChild(parent).refresh(false);
            }
        });
    }

    private class ScaleUndo extends AbstractUndoableSceneEdit {

        private Spatial spatial;
        private Vector3f before, after;

        ScaleUndo(Spatial spatial, Vector3f before, Vector3f after) {
            this.spatial = spatial;
            this.before = before;
            this.after = after;
        }

        @Override
        public void sceneUndo() {
            spatial.setLocalScale(before);
        }

        @Override
        public void sceneRedo() {
            spatial.setLocalScale(after);
        }
    }

    private class RotateUndo extends AbstractUndoableSceneEdit {

        private Spatial spatial;
        private Quaternion before, after;

        RotateUndo(Spatial spatial, Quaternion before, Quaternion after) {
            this.spatial = spatial;
            this.before = before;
            this.after = after;
        }

        @Override
        public void sceneUndo() {
            spatial.setLocalRotation(before);
        }

        @Override
        public void sceneRedo() {
            spatial.setLocalRotation(after);
        }
    }

    private class DeleteUndo extends AbstractUndoableSceneEdit {

        private Spatial spatial;
        private Node parent;

        DeleteUndo(Spatial spatial, Node parent) {
            this.spatial = spatial;
            this.parent = parent;
        }

        @Override
        public void sceneUndo() {
            parent.attachChild(spatial);
        }

        @Override
        public void sceneRedo() {
            spatial.removeFromParent();
        }
    }

    private class DuplicateUndo extends AbstractUndoableSceneEdit {

        private Spatial spatial;
        private Node parent;

        DuplicateUndo(Spatial spatial, Node parent) {
            this.spatial = spatial;
            this.parent = parent;
        }

        @Override
        public void sceneUndo() {
            spatial.removeFromParent();
        }

        @Override
        public void sceneRedo() {
            parent.attachChild(spatial);
        }
    }

    /**
     * Check if the selected item is a Terrain
     * It will climb up the parent tree to see if
     * a parent is terrain too.
     * Recursive call.
     */
    protected boolean isTerrain(Spatial s) {
        if (s == null) {
            return false;
        }
        if (s instanceof Terrain) {
            return true;
        }

        if (s.getParent() != null) {
            return isTerrain(s.getParent());
        }
        return false;
    }
}
