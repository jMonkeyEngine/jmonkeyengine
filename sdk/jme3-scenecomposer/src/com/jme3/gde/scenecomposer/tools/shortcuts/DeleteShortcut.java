/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer.tools.shortcuts;

import com.jme3.asset.AssetManager;
import com.jme3.gde.core.sceneexplorer.SceneExplorerTopComponent;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.gde.core.sceneexplorer.nodes.JmeSpatial;
import com.jme3.gde.core.sceneviewer.SceneViewerTopComponent;
import com.jme3.gde.core.undoredo.AbstractUndoableSceneEdit;
import com.jme3.gde.scenecomposer.SceneComposerToolController;
import com.jme3.input.KeyInput;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.math.Vector2f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;

/**
 *
 * @author dokthar
 */
public class DeleteShortcut extends ShortcutTool {

    @Override
    public boolean isActivableBy(KeyInputEvent kie) {
        if (kie.getKeyCode() == KeyInput.KEY_X && kie.isPressed()) {
            ShortcutManager scm = Lookup.getDefault().lookup(ShortcutManager.class);
            if (!scm.isActive() && scm.isShiftDown()) {
                // ^ can't be enable if an other shortcut is allready active
                return true;
            }
        }
        return false;
    }

    @Override
    public void cancel() {
        terminate();
    }

    @Override
    public void activate(AssetManager manager, Node toolNode, Node onTopToolNode, Spatial selectedSpatial, SceneComposerToolController toolController) {
        super.activate(manager, toolNode, onTopToolNode, selectedSpatial, toolController); //To change body of generated methods, choose Tools | Templates.
        hideMarker();
        if (selectedSpatial != null) {
            delete();
        }
        terminate();
    }

    private void delete() {
        Spatial selected = toolController.getSelectedSpatial();

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

    @Override
    public void keyPressed(KeyInputEvent kie) {

    }

    @Override
    public void actionPrimary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject dataObject) {
    }

    @Override
    public void actionSecondary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject dataObject) {
    }

    @Override
    public void mouseMoved(Vector2f screenCoord, JmeNode rootNode, DataObject dataObject, JmeSpatial selectedSpatial) {
    }

    @Override
    public void draggedPrimary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject) {
    }

    @Override
    public void draggedSecondary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject) {
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
}
