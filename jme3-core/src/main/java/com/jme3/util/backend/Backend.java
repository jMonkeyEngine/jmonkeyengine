package com.jme3.util.backend;

import com.jme3.material.Material;
import com.jme3.scene.GlMesh;
import com.jme3.scene.Mesh;

/**
 * Creates rendering elements for the specific backend being used.
 */
public interface Backend {

    Material createMaterial(String assetName);

    Material createMaterialFromMatDef(String matdef);

    Mesh createBlankMesh();

    /**
     * Migrates the OpenGL mesh to this backend. Many meshes
     *
     * @param mesh
     * @return
     */
    Mesh migrateMesh(GlMesh mesh);

}
