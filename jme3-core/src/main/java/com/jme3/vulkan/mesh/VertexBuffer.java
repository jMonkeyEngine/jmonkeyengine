package com.jme3.vulkan.mesh;

import com.jme3.export.*;
import com.jme3.util.struct.*;
import com.jme3.vulkan.buffers.Mappable;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.compile.Final;
import com.jme3.vulkan.compile.FinalWriter;
import com.jme3.vulkan.compile.SerializationOnly;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;

@Deprecated
public class VertexBuffer <T extends Struct<VertexAttr>> extends SingletonStructArray<T> {

    private final InputRate rate;
    private T struct;
    private MappableBuffer buffer;
    private final Map<String, VertexAttr> attributeCache = new HashMap<>();

    @SerializationOnly
    protected VertexBuffer() {}

    public VertexBuffer(InputRate rate, T struct) {
        this.rate = rate;
        this.struct = struct;
        this.buffer = buffer;
        this.struct.bind(StructLayout.optimal);
    }

    public VertexBuffer(InputRate rate, T struct, int elements, IntFunction<MappableBuffer> buffer) {
        this.rate = rate;
        this.struct = struct;
        this.struct.bind(StructLayout.optimal);
        this.buffer = buffer.apply(elements * this.struct.alignedSize());
    }

    public StructMapping<T> map(long offset, long elements) {
        return buffer.mapStructs(struct, offset, elements);
    }

    public StructMapping<T> map() {
        return buffer.mapAllStructs(struct);
    }

    public void resize(long elements) {
        buffer.resize(buffer.size().setBytes(elements * struct.alignedSize()));
    }

    public void stage(long offset, long elements) {
        int stride = struct.alignedSize();
        buffer.stage(offset * stride, elements * stride);
    }

    public InputRate getRate() {
        return rate;
    }

    @Override
    public Struct<VertexAttr> getStruct() {
        return struct;
    }

    public MappableBuffer getBuffer() {
        return buffer;
    }

    public int getStride() {
        return struct.alignedSize();
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
