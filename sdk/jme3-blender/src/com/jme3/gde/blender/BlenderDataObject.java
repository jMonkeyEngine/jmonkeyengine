/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.blender;

import com.jme3.asset.BlenderKey;
import com.jme3.asset.ModelKey;
import com.jme3.gde.core.assets.SpatialAssetDataObject;
import java.io.IOException;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiFileLoader;

public class BlenderDataObject extends SpatialAssetDataObject {

    public BlenderDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
    }

    @Override
    public ModelKey getAssetKey() {
        return new BlenderKey(super.getAssetKey().getName());
    }
    
}
