/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.pass;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.renderer.framegraph.MyFrameGraph;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.ui.Picture;

/**
 *
 * @author codex
 */
public abstract class ScreenModule extends ForwardModule {
    
    protected final AssetManager assetManager;
    protected Material screenMat;
    protected Picture screenRect;
    
    public ScreenModule(AssetManager assetManager, RenderQueue.Bucket bucket) {
        super(bucket);
        this.assetManager = assetManager;
    }
    
    @Override
    public abstract void initialize(MyFrameGraph frameGraph);
    
}
