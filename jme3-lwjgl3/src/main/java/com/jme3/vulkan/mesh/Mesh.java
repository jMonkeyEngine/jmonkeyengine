package com.jme3.vulkan.mesh;

import com.jme3.bounding.BoundingVolume;
import com.jme3.vulkan.commands.CommandBuffer;

public interface Mesh {

    void update(CommandBuffer cmd);

    void bind(CommandBuffer cmd);

    void draw(CommandBuffer cmd);

}
