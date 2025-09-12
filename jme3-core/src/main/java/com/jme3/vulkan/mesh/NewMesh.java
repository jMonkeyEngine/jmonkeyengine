package com.jme3.vulkan.mesh;

import com.jme3.vulkan.commands.CommandBuffer;

public interface NewMesh {

    void bind(CommandBuffer cmd);

    void draw(CommandBuffer cmd);

    int getVertexCount();

    int getTriangleCount();

}
