package com.jme3.vulkan.descriptors;

import java.util.Objects;

/**
 * Snapshot of a buffer region bound to a buffer descriptor.
 */
public final class BufferArtifact {

    private final long handle, offset, range;

    public BufferArtifact(long handle, long offset, long range) {
        this.handle = handle;
        this.offset = offset;
        this.range = range;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BufferArtifact that = (BufferArtifact) o;
        return handle == that.handle && offset == that.offset && range == that.range;
    }

    @Override
    public int hashCode() {
        return Objects.hash(handle, offset, range);
    }

    public long getHandle() {
        return handle;
    }

    public long getOffset() {
        return offset;
    }

    public long getRange() {
        return range;
    }

}
