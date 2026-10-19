package com.jme3.vulkan.alloc;

import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructArray;
import com.jme3.vulkan.buffer.GpuBuffer;

import java.util.function.IntFunction;

public class BufferStructArray <T extends Struct> extends StructArray<T> implements GpuBuffer {

    private final IntFunction<GpuBuffer> bufferFactory;
    private GpuBuffer buffer;

    public BufferStructArray(T struct, int length, IntFunction<GpuBuffer> bufferFactory) {
        super(struct, length);
        this.bufferFactory = bufferFactory;
        this.buffer = bufferFactory.apply(getByteSize());
    }

    @Override
    public void copyToPreferOptimal(GpuBuffer dst) {
        buffer.copyToPreferOptimal(dst);
    }

    @Override
    public void copyToPreferHost(GpuBuffer dst) {
        buffer.copyToPreferHost(dst);
    }

    @Override
    public void copyToPreferDevice(GpuBuffer dst) {
        buffer.copyToPreferDevice(dst);
    }

    @Override
    public void resize(int size) {
        setLength((int)Math.ceil((double)size / getByteStride()));
    }

    @Override
    public void setLength(int length) {
        super.setLength(length);
        try {
            buffer.resize(getByteSize());
        } catch (UnsupportedOperationException e) {
            buffer = bufferFactory.apply(getByteSize());
        }
    }

    @Override
    public int size() {
        return getByteSize();
    }

    @Override
    public void stage(int offset, int size) {
        buffer.stage(offset, size);
    }

    @Override
    public void pushStaged() {
        buffer.pushStaged();
    }

    @Override
    public void pullStaged() {
        buffer.pullStaged();
    }

    @Override
    public void clearStaging() {
        buffer.clearStaging();
    }

}
