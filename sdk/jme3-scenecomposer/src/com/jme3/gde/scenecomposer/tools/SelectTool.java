/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package com.jme3.gde.scenecomposer.tools;

import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.gde.scenecomposer.SceneEditTool;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import org.openide.loaders.DataObject;

/**
 *
 * @author Brent Owens
 */
public class SelectTool extends SceneEditTool {

    protected Spatial selected;
    private boolean wasDragging = false;

    @Override
    public void actionPrimary(Vector2f screenCoord, boolean pressed, final JmeNode rootNode, DataObject dataObject) {
        if (!pressed && !wasDragging) {
            // mouse released and wasn't dragging, select a new spatial
            final Spatial result = pickWorldSpatial(getCamera(), screenCoord, rootNode);

            java.awt.EventQueue.invokeLater(new Runnable() {

                public void run() {
                    if (result != null) {
                        SceneApplication.getApplication().setCurrentFileNode(rootNode.getChild(result));
                    } else {
                        SceneApplication.getApplication().setCurrentFileNode(rootNode);
                    }
                }
            });

            if (result != null) {
                replaceSelectionShape(result);
                updateToolsTransformation(selectedSpatial);
            }
        }

        if (!pressed) {
            wasDragging = false;
        }
    }

    @Override
    public void actionSecondary(final Vector2f screenCoord, boolean pressed, final JmeNode rootNode, DataObject dataObject) {
        if (!pressed && !wasDragging) {
            final Vector3f result = pickWorldLocation(getCamera(), screenCoord, rootNode);
            if (result != null) {
                toolController.doSetCursorLocation(result);
            }
        }
        if (!pressed) {
            wasDragging = false;
        }
    }

    @Override
    public void mouseMoved(Vector2f screenCoord) {
    }

    @Override
    public void draggedPrimary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject) {
        wasDragging = pressed;
    }

    @Override
    public void draggedSecondary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject) {
        wasDragging = pressed;
    }
}
