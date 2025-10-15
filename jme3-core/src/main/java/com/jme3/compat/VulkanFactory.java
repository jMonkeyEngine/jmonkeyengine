package com.jme3.compat;

import com.jme3.material.Material;
import com.jme3.scene.Mesh;
import com.jme3.vulkan.buffers.generate.BufferGenerator;
import com.jme3.vulkan.mesh.AdaptiveMesh;
import com.jme3.vulkan.mesh.MeshDescription;
import com.jme3.vulkan.mesh.VkMesh;

public class VulkanFactory implements ComponentFactory {

    private MeshDescription meshDescription;
    private BufferGenerator<?> bufferGenerator;

    @Override
    public Material createMaterial(String material) {
        throw new UnsupportedOperationException("To be implemented.");
    }

    @Override
    public Material createBlankMaterial(String matdef) {
        throw new UnsupportedOperationException("To be implemented.");
    }

    @Override
    public Mesh createBlankMesh() {
        return new AdaptiveMesh(meshDescription, bufferGenerator);
    }

    @Override
    public Mesh migrateMesh(Mesh mesh) {
        if (!(mesh instanceof VkMesh)) {

        }
    }

}
