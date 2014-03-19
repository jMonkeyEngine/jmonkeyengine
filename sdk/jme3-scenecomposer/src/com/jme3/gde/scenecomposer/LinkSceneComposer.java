/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer;

import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.assets.SpatialAssetDataObject;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.List;
import org.openide.filesystems.FileObject;

public final class LinkSceneComposer implements ActionListener {

    private final List<SpatialAssetDataObject> context;

    public LinkSceneComposer(List<SpatialAssetDataObject> context) {
        this.context = context;
    }

    public void actionPerformed(ActionEvent ev) {
        for (Iterator<SpatialAssetDataObject> it = context.iterator(); it.hasNext();) {
            SpatialAssetDataObject spatialAssetDataObject = it.next();
            ProjectAssetManager manager = spatialAssetDataObject.getLookup().lookup(ProjectAssetManager.class);
            if (manager == null) {
                return;
            }
            FileObject file = spatialAssetDataObject.getPrimaryFile();
            String assetName = manager.getRelativeAssetPath(file.getPath());
            SceneComposerTopComponent composer = SceneComposerTopComponent.findInstance();
            composer.linkModel(manager, assetName);
        }
    }
}
