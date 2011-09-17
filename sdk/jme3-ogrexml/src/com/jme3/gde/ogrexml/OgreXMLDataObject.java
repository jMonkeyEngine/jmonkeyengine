/*
 * Copyright (c) 2009-2010 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.ogrexml;

import com.jme3.asset.ModelKey;
import com.jme3.gde.core.assets.AssetData;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.assets.SpatialAssetDataObject;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.ogre.OgreMeshKey;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.NotifyDescriptor.Confirmation;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiFileLoader;
import org.openide.util.Exceptions;

public class OgreXMLDataObject extends SpatialAssetDataObject {

    public OgreXMLDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
    }

    @Override
    public ModelKey getAssetKey() {
        if(super.getAssetKey() instanceof OgreMeshKey){
            return (OgreMeshKey)assetKey;
        }
        assetKey = new OgreMeshKey(super.getAssetKey().getName());
        return (OgreMeshKey)assetKey;
    }
    
    @Override
    public Spatial loadAsset() {
        if (isModified() && savable != null) {
            return (Spatial) savable;
        }
        ProjectAssetManager mgr = getLookup().lookup(ProjectAssetManager.class);
        if (mgr == null) {
            return null;
        }
        String name = getPrimaryFile().getName();
        int idx = name.toLowerCase().indexOf(".mesh");
        if(idx!=-1){
            name = name.substring(0, idx);
        }
        FileObject sourceMatFile = getPrimaryFile().getParent().getFileObject(name, "material");
        if (sourceMatFile != null && sourceMatFile.isValid()) {
            try {
                sourceMatFile.copy(sourceMatFile.getParent(), "+" + sourceMatFile.getName(), sourceMatFile.getExt());
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        } else {
            Confirmation msg = new NotifyDescriptor.Confirmation(
                    "No material file found for " + getPrimaryFile().getNameExt() + "\n"
                    + "A file named " + name + ".material should be in the same folder.\n"
                    + "Press OK to import mesh only.",
                    NotifyDescriptor.OK_CANCEL_OPTION,
                    NotifyDescriptor.WARNING_MESSAGE);
            Object result = DialogDisplayer.getDefault().notify(msg);
            if (!NotifyDescriptor.OK_OPTION.equals(result)) {
                return null;
            }
        }
        
        FileLock lock = null;
        try {
            lock = getPrimaryFile().lock();
            mgr.deleteFromCache(getAssetKey());
            Spatial spatial = mgr.loadModel(getAssetKey());
            savable = spatial;
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

    public void saveAsset() throws IOException {
        super.saveAsset();
        ProjectAssetManager mgr = getLookup().lookup(ProjectAssetManager.class);
        if (mgr == null) {
            return;
        }
        FileObject outFile = null;
        if (saveExtension == null) {
            outFile = getPrimaryFile();
        } else {
            String name = getPrimaryFile().getName();
            int idx = name.toLowerCase().indexOf(".mesh");
            if(idx!=-1){
                name = name.substring(0, idx);
            }
            outFile = getPrimaryFile().getParent().getFileObject(name, saveExtension);
            if (outFile == null) {
                Logger.getLogger(SpatialAssetDataObject.class.getName()).log(Level.SEVERE, "Could not locate saved file.");
                return;
            }
        }
        try {
            DataObject targetModel = DataObject.find(outFile);
            AssetData properties = targetModel.getLookup().lookup(AssetData.class);
            if (properties != null) {
                properties.loadProperties();
                properties.setProperty("ORIGINAL_PATH", mgr.getRelativeAssetPath(outFile.getPath()));
                properties.saveProperties();
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
