/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.assetpack.actions;

import com.jme3.gde.assetpack.AssetConfiguration;
import com.jme3.gde.assetpack.AssetPackLoader;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.scene.SceneRequest;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.gde.core.sceneexplorer.nodes.NodeUtility;
import com.jme3.scene.Spatial;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import org.openide.nodes.Node;
import org.w3c.dom.Element;

public final class PreviewAssetAction implements Action {

    private final Node context;

    public PreviewAssetAction(Node context) {
        this.context = context;
    }

    public void actionPerformed(ActionEvent ev) {
        ProjectAssetManager pm = context.getLookup().lookup(ProjectAssetManager.class);
        if (pm == null) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "AssetManager not found!");
            return;
        }
        Element assetElement = context.getLookup().lookup(Element.class);
        com.jme3.scene.Node node = new com.jme3.scene.Node("PreviewRootNode");
        Spatial model = null;
        model = AssetPackLoader.loadAssetPackModel(pm, new AssetConfiguration(assetElement));
        node.attachChild(model);
        JmeNode jmeNode = NodeUtility.createNode(node);
        SceneApplication app = SceneApplication.getApplication();
        SceneRequest request = new SceneRequest(app, jmeNode, pm);
        request.setWindowTitle("SceneViewer - PreView AssetPack Model");
        app.requestScene(request);

    }

    public Object getValue(String key) {
        if (key.equals(NAME)) {
            return "Preview Asset..";
        }
        return null;
    }

    public void putValue(String key, Object value) {
    }

    public void setEnabled(boolean b) {
    }

    public boolean isEnabled() {
        return true;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
    }
}
