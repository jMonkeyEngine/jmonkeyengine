package com.jme3.vulkan.buffer;

public class DynamicallySizedBuffer implements EngineBuffer {

    private EngineBuffer buffer;

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

    @Override
    public BufferMapping map() {
        return buffer.map();
    }

    @Override
    public EngineBuffer getSourceBuffer() {
        return this;
    }

    @Override
    public int size() {
        return buffer.size();
    }

}
