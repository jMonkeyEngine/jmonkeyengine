/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.ogretools.convert;

import com.jme3.export.binary.BinaryExporter;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.scene.Spatial;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.NotifyDescriptor.Confirmation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;

public final class AdvOgreXMLConvertAction implements ActionListener {

    private final DataObject context;

    public AdvOgreXMLConvertAction(DataObject context) {
        this.context = context;
    }

    public void actionPerformed(ActionEvent ev) {
        final ProjectAssetManager manager = context.getLookup().lookup(ProjectAssetManager.class);
        if (manager == null) {
//            StatusDisplayer.getDefault().setStatusText("Project has no AssetManager!");
            return;
        }

        // TODO use context
        final FileObject file = context.getPrimaryFile();
        final OgreXMLConvertOptions options = new OgreXMLConvertOptions(file.getPath(), file.getParent().getPath() + "/" + "+" + file.getNameExt());
        AdvOgreXMLConvertDialog dialog = new AdvOgreXMLConvertDialog(WindowManager.getDefault().getMainWindow(), true, options);
        dialog.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dialog.setVisible(true);


        Runnable run = new Runnable() {

            public void run() {
                ProgressHandle progressHandle = ProgressHandleFactory.createHandle("Converting OgreXML");
                progressHandle.start();

                OgreXMLConvert converter = new OgreXMLConvert();
                if (!converter.doConvert(options, progressHandle)) {
                    progressHandle.finish();
                    return;
                }
                FileObject sourceMatFile = FileUtil.toFileObject(new File(options.getSourceFile().replaceAll("mesh.xml", "material")));
                if (sourceMatFile != null && sourceMatFile.isValid()) {
                    try {
                        sourceMatFile.copy(sourceMatFile.getParent(), "+" + sourceMatFile.getName(), sourceMatFile.getExt());
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                } else {
                    Confirmation msg = new NotifyDescriptor.Confirmation(
                            "No material file found for " + file.getNameExt() + "\n"
                            + "A file named " + file.getNameExt().replaceAll(".mesh.xml", ".material") + " should be in the same folder.\n"
                            + "Press OK to import mesh only.",
                            NotifyDescriptor.OK_CANCEL_OPTION,
                            NotifyDescriptor.WARNING_MESSAGE);
                    Object result = DialogDisplayer.getDefault().notify(msg);
                    if (!NotifyDescriptor.OK_OPTION.equals(result)) {
                        return;
                    }
                }

                FileObject file = FileUtil.toFileObject(new File(options.getDestFile()));
//                FileLock lock = null;
                try {
//                    lock = file.lock();
                    progressHandle.progress("Creating j3o file");
                    String outputPath = file.getParent().getPath() + "/" + context.getPrimaryFile().getName() + ".j3o";
                    manager.clearCache();
                    Spatial model = manager.loadModel(manager.getRelativeAssetPath(file.getPath()));
                    BinaryExporter exp = BinaryExporter.getInstance();
                    exp.save(model, new File(outputPath));
                    //cleanup
                    try {
                        FileUtil.toFileObject(new File(options.getDestFile())).delete();
                        FileUtil.toFileObject(new File(options.getDestFile().replaceAll("mesh.xml", "material"))).delete();
                    } catch (Exception e) {
                    }
                    context.getPrimaryFile().getParent().refresh();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                    Confirmation msg = new NotifyDescriptor.Confirmation(
                            "Error converting " + file.getNameExt() + "\n" + ex.toString(),
                            NotifyDescriptor.OK_CANCEL_OPTION,
                            NotifyDescriptor.ERROR_MESSAGE);
                    DialogDisplayer.getDefault().notify(msg);
                } finally {
//                    if (lock != null) {
//                        lock.releaseLock();
//                    }
                    progressHandle.finish();
                }
            }
        };

        new Thread(run).start();
    }
}
