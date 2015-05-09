/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer.tools.shortcuts;

import com.jme3.asset.AssetManager;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.gde.core.sceneexplorer.nodes.JmeSpatial;
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
    private Vector3f initalLocation;
    private Vector3f finalLocation;
    private PickManager pickManager;

    @Override
    public boolean isActivableBy(KeyInputEvent kie) {
        return kie.getKeyCode() == KeyInput.KEY_G;
    }

    @Override
    public void cancel() {
        spatial.setLocalTranslation(initalLocation);
        terminate();
    }

    private void apply() {
        // TODO creat UNDO/REDO
        terminate();
    }

    @Override
    public void activate(AssetManager manager, Node toolNode, Node onTopToolNode, Spatial selectedSpatial, SceneComposerToolController toolController) {
        super.activate(manager, toolNode, onTopToolNode, selectedSpatial, toolController); //To change body of generated methods, choose Tools | Templates.
        hideMarker();
        numberBuilder = new StringBuilder();
        if (selectedSpatial == null) {
            terminate();
        } else {
            spatial = selectedSpatial;
            initalLocation = spatial.getLocalTranslation();
            currentAxis = new Vector3f().set(Vector3f.UNIT_XYZ);

            pickManager = Lookup.getDefault().lookup(PickManager.class);
            ///pickManager.initiatePick(toolController.getSelectedSpatial(), PickManager.PLANE_YZ, getTransformType(), camera, screenCoord);
        }
    }

    @Override
    public void keyPressed(KeyInputEvent kie) {
        if (kie.isPressed()) {

            /*
             ShortcutTool otherShortcut = Lookup.getDefault().lookup(ShortcutManager.class).getActivableShortcut(kie);
             if(otherShortcut != null){
             Lookup.getDefault().lookup(ShortcutManager.class).setShortCut(otherShortcut);
             }*/
            Lookup.getDefault().lookup(ShortcutManager.class).activateShortcut(kie);

            boolean axisChanged = ShortcutManager.checkAxisKey(kie, currentAxis);
            boolean numberChanged = ShortcutManager.checkNumberKey(kie, numberBuilder);
            boolean enterHit = ShortcutManager.checkEnterHit(kie);
            boolean escHit = ShortcutManager.checkEscHit(kie);

            if (escHit) {
                cancel();
            } else if (enterHit) {
                apply();
            } else if (axisChanged || numberChanged) {
                //update transformation
                float number = ShortcutManager.getNumberkey(numberBuilder);
                Vector3f translation = currentAxis.mult(number);
                finalLocation = initalLocation.add(translation);
                spatial.setLocalTranslation(finalLocation);

            }

        }
    }

    @Override
    public void actionPrimary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject dataObject) {
        if (!pressed) {
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
        pickManager.updatePick(camera, screenCoord);
        /* PickManager pickManager = Lookup.getDefault().lookup(PickManager.class);
         if (toolController.isSnapToScene()) {
         moveManager.setAlternativePickTarget(rootNode.getLookup().lookup(Node.class));
         }
         // free form translation
         moveManager.move(camera, screenCoord, axis, toolController.isSnapToGrid());*/
    }

    @Override
    public void draggedPrimary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void draggedSecondary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject) {
        if (pressed) {
            cancel();
        }
    }

}
