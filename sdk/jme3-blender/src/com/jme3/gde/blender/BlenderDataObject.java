/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.blender;

import com.jme3.asset.BlenderKey;
import com.jme3.asset.ModelKey;
import com.jme3.gde.core.assets.SpatialAssetDataObject;
import java.io.IOException;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiFileLoader;
import org.openide.util.NbBundle.Messages;

@Messages({
    "LBL_Blender_LOADER=Blender Files"
})
@MIMEResolver.ExtensionRegistration(
    displayName="#LBL_Blender_LOADER",
    mimeType="application/blender",
    extension={ "blend" }
)
@DataObject.Registration(
    mimeType = "application/blender", 
    iconBase = "com/jme3/gde/blender/blender.png",
    displayName="#LBL_Blender_LOADER",
    position=300
)
@ActionReferences(value = {
    @ActionReference(id =
    @ActionID(category = "jMonkeyPlatform", id = "com.jme3.gde.core.assets.actions.ConvertModel"), path = "Loaders/application/blender/Actions", position = 10),
    @ActionReference(id =
    @ActionID(category = "jMonkeyPlatform", id = "com.jme3.gde.core.assets.actions.OpenModel"), path = "Loaders/application/blender/Actions", position = 20),
    @ActionReference(id =
    @ActionID(category = "Edit", id = "org.openide.actions.CutAction"), path = "Loaders/application/blender/Actions", position = 200, separatorBefore = 100),
    @ActionReference(id =
    @ActionID(category = "Edit", id = "org.openide.actions.CopyAction"), path = "Loaders/application/blender/Actions", position = 300, separatorAfter = 400),
    @ActionReference(id =
    @ActionID(category = "Edit", id = "org.openide.actions.DeleteAction"), path = "Loaders/application/blender/Actions", position = 500),
    @ActionReference(id =
    @ActionID(category = "System", id = "org.openide.actions.RenameAction"), path = "Loaders/application/blender/Actions", position = 600, separatorAfter = 700),
    @ActionReference(id =
    @ActionID(category = "System", id = "org.openide.actions.SaveAsTemplateAction"), path = "Loaders/application/blender/Actions", position = 800, separatorAfter = 900),
    @ActionReference(id =
    @ActionID(category = "System", id = "org.openide.actions.FileSystemAction"), path = "Loaders/application/blender/Actions", position = 1000, separatorAfter = 1100),
    @ActionReference(id =
    @ActionID(category = "System", id = "org.openide.actions.ToolsAction"), path = "Loaders/application/blender/Actions", position = 1200),
    @ActionReference(id =
    @ActionID(category = "System", id = "org.openide.actions.PropertiesAction"), path = "Loaders/application/blender/Actions", position = 1300)
})
public class BlenderDataObject extends SpatialAssetDataObject {

    public BlenderDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
    }

    @Override
    public ModelKey getAssetKey() {
        if(super.getAssetKey() instanceof BlenderKey){
            return (BlenderKey)assetKey;
        }
        assetKey = new BlenderKey(super.getAssetKey().getName());
        return (BlenderKey)assetKey;
    }
    
}
