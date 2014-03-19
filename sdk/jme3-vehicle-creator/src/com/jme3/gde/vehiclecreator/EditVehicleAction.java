/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.vehiclecreator;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.gde.core.assets.BinaryModelDataObject;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.NotifyDescriptor.Confirmation;

public final class EditVehicleAction implements ActionListener {

    private final BinaryModelDataObject context;

    public EditVehicleAction(BinaryModelDataObject context) {
        this.context = context;
    }

    public void actionPerformed(ActionEvent ev) {
        final ProjectAssetManager manager = context.getLookup().lookup(ProjectAssetManager.class);
        if (manager == null) {
            return;
        }
        Runnable call = new Runnable() {

            public void run() {
                ProgressHandle progressHandle = ProgressHandleFactory.createHandle("Opening in Vehicle Editor");
                progressHandle.start();


                final Spatial asset = context.loadAsset();

                if (asset != null && asset instanceof Node) {
                    java.awt.EventQueue.invokeLater(new Runnable() {

                        public void run() {
                            manager.clearCache();
                            VehicleCreatorTopComponent composer = VehicleCreatorTopComponent.findInstance();
                            composer.openFile(context, (Node) asset);
                        }
                    });
                } else {
                    Confirmation msg = new NotifyDescriptor.Confirmation(
                            "Error opening " + context.getPrimaryFile().getNameExt(),
                            NotifyDescriptor.OK_CANCEL_OPTION,
                            NotifyDescriptor.ERROR_MESSAGE);
                    DialogDisplayer.getDefault().notify(msg);
                }
                progressHandle.finish();
            }
        };
        new Thread(call).start();
    }
}
