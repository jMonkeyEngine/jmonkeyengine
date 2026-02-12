package com.jme3.vulkan.mesh;

import com.jme3.scene.GlVertexBuffer;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.mesh.attribute.Attribute;
import com.jme3.vulkan.util.IntEnum;

import java.util.Collection;

public interface VertexBinding {

    <T extends Attribute> T mapAttribute(String name, MappableBuffer vertices, int size);

    MappableBuffer createBuffer(int elements, GlVertexBuffer.Usage usage);

    void setOffset(long offset);

    long getOffset();

    int getStride();

    IntEnum<InputRate> getInputRate();

    Collection<NamedAttribute> getAttributes();

    NamedAttribute getFirstAttribute();

}
