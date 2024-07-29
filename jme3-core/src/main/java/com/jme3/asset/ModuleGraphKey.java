/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.asset;

import com.jme3.asset.cache.AssetCache;
import com.jme3.renderer.framegraph.export.ModuleGraphData;

/**
 *
 * @author codex
 */
public class ModuleGraphKey extends AssetKey<ModuleGraphData> {
    
    public ModuleGraphKey(String name) {
        super(name);
    }
    public ModuleGraphKey() {
        super();
    }
    
    @Override
    public Class<? extends AssetCache> getCacheType() {
        return null;
    }
    
}
