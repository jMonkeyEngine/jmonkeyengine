package com.jme3.vulkan.mesh;

import com.jme3.vulkan.Format;

public class VertexAttribute {

    private final VertexBinding binding;
    private final String name;
    private final Format format;
    private final int location;
    private final int offset;

    public VertexAttribute(VertexBinding binding, String name, Format format, int location, int offset) {
        this.binding = binding;
        this.name = name;
        this.format = format;
        this.location = location;
        this.offset = offset;
    }

    public AttributeModifier modify() {
        return new AttributeModifier(this);
    }

    public String getName() {
        return name;
    }

    public VertexBinding getBinding() {
        return binding;
    }

    public Format getFormat() {
        return format;
    }

    public int getLocation() {
        return location;
    }

    public int getOffset() {
        return offset;
    }

}
