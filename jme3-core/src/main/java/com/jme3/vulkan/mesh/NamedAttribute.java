package com.jme3.vulkan.mesh;

import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.mesh.attribute.Attribute;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

public class NamedAttribute {

    private final String name;
    private final Format[] formats;
    private final Function<AttributeMappingInfo, Attribute> mapper;
    private final int offset, size;

    public NamedAttribute(String name, Format[] formats, Function<AttributeMappingInfo, Attribute> mapper, int offset) {
        this.name = name;
        this.formats = formats;
        this.mapper = mapper;
        this.offset = offset;
        int size = 0;
        for (Format f : formats) {
            size += f.getBytes();
        }
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public Function<AttributeMappingInfo, Attribute> getMapper() {
        return mapper;
    }

    public Format[] getFormats() {
        return formats;
    }

    public int getOffset() {
        return offset;
    }

    public int getSize() {
        return size;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        NamedAttribute that = (NamedAttribute) o;
        return offset == that.offset && formats == that.formats;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(formats), offset);
    }

}
