package com.jme3.vulkan.mesh;

import com.jme3.vulkan.buffers.Mappable;
import com.jme3.vulkan.mesh.attribute.Attribute;

public interface AttributeMapping <T extends Attribute> {

    T map(Mappable vertices, int size, int stride, int offset);

}
