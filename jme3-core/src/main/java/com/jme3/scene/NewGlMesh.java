package com.jme3.scene;

import com.jme3.vulkan.mesh.attribute.Attribute;

import java.util.HashMap;
import java.util.Map;

public class NewGlMesh {

    private final Map<Class, GlVertexBuffer> buffers = new HashMap<>();
    private int vertices = 0;

    public <K extends Attribute, T extends K> T getAttribute(Class<? extends Attribute> key) {
        GlVertexBuffer buf = buffers.get(key);
        if (buf == null) {
            // create buffer??? How???
        }
        return (T)buf.getAttribute().map();
    }

}
