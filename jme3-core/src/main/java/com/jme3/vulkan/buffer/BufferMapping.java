package com.jme3.vulkan.buffer;

import java.nio.ByteBuffer;

public class BufferMapping {

    private final EngineBuffer source;
    private final ByteBuffer buffer;

    public BufferMapping(EngineBuffer source, ByteBuffer buffer) {
        this.source = source;
        this.buffer = buffer;
    }

    public BufferMapping region(int offset, int size) {
        buffer.position(buffer.position() + offset).limit(buffer.position() + size);
        return this;
    }

    public BufferMapping offset(int offset) {
        buffer.position(buffer.position() + offset);
        return this;
    }

    public BufferMapping size(int size) {
        buffer.limit(buffer.position() + size);
        return this;
    }

    public void stage() {
        source.stage(buffer.position(), buffer.remaining());
    }

    public EngineBuffer getSource() {
        return source;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

}
