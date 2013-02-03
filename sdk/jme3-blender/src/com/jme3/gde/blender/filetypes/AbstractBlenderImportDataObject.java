/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.blender.filetypes;

import com.jme3.asset.BlenderKey;
import com.jme3.gde.blender.BlenderTool;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.assets.SpatialAssetDataObject;
import com.jme3.gde.core.util.Beans;
import com.jme3.gde.core.util.SpatialUtil;
import com.jme3.scene.Spatial;
import java.io.IOException;
import java.util.logging.Level;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiFileLoader;
import org.openide.util.Exceptions;

/**
 *
 * @author normenhansen
 */
public abstract class AbstractBlenderImportDataObject extends SpatialAssetDataObject {

    protected String SUFFIX;

    public AbstractBlenderImportDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
    }

    @Override
    public Spatial loadAsset() {
        if (savable != null) {
            return (Spatial) savable;
        }
        if (SUFFIX == null) {
            throw new IllegalStateException("Suffix for blender filetype is null! Set SUFFIX = \"sfx\" in constructor!");
        }
        ProjectAssetManager mgr = getLookup().lookup(ProjectAssetManager.class);
        if (mgr == null) {
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("File is not part of a project!\nCannot load without ProjectAssetManager."));
            return null;
        }
        //make sure its actually closed and all data gets reloaded
        closeAsset();
        FileObject mainFile = getPrimaryFile();
        if (!BlenderTool.runConversionScript(SUFFIX, mainFile)) {
            logger.log(Level.SEVERE, "Failed to create model, running blender caused an error");
            return null;
        }
        mainFile.getParent().refresh();
        FileObject outFile = FileUtil.findBrother(mainFile, BlenderTool.TEMP_SUFFIX);
        if (outFile == null) {
            logger.log(Level.SEVERE, "Failed to create model, blend file cannot be found");
            return null;
        }
        int i = 1;
        FileObject blend1File = FileUtil.findBrother(mainFile, BlenderTool.TEMP_SUFFIX + i);
        while (blend1File != null) {
            try {
                blend1File.delete();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
            i++;
            blend1File = FileUtil.findBrother(mainFile, BlenderTool.TEMP_SUFFIX + i);
        }
        String assetKeyName = mgr.getRelativeAssetPath(outFile.getPath());
        BlenderKey key = new BlenderKey(assetKeyName);
        Beans.copyProperties(key, getAssetKey());
        try {
            listListener.start();
            Spatial spatial = mgr.loadModel(key);
            replaceFiles();
            listListener.stop();
            SpatialUtil.storeOriginalPathUserData(spatial);
            savable = spatial;
            logger.log(Level.INFO, "Loaded asset {0}", getName());
            return spatial;
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            try {
                outFile.delete();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return null;
    }

    @Override
    public synchronized BlenderKey getAssetKey() {
        if (super.getAssetKey() instanceof BlenderKey) {
            return (BlenderKey) assetKey;
        }
        assetKey = new BlenderKey(super.getAssetKey().getName());
        return (BlenderKey) assetKey;
    }

    protected void replaceFiles() {
        for (int i = 0; i < assetList.size(); i++) {
            FileObject fileObject = assetList.get(i);
            if (fileObject.hasExt(BlenderTool.TEMP_SUFFIX)) {
                assetList.remove(i);
                assetKeyList.remove(i);
                assetList.add(i, getPrimaryFile());
                assetKeyList.add(getAssetKey());
                return;
            }
        }
    }
}
