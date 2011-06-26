/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.assets.BinaryModelDataObject;
import com.jme3.scene.Spatial;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.NotifyDescriptor.Confirmation;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import sun.misc.Perf.GetPerfAction;

public final class OpenSceneComposer implements ActionListener {

    private final BinaryModelDataObject context;

    public OpenSceneComposer(BinaryModelDataObject context) {
        this.context = context;
    }

    public void actionPerformed(ActionEvent ev) {
        final ProjectAssetManager manager = context.getLookup().lookup(ProjectAssetManager.class);
        if (manager == null) {
            return;
        }
        Runnable call = new Runnable() {

            public void run() {
                ProgressHandle progressHandle = ProgressHandleFactory.createHandle("Opening in SceneComposer");
                progressHandle.start();
                try {

                    final Spatial asset = context.loadAsset();

                    if (asset != null) {
                        java.awt.EventQueue.invokeLater(new Runnable() {

                            public void run() {
                                manager.clearCache();
                                SceneComposerTopComponent composer = SceneComposerTopComponent.findInstance();
                                composer.openScene(asset, context, manager);
                            }
                        });
                    } else {
                        Confirmation msg = new NotifyDescriptor.Confirmation(
                                "Error opening " + context.getPrimaryFile().getNameExt(),
                                NotifyDescriptor.OK_CANCEL_OPTION,
                                NotifyDescriptor.ERROR_MESSAGE);
                        DialogDisplayer.getDefault().notify(msg);
                    }
                } finally {
                    progressHandle.finish();
                }
            }
        };
        new Thread(call).start();
    }
}
