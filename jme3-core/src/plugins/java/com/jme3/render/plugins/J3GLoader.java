/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.render.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeImporter;
import com.jme3.export.Savable;
import java.io.IOException;

/**
 *
 * @author codex
 */
public class J3GLoader implements AssetLoader, JmeImporter {
    
    public static final int CURRENT_VERSION = 0;
    
    private AssetManager assetManager;
    
    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        
    }
    @Override
    public InputCapsule getCapsule(Savable id) {}
    @Override
    public AssetManager getAssetManager() {
        return assetManager;
    }
    @Override
    public int getFormatVersion() {}
    
}
