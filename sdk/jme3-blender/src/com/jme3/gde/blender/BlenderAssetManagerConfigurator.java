/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.blender;

import com.jme3.asset.AssetManager;
import com.jme3.gde.core.assets.AssetManagerConfigurator;

/**
 *
 * @author normenhansen
 */
@org.openide.util.lookup.ServiceProvider(service = AssetManagerConfigurator.class)
public class BlenderAssetManagerConfigurator implements AssetManagerConfigurator {

    public void prepareManager(AssetManager manager) {
        manager.registerLoader(com.jme3.scene.plugins.blender.BlenderModelLoader.class, "blend");
        manager.registerLoader(com.jme3.scene.plugins.blender.BlenderModelLoader.class, BlenderTool.TEMP_SUFFIX);
    }
}
