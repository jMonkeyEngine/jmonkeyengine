/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.ogretools;

import com.jme3.asset.AssetKey;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.assets.SpatialAssetDataObject;
import com.jme3.gde.ogretools.convert.OgreXMLConvert;
import com.jme3.gde.ogretools.convert.OgreXMLConvertOptions;
import com.jme3.scene.Spatial;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiFileLoader;
import org.openide.util.Exceptions;

public class OgreBinaryMeshDataObject extends SpatialAssetDataObject {

    public OgreBinaryMeshDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
    }

    @Override
    public Spatial loadAsset() {
        ProgressHandle handle = ProgressHandleFactory.createHandle("Converting OgreBinary");
        handle.start();
        //mesh
        OgreXMLConvertOptions options = new OgreXMLConvertOptions(getPrimaryFile().getPath());
        options.setBinaryFile(true);
        OgreXMLConvert conv = new OgreXMLConvert();
        conv.doConvert(options, handle);
        //try skeleton
        if (getPrimaryFile().existsExt("skeleton")) {
            OgreXMLConvertOptions options2 = new OgreXMLConvertOptions(getPrimaryFile().getParent().getFileObject(getPrimaryFile().getName(), "skeleton").getPath());
            options2.setBinaryFile(true);
            OgreXMLConvert conv2 = new OgreXMLConvert();
            conv2.doConvert(options2, handle);
        }
        handle.progress("Convert Model");
        ProjectAssetManager mgr = getLookup().lookup(ProjectAssetManager.class);
        if (mgr == null) {
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("File is not part of a project!\nCannot load without ProjectAssetManager."));
            return null;
        }
        String assetKey = mgr.getRelativeAssetPath(options.getDestFile());
        FileLock lock = null;
        try {
            lock = getPrimaryFile().lock();
            listListener.start();
            Spatial spatial = mgr.loadModel(assetKey);
            //replace transient xml files in list of assets for this model
            replaceXmlFiles(mgr);
            listListener.stop();
            savable = spatial;
            lock.releaseLock();
            File deleteFile = new File(options.getDestFile());
            deleteFile.delete();
            handle.finish();
            return spatial;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            if (lock != null) {
                lock.releaseLock();
            }
        }
        File deleteFile = new File(options.getDestFile());
        deleteFile.delete();
        handle.finish();
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
