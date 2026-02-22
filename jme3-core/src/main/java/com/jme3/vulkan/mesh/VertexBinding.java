package com.jme3.vulkan.mesh;

import com.jme3.backend.Engine;
import com.jme3.export.*;
import com.jme3.scene.GlVertexBuffer;
import com.jme3.scene.Mesh;
import com.jme3.vulkan.buffers.BufferGenerator;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.mesh.attribute.Attribute;
import com.jme3.vulkan.tmp.EffectivelyFinal;
import com.jme3.vulkan.tmp.EffectivelyFinalWriter;
import com.jme3.vulkan.tmp.SerializationOnly;
import com.jme3.vulkan.util.IntEnum;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class VertexBinding implements Savable {

    @EffectivelyFinal
    private BufferGenerator<MappableBuffer> bufferGen;
    @EffectivelyFinal
    private IntEnum<InputRate> rate;

    private final List<NamedAttribute> attributes = new ArrayList<>();
    private int stride = 0;
    private long offset = 0L;

    @SerializationOnly
    protected VertexBinding() {}

    protected VertexBinding(BufferGenerator<MappableBuffer> bufferGen, IntEnum<InputRate> rate) {
        this.bufferGen = bufferGen;
        this.rate = rate;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(rate.getEnum(), "rate", InputRate.Vertex.getEnum());
        out.writeSavableArrayList(new ArrayList<>(attributes), "attributes", null);
        out.write(offset, "offset", 0);
        for (NamedAttribute a : attributes) {
            stride += a.getSize();
        }
    }

    @Override
    @EffectivelyFinalWriter
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        rate = IntEnum.of(in.readInt("rate", InputRate.Vertex.getEnum()));
        attributes.addAll(in.readSavableArrayList("attributes", null));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        VertexBinding that = (VertexBinding)o;
        return stride == that.stride
                && offset == that.offset
                && Objects.equals(attributes, that.attributes)
                && Objects.equals(rate, that.rate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributes, rate, stride);
    }

    public <T extends Attribute> T mapAttribute(String name, Mesh mesh) {
        for (NamedAttribute a : attributes) {
            if (a.getName().equals(name)) return a.map(mesh, this);
        }
        return null;
    }

    public MappableBuffer createBuffer(long elements, GlVertexBuffer.Usage usage) {
        return bufferGen.createBuffer(new MemorySize(elements, stride), BufferUsage.Vertex, usage);
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public int getStride() {
        return stride;
    }

    public IntEnum<InputRate> getInputRate() {
        return rate;
    }

    public long getOffset() {
        return offset;
    }

    public Collection<NamedAttribute> getAttributes() {
        return Collections.unmodifiableCollection(attributes);
    }

    public NamedAttribute getFirstAttribute() {
        return attributes.get(0);
    }

    public NamedAttribute getAttribute(String name) {
        return attributes.stream().filter(a -> a.getName().equals(name)).findAny().orElse(null);
    }

    public static VertexBinding build(Engine engine, IntEnum<InputRate> rate, Consumer<VertexBinding.Builder> config) {
        VertexBinding b = new VertexBinding(engine, rate);
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
