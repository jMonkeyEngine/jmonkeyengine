package com.jme3.vulkan.mesh;

import com.jme3.vulkan.commands.CommandBuffer;

public interface Mesh {

    void bind(CommandBuffer cmd);

    void draw(CommandBuffer cmd);

    int getVertexCount();

    int getTriangleCount();

}
