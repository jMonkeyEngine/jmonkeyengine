/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.wavefront;

import com.jme3.gde.core.assets.AssetDataObject;
import java.io.IOException;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiFileLoader;

@MIMEResolver.ExtensionRegistration(
        displayName = "Wavefront OBJ Material",
        mimeType = "text/x-wavefrontmtl",
        extension = {"mtl", "MTL"}
)
@DataObject.Registration(displayName = "Wavefront OBJ Material", mimeType = "text/x-wavefrontmtl", iconBase="com/jme3/gde/wavefront/Computer_File_083.gif")
public class WaveFrontMaterialDataObject extends AssetDataObject {

    public WaveFrontMaterialDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
    }

}
