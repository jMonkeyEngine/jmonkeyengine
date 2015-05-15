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
public class DuplicateShortcut extends ShortcutTool {

    @Override
    public boolean isActivableBy(KeyInputEvent kie) {
        if (kie.getKeyCode() == KeyInput.KEY_D && kie.isPressed()) {
            if (Lookup.getDefault().lookup(ShortcutManager.class).isShiftDown()) {
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
            duplicate();
            terminate();
            
            //then enable move shortcut
            toolController.doKeyPressed(new KeyInputEvent(KeyInput.KEY_G, 'g', true, false));
        } else {
            terminate();
        }
    }

    private void duplicate() {
        Spatial selected = toolController.getSelectedSpatial();

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

        toolController.updateSelection(selected);
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
}
