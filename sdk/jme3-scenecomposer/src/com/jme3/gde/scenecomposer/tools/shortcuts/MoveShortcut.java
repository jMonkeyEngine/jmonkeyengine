/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer.tools.shortcuts;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.gde.core.sceneexplorer.nodes.JmeSpatial;
import com.jme3.gde.core.undoredo.AbstractUndoableSceneEdit;
import com.jme3.gde.scenecomposer.SceneComposerToolController;
import com.jme3.gde.scenecomposer.tools.PickManager;
import com.jme3.input.KeyInput;
import com.jme3.input.event.KeyInputEvent;
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
public class MoveShortcut extends ShortcutTool {

    private Vector3f currentAxis;
    private StringBuilder numberBuilder;
    private Spatial spatial;
    private PickManager pickManager;
    private boolean pickEnabled;
    private Vector3f startPosition;
    private Vector3f finalPosition;

    @Override

    public boolean isActivableBy(KeyInputEvent kie) {
        return kie.getKeyCode() == KeyInput.KEY_G;
    }

    @Override
    public void cancel() {
        spatial.setLocalTranslation(startPosition);
        terminate();
    }

    private void apply() {
        actionPerformed(new MoveUndo(toolController.getSelectedSpatial(), startPosition, finalPosition));
        terminate();
    }

    private void init(Spatial selectedSpatial) {
        spatial = selectedSpatial;
        startPosition = spatial.getLocalTranslation().clone();
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
                //update pick manager

                if (currentAxis.equals(Vector3f.UNIT_X)) {
                    pickManager.setTransformation(PickManager.PLANE_XY, getTransformType(), camera);
                } else if (currentAxis.equals(Vector3f.UNIT_Y)) {
                    pickManager.setTransformation(PickManager.PLANE_YZ, getTransformType(), camera);
                } else if (currentAxis.equals(Vector3f.UNIT_Z)) {
                    pickManager.setTransformation(PickManager.PLANE_XZ, getTransformType(), camera);
                }
            } else if (axisChanged || numberChanged) {
                //update transformation
                float number = ShortcutManager.getNumberKey(numberBuilder);
                Vector3f translation = currentAxis.mult(number);
                finalPosition = startPosition.add(translation);
                spatial.setLocalTranslation(finalPosition);

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
                pickManager.initiatePick(toolController.getSelectedSpatial(), PickManager.PLANE_XY, getTransformType(), camera, screenCoord);
                pickEnabled = true;
            } else if (currentAxis.equals(Vector3f.UNIT_Y)) {
                pickManager.initiatePick(toolController.getSelectedSpatial(), PickManager.PLANE_YZ, getTransformType(), camera, screenCoord);
                pickEnabled = true;
            } else if (currentAxis.equals(Vector3f.UNIT_Z)) {
                pickManager.initiatePick(toolController.getSelectedSpatial(), PickManager.PLANE_XZ, getTransformType(), camera, screenCoord);
                pickEnabled = true;
            } else {
                return;
            }
        }

        if (pickManager.updatePick(camera, screenCoord)) {
            //pick update success
            Vector3f diff;

            if (currentAxis.equals(Vector3f.UNIT_XYZ)) {
                diff = pickManager.getTranslation();
            } else {
                diff = pickManager.getTranslation(currentAxis);
            }
            Vector3f position = startPosition.add(diff);
            finalPosition = position;
            toolController.getSelectedSpatial().setLocalTranslation(position);
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
    
    private class MoveUndo extends AbstractUndoableSceneEdit {

        private Spatial spatial;
        private Vector3f before = new Vector3f(), after = new Vector3f();

        MoveUndo(Spatial spatial, Vector3f before, Vector3f after) {
            this.spatial = spatial;
            this.before.set(before);
            if (after != null) {
                this.after.set(after);
            }
        }

        @Override
        public void sceneUndo() {
            spatial.setLocalTranslation(before);
            RigidBodyControl control = spatial.getControl(RigidBodyControl.class);
            if (control != null) {
                control.setPhysicsLocation(spatial.getWorldTranslation());
            }
            CharacterControl character = spatial.getControl(CharacterControl.class);
            if (character != null) {
                character.setPhysicsLocation(spatial.getWorldTranslation());
            }
            //     toolController.selectedSpatialTransformed();
        }

        @Override
        public void sceneRedo() {
            spatial.setLocalTranslation(after);
            RigidBodyControl control = spatial.getControl(RigidBodyControl.class);
            if (control != null) {
                control.setPhysicsLocation(spatial.getWorldTranslation());
            }
            CharacterControl character = spatial.getControl(CharacterControl.class);
            if (character != null) {
                character.setPhysicsLocation(spatial.getWorldTranslation());
            }
            //toolController.selectedSpatialTransformed();
        }

        public void setAfter(Vector3f after) {
            this.after.set(after);
        }
    }

}
