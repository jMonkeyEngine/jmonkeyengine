package com.jme3.vulkan.mesh;

import com.jme3.backend.Engine;
import com.jme3.scene.GlVertexBuffer;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.mesh.attribute.Attribute;
import com.jme3.vulkan.util.IntEnum;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class AdaptiveVertexBinding implements VertexBinding {

    private final Engine engine;
    private final IntEnum<InputRate> rate;
    private final List<NamedAttribute> attributes = new ArrayList<>();
    private int stride = 0;
    private long offset = 0L;

    protected AdaptiveVertexBinding(Engine engine, IntEnum<InputRate> rate) {
        this.engine = engine;
        this.rate = rate;
    }

    @Override
    public <T extends Attribute> T mapAttribute(String name, MappableBuffer vertices, int size) {
        for (NamedAttribute a : attributes) {
            if (a.getName().equals(name)) {
                return (T)a.getMapper().apply(new AttributeMappingInfo(this, vertices, size, a.getOffset()));
            }
        }
        return null;
    }

    @Override
    public MappableBuffer createBuffer(int elements, GlVertexBuffer.Usage usage) {
        return engine.createBuffer(new MemorySize(elements, stride), BufferUsage.Vertex, usage);
    }

    @Override
    public void setOffset(long offset) {
        this.offset = offset;
    }

    @Override
    public int getStride() {
        return stride;
    }

    @Override
    public IntEnum<InputRate> getInputRate() {
        return rate;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Collection<NamedAttribute> getAttributes() {
        return Collections.unmodifiableCollection(attributes);
    }

    @Override
    public NamedAttribute getFirstAttribute() {
        return attributes.get(0);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AdaptiveVertexBinding that = (AdaptiveVertexBinding)o;
        return stride == that.stride
                && Objects.equals(attributes, that.attributes)
                && Objects.equals(rate, that.rate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributes, rate, stride);
    }

    public static AdaptiveVertexBinding build(Engine engine, IntEnum<InputRate> rate, Consumer<Builder> config) {
        AdaptiveVertexBinding b = new AdaptiveVertexBinding(engine, rate);
        config.accept(b.new Builder());
        return b;
    }

    public class Builder {

        protected Builder() {}

        public void add(String name, Format[] format, Function<AttributeMappingInfo, Attribute> mapper) {
            NamedAttribute attr = new NamedAttribute(name, format, mapper, stride);
            attributes.add(attr);
            stride += attr.getSize();
        }

        public void add(GlVertexBuffer.Type type, Format[] format, Function<AttributeMappingInfo, Attribute> mapper) {
            add(type.name(), format, mapper);
        }

        public void add(String name, Format format, Function<AttributeMappingInfo, Attribute> mapper) {
            add(name, new Format[] {format}, mapper);
        }

        public void add(GlVertexBuffer.Type type, Format format, Function<AttributeMappingInfo, Attribute> mapper) {
            add(type.name(), format, mapper);
        }

    }

}
