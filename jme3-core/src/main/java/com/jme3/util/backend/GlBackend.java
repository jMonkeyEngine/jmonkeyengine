package com.jme3.util.backend;

import com.jme3.asset.AssetManager;
import com.jme3.material.GlMaterial;
import com.jme3.material.Material;
import com.jme3.scene.GlMesh;
import com.jme3.scene.Mesh;

public class GlBackend implements Backend {

    private final AssetManager assetManager;

    public GlBackend(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    @Override
    public Material createMaterial(String assetName) {
        return assetManager.loadMaterial(assetName);
    }

    @Override
    public Material createMaterialFromMatDef(String matdef) {
        return new GlMaterial(assetManager, matdef);
    }

    @Override
    public Mesh createBlankMesh() {
        return new GlMesh();
    }

    @Override
    public Mesh migrateMesh(GlMesh mesh) {
        return mesh;
    }

}
