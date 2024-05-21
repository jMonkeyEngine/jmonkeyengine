/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.asset;

import com.jme3.asset.cache.AssetCache;
import com.jme3.renderer.framegraph.FrameGraphData;

/**
 *
 * @author codex
 */
public class FrameGraphKey extends AssetKey<FrameGraphData> {
    
    public FrameGraphKey(String name) {
        super(name);
    }
    public FrameGraphKey() {
        super();
    }
    
    @Override
    public Class<? extends AssetCache> getCacheType() {
        return null;
    }
    
}
