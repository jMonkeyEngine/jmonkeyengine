/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.ogretools;

import com.jme3.asset.AssetKey;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.assets.SpatialAssetDataObject;
import com.jme3.gde.core.util.SpatialUtil;
import com.jme3.gde.ogretools.convert.OgreXMLConvert;
import com.jme3.gde.ogretools.convert.OgreXMLConvertOptions;
import com.jme3.scene.Spatial;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiFileLoader;
import org.openide.util.Exceptions;

@MIMEResolver.ExtensionRegistration(
    displayName="Ogre Binary Mesh",
    mimeType="application/ogrebinarymesh",
    extension={ "mesh" }
)
@DataObject.Registration(displayName = "Ogre Binary Mesh", mimeType = "application/ogrebinarymesh", iconBase = "com/jme3/gde/ogretools/ogre-logo.png")
@ActionReferences(value = {
    @ActionReference(id =
    @ActionID(category = "jMonkeyPlatform", id = "com.jme3.gde.core.assets.actions.ConvertModel"), path = "Loaders/application/ogrebinarymesh/Actions", position = 10),
    @ActionReference(id =
    @ActionID(category = "jMonkeyPlatform", id = "com.jme3.gde.core.assets.actions.OpenModel"), path = "Loaders/application/ogrebinarymesh/Actions", position = 20),
    @ActionReference(id =
    @ActionID(category = "Edit", id = "org.openide.actions.CutAction"), path = "Loaders/application/ogrebinarymesh/Actions", position = 200, separatorBefore = 100),
    @ActionReference(id =
    @ActionID(category = "Edit", id = "org.openide.actions.CopyAction"), path = "Loaders/application/ogrebinarymesh/Actions", position = 300, separatorAfter = 400),
    @ActionReference(id =
    @ActionID(category = "Edit", id = "org.openide.actions.DeleteAction"), path = "Loaders/application/ogrebinarymesh/Actions", position = 500),
    @ActionReference(id =
    @ActionID(category = "System", id = "org.openide.actions.RenameAction"), path = "Loaders/application/ogrebinarymesh/Actions", position = 600, separatorAfter = 700),
    @ActionReference(id =
    @ActionID(category = "System", id = "org.openide.actions.SaveAsTemplateAction"), path = "Loaders/application/ogrebinarymesh/Actions", position = 800, separatorAfter = 900),
    @ActionReference(id =
    @ActionID(category = "System", id = "org.openide.actions.FileSystemAction"), path = "Loaders/application/ogrebinarymesh/Actions", position = 1000, separatorAfter = 1100),
    @ActionReference(id =
    @ActionID(category = "System", id = "org.openide.actions.ToolsAction"), path = "Loaders/application/ogrebinarymesh/Actions", position = 1200),
    @ActionReference(id =
    @ActionID(category = "System", id = "org.openide.actions.PropertiesAction"), path = "Loaders/application/ogrebinarymesh/Actions", position = 1300)
})
public class OgreBinaryMeshDataObject extends SpatialAssetDataObject {

    public OgreBinaryMeshDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
    }

    @Override
    public Spatial loadAsset() {
        if (savable != null) {
            return (Spatial) savable;
        }
        ProjectAssetManager mgr = getLookup().lookup(ProjectAssetManager.class);
        if (mgr == null) {
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("File is not part of a project!\nCannot load without ProjectAssetManager."));
            return null;
        }
        //make sure its actually closed and all data gets reloaded
        closeAsset();
        //mesh
        OgreXMLConvertOptions options = new OgreXMLConvertOptions(getPrimaryFile().getPath());
        options.setBinaryFile(true);
        OgreXMLConvert conv = new OgreXMLConvert();
        conv.doConvert(options, null);
        //try skeleton
        if (getPrimaryFile().existsExt("skeleton")) {
            OgreXMLConvertOptions options2 = new OgreXMLConvertOptions(getPrimaryFile().getParent().getFileObject(getPrimaryFile().getName(), "skeleton").getPath());
            options2.setBinaryFile(true);
            OgreXMLConvert conv2 = new OgreXMLConvert();
            conv2.doConvert(options2, null);
        }
        String assetKey = mgr.getRelativeAssetPath(options.getDestFile());
        try {
            listListener.start();
            Spatial spatial = mgr.loadModel(assetKey);
            //replace transient xml files in list of assets for this model
            replaceXmlFiles(mgr);
            listListener.stop();
            SpatialUtil.storeOriginalPathUserData(spatial);
            File deleteFile = new File(options.getDestFile());
            deleteFile.delete();
            savable = spatial;
            logger.log(Level.INFO, "Loaded asset {0}", getName());
            return spatial;
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        File deleteFile = new File(options.getDestFile());
        deleteFile.delete();
        return null;
    }

    private void replaceXmlFiles(ProjectAssetManager mgr) {
        for (int i = 0; i < assetList.size(); i++) {
            FileObject fileObject = assetList.get(i);
            if (fileObject.hasExt("xml")) {
                FileObject binaryFile = fileObject.getParent().getFileObject(fileObject.getName());
                if (binaryFile != null) {
                    assetList.remove(i);
                    assetList.add(i, binaryFile);
                    assetKeyList.remove(i);
                    assetKeyList.add(i, new AssetKey(mgr.getRelativeAssetPath(binaryFile.getPath())));
                }
            }
        }
    }
}
