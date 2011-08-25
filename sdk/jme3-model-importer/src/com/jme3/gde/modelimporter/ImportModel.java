/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.modelimporter;

import com.jme3.asset.AssetKey;
import com.jme3.asset.TextureKey;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.gde.core.assets.AssetData;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.assets.SpatialAssetDataObject;
import com.jme3.scene.Spatial;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JComponent;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;

@SuppressWarnings("unchecked")
public final class ImportModel implements ActionListener {

    private final Project context;
    private WizardDescriptor.Panel[] panels;

    public ImportModel(Project context) {
        this.context = context;
    }

    public void actionPerformed(ActionEvent ev) {
        final WizardDescriptor wiz = new WizardDescriptor(getPanels());
        // {0} will be replaced by WizardDesriptor.Panel.getComponent().getName()
        wiz.setTitleFormat(new MessageFormat("{0}"));
        wiz.setTitle("Import Model to Project");
        wiz.putProperty("project", context);
        Dialog dialog = DialogDisplayer.getDefault().createDialog(wiz);
        dialog.setVisible(true);
        dialog.toFront();
        boolean cancelled = wiz.getValue() != WizardDescriptor.FINISH_OPTION;
        ((ModelImporterWizardPanel1) panels[0]).cleanup();
        if (!cancelled) {
            new Thread(new Runnable() {

                public void run() {
                    ProgressHandle handle = ProgressHandleFactory.createHandle("Importing Model..");
                    handle.start();
                    try {
                        copyModel(wiz);
                    } catch (Exception e) {
                        Exceptions.printStackTrace(e);
                    }
                    handle.finish();
                }
            }).start();
        }
    }

    private void copyModel(WizardDescriptor wiz) {
        AssetKey key = (AssetKey) wiz.getProperty("mainkey");
        boolean keepFiles = (Boolean) wiz.getProperty("keepfiles");
        List<AssetKey> keyList = (List<AssetKey>) wiz.getProperty("assetlist");
        String path = (String) wiz.getProperty("path");
        String importPath = (String) wiz.getProperty("destpath");
        ProjectAssetManager manager = context.getLookup().lookup(ProjectAssetManager.class);
        if (manager == null) {
            throw new IllegalStateException("Cannot find project AssetManager!");
        }
        List<FileObject> deleteList = new LinkedList<FileObject>();
        for (Iterator<AssetKey> it = keyList.iterator(); it.hasNext();) {
            AssetKey assetKey = it.next();
            File file = new File(path + "/" + assetKey.getFolder() + assetKey.getName());
            if (file.exists()) {
                FileObject source = FileUtil.toFileObject(file);
                File destFolder = new File(manager.getAssetFolderName() + "/" + importPath + "/" + assetKey.getFolder() + "/");
                destFolder.mkdirs();
                FileObject dest = FileUtil.toFileObject(destFolder);
                try {
                    FileObject fileObj = source.copy(dest, source.getName(), source.getExt());
                    if (!(assetKey instanceof TextureKey)) {
                        deleteList.add(fileObj);
                    }
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
        File file = new File(manager.getAssetFolderName() + "/" + importPath + "/" + key.getName());
//        File outFile = new File(manager.getAssetFolderName() + "/" + importPath + "/" + key.getName().replaceAll(key.getExtension(), "j3o"));
        DataObject targetModel;
        try {
            targetModel = DataObject.find(FileUtil.toFileObject(file));
            if (targetModel instanceof SpatialAssetDataObject) {
                //TODO: wtf? why do i have to add the assetmanager?
                ((SpatialAssetDataObject) targetModel).getLookupContents().add(manager);
                AssetData data = targetModel.getLookup().lookup(AssetData.class);
                data.setAssetKey(key);
                Spatial spat = (Spatial) data.loadAsset();
                if (spat == null) {
                    throw new IllegalStateException("Cannot load model after copying!");

                }
                data.saveAsset();
//                BinaryExporter exp = BinaryExporter.getInstance();
//                exp.save(spat, outFile);
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        if (!keepFiles) {
            for (Iterator<FileObject> it = deleteList.iterator(); it.hasNext();) {
                FileObject fileObject = it.next();
                try {
                    fileObject.delete();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
        FileObject importFolder = manager.getAssetFolder().getFileObject(importPath);//FileUtil.toFileObject(new File(manager.getAssetFolderName() + "/" + importPath));
        FileObject importParentFolder = importFolder.getParent();
        importParentFolder.refresh();
        importFolder.refresh();
    }

    /**
     * Initialize panels representing individual wizard's steps and sets
     * various properties for them influencing wizard appearance.
     */
    private WizardDescriptor.Panel[] getPanels() {
        if (panels == null) {
            panels = new WizardDescriptor.Panel[]{
                new ModelImporterWizardPanel1(),
                new ModelImporterWizardPanel2()
            };
            String[] steps = new String[panels.length];
            for (int i = 0; i < panels.length; i++) {
                Component c = panels[i].getComponent();
                // Default step name to component name of panel. Mainly useful
                // for getting the name of the target chooser to appear in the
                // list of steps.
                steps[i] = c.getName();
                if (c instanceof JComponent) { // assume Swing components
                    JComponent jc = (JComponent) c;
                    // Sets step number of a component
                    // TODO if using org.openide.dialogs >= 7.8, can use WizardDescriptor.PROP_*:
                    jc.putClientProperty("WizardPanel_contentSelectedIndex", new Integer(i));
                    // Sets steps names for a panel
                    jc.putClientProperty("WizardPanel_contentData", steps);
                    // Turn on subtitle creation on each step
                    jc.putClientProperty("WizardPanel_autoWizardStyle", Boolean.TRUE);
                    // Show steps on the left side with the image on the background
                    jc.putClientProperty("WizardPanel_contentDisplayed", Boolean.TRUE);
                    // Turn on numbering of all steps
                    jc.putClientProperty("WizardPanel_contentNumbered", Boolean.TRUE);
                }
            }
        }
        return panels;
    }
}
