package com.jme3.compat;

import com.jme3.material.Material;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Box;

public interface ComponentFactory {

    Material createMaterial(String material);

    Material createBlankMaterial(String matdef);

    Mesh createBlankMesh();

    Mesh migrateMesh(Mesh mesh);

    default Mesh createBoxMesh(float x, float y, float z) {
        return migrateMesh(new Box(x, y, z));
    }

}
