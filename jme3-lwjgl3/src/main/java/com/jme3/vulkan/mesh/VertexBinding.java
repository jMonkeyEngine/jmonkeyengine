package com.jme3.vulkan.mesh;

import com.jme3.vulkan.buffers.GpuBuffer;

import java.nio.ByteBuffer;

public class VertexBinding {

    private final GpuBuffer buffer;
    private final int stride;
    private final InputRate rate;
    private ModificationSession session;

    public VertexBinding(GpuBuffer buffer, int stride, InputRate rate) {
        this.buffer = buffer;
        this.stride = stride;
        this.rate = rate;
    }

    public ByteBuffer map() {
        if (session == null) {
            session = new ModificationSession();
            return session.data;
        }
        return session.addModifier();
    }

    public void unmap() {
        if (session == null) {
            throw new NullPointerException("No modification session is mapped.");
        }
        if (!session.removeModifier()) {
            session = null;
        }
    }

    public GpuBuffer getBuffer() {
        return buffer;
    }

    public int getStride() {
        return stride;
    }

    public InputRate getRate() {
        return rate;
    }

    public class ModificationSession {

        private final ByteBuffer data;
        private int modifiers = 1;

        private ModificationSession() {
            this.data = buffer.mapBytes(0, buffer.size().getBytes());
        }

        public ByteBuffer addModifier() {
            modifiers++;
            return data;
        }

        public boolean removeModifier() {
            if (--modifiers == 0) {
                buffer.unmap();
                return false;
            }
            return true;
        }

    }

}
