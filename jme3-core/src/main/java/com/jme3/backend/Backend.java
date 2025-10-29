package com.jme3.backend;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.scene.Mesh;

import java.util.Objects;

/**
 * Creates rendering elements for the specific backend being used.
 */
public interface Backend {

    Material createBlankMaterial(AssetManager assetManager, String matdef);

    Mesh createBlankMesh();

    static Material material(AssetManager assetManager, String matdef) {
        return get().createBlankMaterial(assetManager, matdef);
    }

    static Mesh mesh() {
        return get().createBlankMesh();
    }

    static void set(Backend instance) {
        Manager.instance = Objects.requireNonNull(instance);
    }

    static Backend get() {
        if (Manager.instance == null) {
            throw new IllegalStateException("Backend not set.");
        }
        return Manager.instance;
    }

    class Manager {

        private static Backend instance;

    }

}
