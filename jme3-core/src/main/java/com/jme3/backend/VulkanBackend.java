package com.jme3.backend;

import com.jme3.asset.AssetManager;
import com.jme3.dev.NotFullyImplemented;
import com.jme3.material.Material;
import com.jme3.scene.Mesh;

public class VulkanBackend implements Backend {

    @Override
    @NotFullyImplemented
    public Material createMaterial(AssetManager assetManager, String assetName) {
        return null;
    }

    @Override
    @NotFullyImplemented
    public Material createBlankMaterial(AssetManager assetManager, String matdef) {
        return null;
    }

    @Override
    @NotFullyImplemented
    public Mesh createBlankMesh() {
        return null;
    }

}
