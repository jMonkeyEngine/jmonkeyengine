package com.jme3.vulkan.material.experimental;

import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.EngineBuffer;
import com.jme3.vulkan.mesh.ExperimentalCubeMesh;
import com.jme3.vulkan.scene.Scene;

/**
 * Renders simple PBR materials using flat color, metallic, and roughness, and a normal texture.
 */
public class PBRTechnique {

    private EngineBuffer instances;

    public void renderScene(Scene.Subset geometries) {
        // each batched geometry (an "instance") requires pointers to mesh and material data.
        // the first thing to do is determine which geometries should be batched together.
        // for now we'll assume all nodes in the subset are geometric and are batchable together.
        DataBuffer inst = instances.cache();
        for (int g : geometries) {
            ExperimentalCubeMesh mesh; // somehow extract mesh from geometry
            inst.put(mesh.getVertexArrayAddress()); // first long is the vertex buffer
            inst.put(mesh.getIndicesArrayAddress()); // second long is the index buffer
        }
    }

}
