/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.modelimporter;

import com.jme3.asset.AssetKey;
import com.jme3.asset.MaterialKey;
import com.jme3.asset.TextureKey;
import com.jme3.gde.core.assets.AssetData;
import com.jme3.gde.core.assets.BinaryModelDataObject;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.assets.SpatialAssetDataObject;
import com.jme3.gde.core.util.Beans;
import com.jme3.gde.modelimporter.UberAssetLocator.UberAssetInfo;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.SceneGraphVisitorAdapter;
import com.jme3.scene.Spatial;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.swing.JComponent;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ui.OpenProjects;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

@ActionID(
    category = "File",
id = "com.jme3.gde.modelimporter.ImportModel")
@ActionRegistration(
    iconBase = "com/jme3/gde/modelimporter/103_.png",
displayName = "#CTL_ImportModel")
@ActionReferences({
    @ActionReference(path = "Menu/File", position = 1413),
    @ActionReference(path = "Toolbars/File", position = 310)
})
@NbBundle.Messages("CTL_SomeAction=test")
@SuppressWarnings("unchecked")
public final class ImportModel implements ActionListener {

    private static final Logger logger = Logger.getLogger(ImportModel.class.getName());
    private Project context;
    private WizardDescriptor.Panel[] panels;

    public ImportModel() {
    }

    public ImportModel(Project context) {
        this.context = context;
    }

    public void actionPerformed(ActionEvent ev) {
        Project context = OpenProjects.getDefault().getMainProject();
        if (context == null) {
            context = ProjectSelection.showProjectSelection();
        }
        if (context == null) {
            return;
        }
        if (context.getLookup().lookup(ProjectAssetManager.class) == null) {
            return;
        }
        final WizardDescriptor wiz = new WizardDescriptor(getPanels());
        // {0} will be replaced by WizardDesriptor.Panel.getComponent().getName()
        wiz.setTitleFormat(new MessageFormat("{0}"));
        wiz.setTitle("Import Model to Project " + context.getLookup().lookup(ProjectInformation.class).getDisplayName());
        wiz.putProperty("project", context);
        Dialog dialog = DialogDisplayer.getDefault().createDialog(wiz);
        dialog.setVisible(true);
        dialog.toFront();
        boolean cancelled = wiz.getValue() != WizardDescriptor.FINISH_OPTION;
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
        AssetKey modelKey = (AssetKey) wiz.getProperty("mainkey");
        boolean keepFiles = (Boolean) wiz.getProperty("keepfiles");

        List<FileObject> assetList = (List<FileObject>) wiz.getProperty("assetfiles");
        List<AssetKey> assetKeys = (List<AssetKey>) wiz.getProperty("assetlist");
        String importPath = (String) wiz.getProperty("destpath");
        Project context = (Project) wiz.getProperty("project");
        ProjectAssetManager importManager = (ProjectAssetManager) wiz.getProperty("manager");
        ProjectAssetManager manager = context.getLookup().lookup(ProjectAssetManager.class);
        if (manager == null) {
            throw new IllegalStateException("Cannot find project AssetManager!");
        }

        List<FileObject> deleteList = new LinkedList<FileObject>();
        int idx = 0;
        //go through list and copy assets to project
        for (Iterator<FileObject> it = assetList.iterator(); it.hasNext();) {
            FileObject source = it.next();
            AssetKey key = assetKeys.get(idx);
            UberAssetInfo info = UberAssetLocator.getInfo(key);
            if (info != null) {
                logger.log(Level.INFO, "Found relocation info for {0}", key.getName());
                //save texture in Textures folder
                int i = 0;
                String newTexturePath = importPath + key.getName().replace(key.getFolder(), "");
                while (manager.getAssetFolder().getFileObject(newTexturePath) != null) {
                    i++;
                    newTexturePath = importPath + i + key.getName().replace(key.getFolder(), "");
                }
                newTexturePath = new AssetKey(newTexturePath).getName();
                FileObject newFile = manager.createAsset(newTexturePath, info.getFileObject());
                if (newFile == null) {
                    logger.log(Level.SEVERE, "Could not create new file {0}", newTexturePath);
                } else {
                    info.setNewAssetName(newTexturePath);
                    logger.log(Level.INFO, "Created relocated texture file {0}", newTexturePath);
                }
            } else {
                try {
                    String path = importPath + importManager.getRelativeAssetPath(source.getPath());
                    FileObject fileObj = manager.createAsset(path, source);
                    //add to delete list if not texture or j3o model
                    if (fileObj != null) {
                        logger.log(Level.INFO, "Copied file {0} to {1}", new Object[]{source.getPath(), path});
                        DataObject obj = DataObject.find(fileObj);
                        AssetData data = obj.getLookup().lookup(AssetData.class);
                        if (obj instanceof SpatialAssetDataObject) {
                            // Delete models that are not J3O.
                            if (!(obj instanceof BinaryModelDataObject)) {
                                deleteList.add(fileObj);
                                logger.log(Level.INFO, "Add file {0} to delete list", path);
                            }
                        } else if (data != null) {
                            AssetKey assetKey = data.getAssetKey();
                            if (!(assetKey instanceof TextureKey)
                                    && !(assetKey instanceof MaterialKey)) {
                                // Also delete anything thats not an image or J3M file.
                                deleteList.add(fileObj);
                                logger.log(Level.INFO, "Add file {0} to delete list", path);
                            }
                        }
                    } else {
                        logger.log(Level.SEVERE, "Error copying file {0} to {1}", new Object[]{source.getPath(), path});
                    }
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
            idx++;
        }
        //Find original model file
        FileObject newFile = manager.getAssetFolder().getFileObject(importPath + modelKey.getName());
        if (newFile == null) {
            logger.log(Level.SEVERE, "Could not find file {0} after copying to project folder!", importPath + modelKey.getName());
            return;
        }
        DataObject targetModel;
        ProjectAssetManager tempProjectManager = null;
        try {
            targetModel = DataObject.find(newFile);
            if (targetModel instanceof SpatialAssetDataObject) {
                //Load model
                tempProjectManager = targetModel.getLookup().lookup(ProjectAssetManager.class);
                if (tempProjectManager != null) {
                    logger.log(Level.INFO, "Using real ProjectAssetManager for import instatiation.");
                } else {
                    logger.log(Level.WARNING, "Using dummy ProjectAssetManager for import instantiation.");
                    tempProjectManager = new ProjectAssetManager(manager.getAssetFolder());
                    ((SpatialAssetDataObject) targetModel).getLookupContents().add(tempProjectManager);
                }
                UberAssetLocator.setAssetBaseFolder(importPath);
                //register locator with cached located assets so they can be replaced later
                tempProjectManager.registerLocator(importManager.getAssetFolderName(), UberAssetLocator.class);
                AssetData targetData = targetModel.getLookup().lookup(AssetData.class);
                targetData.setAssetKey(modelKey);
                Spatial spat = (Spatial) targetData.loadAsset();
                if (spat == null) {
                    throw new IllegalStateException("Cannot load model after copying!");
                }
                replaceLocatedTextures(spat, manager);
                targetData.saveAsset();
                ((SpatialAssetDataObject) targetModel).getLookupContents().remove(tempProjectManager);
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (tempProjectManager != null) {
                try {
                    tempProjectManager.unregisterLocator(importManager.getAssetFolderName(), UberAssetLocator.class);
                } catch (Exception e) {
                    Exceptions.printStackTrace(e);
                }
            }
            UberAssetLocator.setAssetBaseFolder(null);
        }
        //delete files if not keeping original
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

    private void replaceLocatedTextures(Spatial spat, final ProjectAssetManager mgr) {
        spat.depthFirstTraversal(new SceneGraphVisitorAdapter() {
            @Override
            public void visit(Geometry geom) {
                Material mat = geom.getMaterial();
                if (mat != null) {
                    Collection<MatParam> params = mat.getParams();
                    for (Iterator<MatParam> it = params.iterator(); it.hasNext();) {
                        MatParam matParam = it.next();
                        VarType paramType = matParam.getVarType();
                        String paramName = matParam.getName();
                        switch (paramType) {
                            case Texture2D:
                            case Texture3D:
                            case TextureArray:
                            case TextureBuffer:
                            case TextureCubeMap:
                                try {
                                    Texture tex = mat.getTextureParam(paramName).getTextureValue();
                                    AssetKey curKey = tex.getKey();
                                    UberAssetInfo newInfo = UberAssetLocator.getInfo(curKey);
                                    if (newInfo != null) {
                                        if (newInfo.getNewAssetName() != null) {
                                            logger.log(Level.INFO, "Create new key with name {0}", newInfo.getNewAssetName());
                                            TextureKey newKey = new TextureKey(newInfo.getNewAssetName());
                                            Beans.copyProperties(curKey, newKey);
                                            Texture texture = mgr.loadTexture(newKey);
                                            if (texture != null) {
                                                mat.setTextureParam(paramName, paramType, texture);
                                                geom.setMaterial(mat);
                                                logger.log(Level.INFO, "Apply relocated texture {0} for {1}", new Object[]{geom, newKey.getName()});
                                            } else {
                                                logger.log(Level.WARNING, "Could not find relocated texture!");
                                            }
                                        } else {
                                            logger.log(Level.SEVERE, "Don't have name for previously relocated asset {0}, something went wrong!", curKey);
                                        }
                                    }
                                } catch (Exception ex) {
                                    Exceptions.printStackTrace(ex);
                                }
                                break;
                            default:
                        }
                    }
                }
                super.visit(geom);
            }
        });
    }

    /**
     * Initialize panels representing individual wizard's steps and sets various
     * properties for them influencing wizard appearance.
     */
    private WizardDescriptor.Panel[] getPanels() {
        if (panels == null) {
            panels = new WizardDescriptor.Panel[]{
                new ModelImporterWizardPanel1(),
                new ModelImporterWizardPanel3(),
                new ModelImporterWizardPanel4()
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
