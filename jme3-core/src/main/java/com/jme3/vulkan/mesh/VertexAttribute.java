package com.jme3.vulkan.mesh;

import com.jme3.vulkan.Format;

import java.util.Objects;

public class VertexAttribute {

    // todo: remove
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
        this.binding = Objects.requireNonNull(binding);
        this.name = Objects.requireNonNull(name);
        this.format = Objects.requireNonNull(format);
        this.location = location;
        this.offset = offset;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        VertexAttribute that = (VertexAttribute) o;
        return location == that.location
                && offset == that.offset
                && binding.getBindingIndex() == that.binding.getBindingIndex()
                && Objects.equals(name, that.name)
                && format == that.format;
    }

    @Override
    public int hashCode() {
        return Objects.hash(binding, name, format, location, offset);
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
