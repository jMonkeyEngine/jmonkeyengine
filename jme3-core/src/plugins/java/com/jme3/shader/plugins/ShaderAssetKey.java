package com.jme3.shader.plugins;

import com.jme3.asset.AssetKey;

/**
 * Created by Nehon on 28/10/2017.
 */
public class ShaderAssetKey extends AssetKey {

    private boolean injectDependencies = false;

    public ShaderAssetKey(String name, boolean injectDependencies) {
        super(name);
        this.injectDependencies = injectDependencies;
    }

    public boolean isInjectDependencies() {
        return injectDependencies;
    }

    public void setInjectDependencies(boolean injectDependencies) {
        this.injectDependencies = injectDependencies;
    }
}
