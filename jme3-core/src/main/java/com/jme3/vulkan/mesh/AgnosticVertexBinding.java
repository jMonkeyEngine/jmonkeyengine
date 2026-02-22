package com.jme3.vulkan.mesh;

import com.jme3.backend.Engine;
import com.jme3.scene.GlVertexBuffer;
import com.jme3.scene.Mesh;
import com.jme3.vulkan.buffers.BufferGenerator;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.mesh.attribute.Attribute;
import com.jme3.vulkan.util.IntEnum;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

@Deprecated
public class AgnosticVertexBinding implements VertexBinding {

    private final BufferGenerator<MappableBuffer> bufferGen;
    private final IntEnum<InputRate> rate;
    private final List<NamedAttribute> attributes = new ArrayList<>();
    private int stride = 0;
    private long offset = 0L;

    protected AgnosticVertexBinding(BufferGenerator<MappableBuffer> bufferGen, IntEnum<InputRate> rate) {
        this.bufferGen = bufferGen;
        this.rate = rate;
    }

    @Override
    public <T extends Attribute> T mapAttribute(String name, Mesh mesh) {
        for (NamedAttribute a : attributes) {
            if (a.getName().equals(name)) return a.map(mesh, this);
        }
        return null;
    }

    @Override
    public MappableBuffer createBuffer(long elements, GlVertexBuffer.Usage usage) {
        return bufferGen.createBuffer(new MemorySize(elements, stride), BufferUsage.Vertex, usage);
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
    public NamedAttribute getAttribute(String name) {
        return attributes.stream().filter(a -> a.getName().equals(name)).findAny().orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AgnosticVertexBinding that = (AgnosticVertexBinding)o;
        return stride == that.stride
                && Objects.equals(attributes, that.attributes)
                && Objects.equals(rate, that.rate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributes, rate, stride);
    }

    public static AgnosticVertexBinding build(Engine engine, IntEnum<InputRate> rate, Consumer<Builder> config) {
        AgnosticVertexBinding b = new AgnosticVertexBinding(engine, rate);
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
