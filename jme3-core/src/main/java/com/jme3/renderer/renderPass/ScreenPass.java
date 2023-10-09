package com.jme3.renderer.renderPass;

import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.material.Material;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.shader.plugins.GLSLLoader;
import com.jme3.system.JmeSystem;
import com.jme3.ui.Picture;

public abstract class ScreenPass extends ForwardPass{
    protected static AssetManager assetManager;
    protected Material screenMat;
    protected Picture screenRect;
    public ScreenPass(String name, RenderQueue.Bucket bucket) {
        super(name, bucket);
        initAssetManager();
        init();
    }

    public abstract void init();

    private static void initAssetManager(){
        if(assetManager == null){
            assetManager = JmeSystem.newAssetManager();
            assetManager.registerLocator(".", FileLocator.class);
            assetManager.registerLocator("/", ClasspathLocator.class);
            assetManager.registerLoader(J3MLoader.class, "j3m");
            assetManager.registerLoader(J3MLoader.class, "j3md");
            assetManager.registerLoader(GLSLLoader.class, "vert", "frag","geom","tsctrl","tseval","glsllib","glsl");
        }
    }
}
