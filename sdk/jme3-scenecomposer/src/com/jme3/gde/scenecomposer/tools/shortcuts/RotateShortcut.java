/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer.tools.shortcuts;

import com.jme3.asset.AssetManager;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.gde.core.sceneexplorer.nodes.JmeSpatial;
import com.jme3.gde.core.undoredo.AbstractUndoableSceneEdit;
import com.jme3.gde.scenecomposer.SceneComposerToolController;
import com.jme3.gde.scenecomposer.tools.PickManager;
import com.jme3.input.KeyInput;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;

/**
 *
 * @author dokthar
 */
public class RotateShortcut extends ShortcutTool {

    private Vector3f currentAxis;
    private StringBuilder numberBuilder;
    private Spatial spatial;
    private PickManager pickManager;
    private boolean pickEnabled;
    private Quaternion startRotation;
    private Quaternion finalRotation;

    @Override

    public boolean isActivableBy(KeyInputEvent kie) {
        return kie.getKeyCode() == KeyInput.KEY_R;
    }

    @Override
    public void cancel() {
        spatial.setLocalRotation(startRotation);
        terminate();
    }

    private void apply() {
        actionPerformed(new RotateUndo(toolController.getSelectedSpatial(), startRotation, finalRotation));
        terminate();
    }

    private void init(Spatial selectedSpatial) {
        spatial = selectedSpatial;
        startRotation = spatial.getLocalRotation().clone();
        currentAxis = Vector3f.UNIT_XYZ;
        pickManager = Lookup.getDefault().lookup(PickManager.class);
        pickEnabled = false;
    }

    @Override
    public void activate(AssetManager manager, Node toolNode, Node onTopToolNode, Spatial selectedSpatial, SceneComposerToolController toolController) {
        super.activate(manager, toolNode, onTopToolNode, selectedSpatial, toolController); //To change body of generated methods, choose Tools | Templates.
        hideMarker();
        numberBuilder = new StringBuilder();
        if (selectedSpatial == null) {
            terminate();
        } else {
            init(selectedSpatial);
        }
    }

    @Override
    public void keyPressed(KeyInputEvent kie) {
        if (kie.isPressed()) {
            Lookup.getDefault().lookup(ShortcutManager.class).activateShortcut(kie);

            Vector3f axis = new Vector3f();
            boolean axisChanged = ShortcutManager.checkAxisKey(kie, axis);
            if (axisChanged) {
                currentAxis = axis;
            }
            boolean numberChanged = ShortcutManager.checkNumberKey(kie, numberBuilder);
            boolean enterHit = ShortcutManager.checkEnterHit(kie);
            boolean escHit = ShortcutManager.checkEscHit(kie);

            if (escHit) {
                cancel();
            } else if (enterHit) {
                apply();
            } else if (axisChanged && pickEnabled) {
                pickEnabled = false;
                spatial.setLocalRotation(startRotation.clone());
            } else if (axisChanged || numberChanged) {
                //update transformation
       /*         float number = ShortcutManager.getNumberKey(numberBuilder);
                 Vector3f translation = currentAxis.mult(number);
                 finalPosition = startPosition.add(translation);
                 spatial.setLocalTranslation(finalPosition);
                 */
            }

        }
    }

    @Override
    public void actionPrimary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject dataObject) {
        if (pressed) {
            apply();
        }
    }

    @Override
    public void actionSecondary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject dataObject) {
        if (pressed) {
            cancel();
        }
    }

    @Override
    public void mouseMoved(Vector2f screenCoord, JmeNode rootNode, DataObject dataObject, JmeSpatial selectedSpatial) {

        if (!pickEnabled) {
            if (currentAxis.equals(Vector3f.UNIT_XYZ)) {
                pickManager.initiatePick(toolController.getSelectedSpatial(), camera.getRotation(), SceneComposerToolController.TransformationType.camera, camera, screenCoord);
                pickEnabled = true;
            } else if (currentAxis.equals(Vector3f.UNIT_X)) {
                pickManager.initiatePick(toolController.getSelectedSpatial(), PickManager.PLANE_YZ, getTransformType(), camera, screenCoord);
                pickEnabled = true;
            } else if (currentAxis.equals(Vector3f.UNIT_Y)) {
                pickManager.initiatePick(toolController.getSelectedSpatial(), PickManager.PLANE_XZ, getTransformType(), camera, screenCoord);
                pickEnabled = true;
            } else if (currentAxis.equals(Vector3f.UNIT_Z)) {
                pickManager.initiatePick(toolController.getSelectedSpatial(), PickManager.PLANE_XY, getTransformType(), camera, screenCoord);
                pickEnabled = true;
            } else {
                return;
            }
        }

        if (pickManager.updatePick(camera, screenCoord)) {

            Quaternion rotation = startRotation.mult(pickManager.getRotation(startRotation.inverse()));
            toolController.getSelectedSpatial().setLocalRotation(rotation);
            finalRotation = rotation;
            updateToolsTransformation();
        }
    }

    @Override
    public void draggedPrimary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject) {
        if (pressed) {
            apply();
        }
    }

    @Override
    public void draggedSecondary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject) {
        if (pressed) {
            cancel();
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
            toolController.selectedSpatialTransformed();
        }

        @Override
        public void sceneRedo() {
            spatial.setLocalRotation(after);
            toolController.selectedSpatialTransformed();
        }
    }
}
