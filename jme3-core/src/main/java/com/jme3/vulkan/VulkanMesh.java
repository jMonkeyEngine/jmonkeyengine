package com.jme3.vulkan;

import com.jme3.scene.Mesh;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.mesh.MeshDescription;

public interface VulkanMesh extends Mesh {

    void bind(CommandBuffer cmd, int lod);

    void render(CommandBuffer cmd, int lod);

    MeshDescription getDescription();

}
