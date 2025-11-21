package com.jme3.vulkan.mesh;

import com.jme3.scene.GlVertexBuffer;
import com.jme3.vulkan.Format;
import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.buffers.Mappable;
import com.jme3.vulkan.mesh.attribute.Attribute;
import com.jme3.vulkan.util.IntEnum;

import java.util.Objects;

public interface VertexBinding extends Iterable<VertexBinding.NamedAttribute> {

    <T extends Attribute> T mapAttribute(String name, Mappable vertices, int size);

    GpuBuffer createBuffer(int elements);

    void setOffset(long offset);

    long getOffset();

    int getStride();

    IntEnum<InputRate> getInputRate();

    int getNumAttributes();

    class NamedAttribute {

        private final String name;
        private final Format format;
        private final AttributeMapping mapping;
        private final int offset;

        public NamedAttribute(String name, Format format, AttributeMapping mapping, int offset) {
            this.name = name;
            this.format = format;
            this.mapping = mapping;
            this.offset = offset;
        }

        public String getName() {
            return name;
        }

        public AttributeMapping getMapping() {
            return mapping;
        }

        public Format getFormat() {
            return format;
        }

        public int getOffset() {
            return offset;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            NamedAttribute that = (NamedAttribute) o;
            return offset == that.offset && format == that.format;
        }

        @Override
        public int hashCode() {
            return Objects.hash(format, offset);
        }

    }

    interface Builder {

        Builder add(String name, Format format, AttributeMapping mapping);

        VertexBinding build();

        default Builder add(GlVertexBuffer.Type name, Format format, AttributeMapping mapping) {
            return add(name.name(), format, mapping);
        }

    }

}
