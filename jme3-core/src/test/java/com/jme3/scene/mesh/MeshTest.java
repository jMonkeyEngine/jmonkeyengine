package com.jme3.scene.mesh;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.jme3.scene.Mesh;

public class MeshTest {
       @Test
    public void testEmptyMesh() {
        final Mesh mesh = new Mesh();

        assertEquals(-1, mesh.getVertexCount());;
    }
}
