/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.assetpack.actions;

import com.jme3.export.binary.BinaryExporter;
import com.jme3.gde.assetpack.AssetConfiguration;
import com.jme3.gde.assetpack.AssetPackLoader;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.scene.Spatial;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.w3c.dom.Element;
import com.jme3.gde.scenecomposer.SceneComposerTopComponent;
import java.io.OutputStream;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.NotifyDescriptor.Message;
import org.openide.filesystems.FileObject;

public final class AddToProjectAction implements Action {

    private final Node context;

    public AddToProjectAction(Node context) {
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
        try {
            if ("model".equals(type) || "scene".equals(type)) {
                AssetConfiguration conf = new AssetConfiguration(assetElement);
                Spatial model = AssetPackLoader.loadAssetPackModel(pm, conf);
                if (model != null) {
                    ProjectAssetManager mgr = ProjectSelection.getProjectAssetManager();
                    if (mgr != null && mgr != pm) {
                        FileObject modelFolder = mgr.getAssetFolder().getFileObject("Models");
                        if (modelFolder == null) {
                            modelFolder = mgr.getAssetFolder().createFolder("Models");
                        }
                        if (modelFolder.isFolder()) {
                            AssetPackLoader.addModelFiles(pm, mgr, conf);
                            OutputStream out = modelFolder.createAndOpen(conf.getAssetElement().getAttribute("name") + ".j3o");
                            BinaryExporter.getInstance().save(model, out);
                            out.close();
                            modelFolder.refresh();
                        } else {
                            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Cannot copy, file 'Models' exists");
                        }
                    } else {
                        Message msg = new NotifyDescriptor.Message(
                                "Please open a model from the destination\n"
                                + "project in the SceneExplorer\n"
                                + "to define the project.\n"
                                + "(temp. workaround)",
                                NotifyDescriptor.ERROR_MESSAGE);
                        DialogDisplayer.getDefault().notifyLater(msg);
                    }
                } else {
                    Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Error loading model");
                }
            } else {
                AssetConfiguration conf = new AssetConfiguration(assetElement);
                ProjectAssetManager mgr = ProjectSelection.getProjectAssetManager();
                AssetPackLoader.addAllFiles(pm, mgr, conf);
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public Object getValue(String key) {
        if (key.equals(NAME)) {
            return "Add to Project..";
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
