package com.jme3.vulkan.mesh;

import com.jme3.export.*;
import com.jme3.util.struct.*;
import com.jme3.vulkan.buffers.Mappable;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.tmp.Final;
import com.jme3.vulkan.tmp.FinalWriter;
import com.jme3.vulkan.tmp.SerializationOnly;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;

public class VertexBuffer <T extends Struct<VertexAttr>> implements Savable {

    @Final private InputRate rate;
    @Final private T struct;
    @Final private MappableBuffer buffer;
    private final Map<String, VertexAttr> attributeCache = new HashMap<>();

    @SerializationOnly
    protected VertexBuffer() {}

    public VertexBuffer(InputRate rate, T struct, MappableBuffer buffer) {
        this.rate = rate;
        this.struct = struct;
        this.buffer = buffer;
        this.struct.bind(StructLayout.packed);
    }

    public VertexBuffer(InputRate rate, T struct, int elements, IntFunction<MappableBuffer> buffer) {
        this.rate = rate;
        this.struct = struct;
        this.struct.bind(StructLayout.packed);
        this.buffer = buffer.apply(elements * this.struct.getAlignedSize());
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(rate, "rate", InputRate.Vertex);
        out.write(struct, "struct", null);
        out.write(buffer, "buffer", null);
    }

    @Override
    @FinalWriter
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        rate = in.readEnum("rate", InputRate.class, InputRate.Vertex);
        struct = (T)in.readSavable("struct", null);
        buffer = (MappableBuffer)in.readSavable("buffer", null);
    }

    public StructMapping<T> map(long offset, long elements) {
        return buffer.mapStructs(struct, offset, elements);
    }

    public StructMapping<T> map() {
        return buffer.mapAllStructs(struct);
    }

    public void resize(long elements) {
        buffer.resize(buffer.size().setBytes(elements * struct.getAlignedSize()));
    }

    public void stage(long offset, long elements) {
        int stride = struct.getAlignedSize();
        buffer.stage(offset * stride, elements * stride);
    }

    public InputRate getRate() {
        return rate;
    }

    public Struct<VertexAttr> getStruct() {
        return struct;
    }

    public MappableBuffer getBuffer() {
        return buffer;
    }

    public int getStride() {
        return struct.getAlignedSize();
    }

    public <E extends StructField> E getAttribute(String name) {
        VertexAttr attr = attributeCache.get(name);
        if (attr == null) for (VertexAttr a : struct.getFields()) {
            if (a.getName().equals(name)) {
                attributeCache.put(name, a);
                return (E)a;
            }
        }
        return (E)attr;
    }

}
