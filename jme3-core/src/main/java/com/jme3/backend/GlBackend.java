package com.jme3.backend;

import com.jme3.asset.AssetManager;
import com.jme3.material.GlMaterial;
import com.jme3.material.Material;
import com.jme3.scene.GlMesh;
import com.jme3.scene.Mesh;

public class GlBackend implements Backend {

    @Override
    public Material createBlankMaterial(AssetManager assetManager, String matdef) {
        return new GlMaterial(assetManager, matdef);
    }

    @Override
    public Mesh createBlankMesh() {
        return new GlMesh();
    }

}
