package com.jme3.vulkan.mesh;

import com.jme3.scene.Mesh;
import com.jme3.vulkan.commands.CommandBuffer;

// todo: rename to VulkanMesh; for whatever reason, IntelliJ refuses to recognize VulkanMesh as a valid type
public interface VkMesh extends Mesh {

    void bind(CommandBuffer cmd, int lod);

    void render(CommandBuffer cmd, int lod);

    MeshDescription getDescription();

}
