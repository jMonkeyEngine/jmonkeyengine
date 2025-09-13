package com.jme3.vulkan.mesh;

import com.jme3.vulkan.Format;

public class VertexAttribute {

    public static final String POSITION = "jme_position";
    public static final String NORMALS = "jme_normals";
    public static final String TEXCOORD = "jme_texCoord";
    public static final String COLOR = "jme_color";

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
