package com.jme3.vulkan.mesh;

public interface AttributeModifier extends AutoCloseable, VertexReader, VertexWriter {

    @Override
    void close();

}
