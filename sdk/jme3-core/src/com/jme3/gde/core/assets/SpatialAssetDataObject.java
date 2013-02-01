/*
 *  Copyright (c) 2009-2010 jMonkeyEngine
 *  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.core.assets;

import com.jme3.asset.AssetKey;
import com.jme3.asset.ModelKey;
import com.jme3.scene.Geometry;
import com.jme3.scene.SceneGraphVisitorAdapter;
import com.jme3.scene.Spatial;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiFileLoader;
import org.openide.util.Exceptions;

/**
 *
 * @author normenhansen
 */
public class SpatialAssetDataObject extends AssetDataObject {

    public SpatialAssetDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
        saveExtension = "j3o";
//        CookieSet cookies = getCookieSet();
//        cookies.add((Node.Cookie) new SpatialAssetOpenSupport(this));
//        cookies.assign(OpenCookie.class, new SpatialAssetOpenSupport(this));
//        cookies.assign(CloseCookie.class, new SpatialAssetCloseSupport(this));
    }

    @Override
    public synchronized ModelKey getAssetKey() {
        AssetKey superKey = super.getAssetKey();
        if (superKey instanceof ModelKey) {
            return (ModelKey) superKey;
        } else {
            ProjectAssetManager mgr = getLookup().lookup(ProjectAssetManager.class);
            if (mgr == null) {
                return null;
            }
            String assetKey = mgr.getRelativeAssetPath(getPrimaryFile().getPath());
            return new ModelKey(assetKey);
        }
    }

    @Override
    public synchronized Spatial loadAsset() {
        if (isModified() && savable != null) {
            return (Spatial) savable;
        }
        ProjectAssetManager mgr = getLookup().lookup(ProjectAssetManager.class);
        if (mgr == null) {
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("File is not part of a project!\nCannot load without ProjectAssetManager."));
            return null;
        }
        FileLock lock = null;
        try {
            lock = getPrimaryFile().lock();
            mgr.deleteFromCache(getAssetKey());
            listListener.start();
            Spatial spatial = mgr.loadModel(getAssetKey());
            listListener.stop();
            savable = spatial;
            if (!(this instanceof BinaryModelDataObject)) {
                storeOriginalPathUserData();
            }
            lock.releaseLock();
            return spatial;
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (lock != null) {
                lock.releaseLock();
            }
        }
        return null;
    }

    @Override
    public synchronized void saveAsset() throws IOException {
        super.saveAsset();
        ProjectAssetManager mgr = getLookup().lookup(ProjectAssetManager.class);
        if (mgr == null) {
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("File is not part of a project!\nCannot load without ProjectAssetManager."));
            return;
        }
        FileObject outFile = null;
        if (saveExtension == null) {
            outFile = getPrimaryFile();
        } else {
            outFile = getPrimaryFile().getParent().getFileObject(getPrimaryFile().getName(), saveExtension);
            if (outFile == null) {
                Logger.getLogger(SpatialAssetDataObject.class.getName()).log(Level.SEVERE, "Could not locate saved file.");
                return;
            }
        }
        try {
            DataObject targetModel = DataObject.find(outFile);
            AssetData properties = targetModel.getLookup().lookup(AssetData.class);
            if (properties != null) {
                if (properties.getProperty("ORIGINAL_PATH") == null) {
                    properties.setProperty("ORIGINAL_PATH", mgr.getRelativeAssetPath(outFile.getPath()));
                }
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    /**
     * Stores ORIGINAL_NAME and ORIGINAL_PATH UserData to all Geometry in
     * loaded spatial
     */
    protected void storeOriginalPathUserData() {
        final ArrayList<String> geomMap = new ArrayList<String>();
        Spatial spat = (Spatial) savable;
        if (spat != null) {
            spat.depthFirstTraversal(new SceneGraphVisitorAdapter() {
                @Override
                public void visit(Geometry geom) {
                    StringBuilder geometryIdentifier = new StringBuilder();
                    Spatial curSpat = geom;
                    String geomName = curSpat.getName();
                    if (geomName == null) {
                        logger.log(Level.WARNING, "Null geometry name!");
                        geomName = "null";
                    }
                    geom.setUserData("ORIGINAL_NAME", geomName);
                    logger.log(Level.FINE, "Set ORIGINAL_NAME for {0}", geomName);
                    while (curSpat != null) {
                        String name = curSpat.getName();
                        if (name == null) {
                            logger.log(Level.WARNING, "Null spatial name!");
                            name = "null";
                        }
                        geometryIdentifier.insert(0, name);
                        geometryIdentifier.insert(0, '/');
                        curSpat = curSpat.getParent();
                    }
                    String id = geometryIdentifier.toString();
                    if (geomMap.contains(id)) {
                        logger.log(Level.WARNING, "Cannot create unique name for Geometry {0}: {1}", new Object[]{geom, id});
                    }
                    geomMap.add(id);
                    geom.setUserData("ORIGINAL_PATH", id);
                    logger.log(Level.FINE, "Set ORIGINAL_PATH for {0}", id);
                    super.visit(geom);
                }
            });
        } else {
            logger.log(Level.SEVERE, "No geometry available when trying to scan initial geometry configuration");
        }
    }
}
