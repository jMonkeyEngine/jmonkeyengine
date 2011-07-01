/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.assetpack.actions;

import com.jme3.gde.assetpack.AssetConfiguration;
import com.jme3.gde.assetpack.AssetPackLoader;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.scene.Spatial;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import org.openide.nodes.Node;
import org.w3c.dom.Element;
import com.jme3.gde.scenecomposer.SceneComposerTopComponent;
import java.util.ArrayList;
import java.util.List;

public final class AddAssetAction implements Action {

    private final Node context;

    public AddAssetAction(Node context) {
        this.context = context;
    }

    public void actionPerformed(ActionEvent ev) {
        ProjectAssetManager pm = context.getLookup().lookup(ProjectAssetManager.class);
        if (pm == null) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "AssetManager not found!");
            return;
        }
        Element assetElement = context.getLookup().lookup(Element.class);
        String type = assetElement.getAttribute("type");
        if ("model".equals(type) || "scene".equals(type)) {
            AssetConfiguration conf = new AssetConfiguration(assetElement);
            Spatial model = AssetPackLoader.loadAssetPackModel(pm, conf);
            if (model != null) {
                SceneComposerTopComponent.findInstance().addModel(model);
                AssetPackLoader.addModelFiles(pm, conf);
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Error loading model");
            }
        } else {
            AssetConfiguration conf = new AssetConfiguration(assetElement);
            AssetPackLoader.addAllFiles(pm, conf);
        }
    }

    public Object getValue(String key) {
        if (key.equals(NAME)) {
            return "Add to SceneComposer..";
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
