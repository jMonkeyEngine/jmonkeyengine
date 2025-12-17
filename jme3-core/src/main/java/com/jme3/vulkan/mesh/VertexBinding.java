package com.jme3.vulkan.mesh;

import com.jme3.scene.GlVertexBuffer;
import com.jme3.vulkan.Format;
import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.buffers.Mappable;
import com.jme3.vulkan.mesh.attribute.Attribute;
import com.jme3.vulkan.pipeline.VertexPipeline;
import com.jme3.vulkan.util.IntEnum;

import java.util.Collection;
import java.util.Objects;

public interface VertexBinding {

    <T extends Attribute> T mapAttribute(String name, GpuBuffer vertices, int size);

    GpuBuffer createBuffer(int elements);

    void setOffset(long offset);

    long getOffset();

    int getStride();

    IntEnum<InputRate> getInputRate();

    Collection<NamedAttribute> getAttributes();

    default boolean bindOnPipeline(VertexPipeline pipeline) {
        return getAttributes().stream().anyMatch(a -> a.bindOnPipeline(pipeline));
    }

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

        public boolean bindOnPipeline(VertexPipeline pipeline) {
            return pipeline.getAttributeLocation(name) != null;
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

        Builder setUsage(GlVertexBuffer.Usage usage);

        VertexBinding build();

        default Builder add(GlVertexBuffer.Type name, Format format, AttributeMapping mapping) {
            return add(name.name(), format, mapping);
        }

    }

}
