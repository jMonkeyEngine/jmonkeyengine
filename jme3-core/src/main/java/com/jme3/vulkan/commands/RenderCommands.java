package com.jme3.vulkan.commands;

import com.jme3.vulkan.buffer.tracking.BufferTracker;
import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.GpuBuffer;

import java.nio.ByteBuffer;

@Deprecated
public interface RenderCommands {

    OpLocation cmdCopy(GpuBuffer src, int srcOffset, GpuBuffer dst, int dstOffset, int size, OpLocation location);

    OpLocation cmdCopy(GpuBuffer src, int srcOffset, GpuBuffer dst, int dstOffset, BufferTracker regions, OpLocation location);

    OpLocation cmdFlatCopy(GpuBuffer src, int srcOffset, GpuBuffer dst, int dstOffset, BufferTracker regions, boolean flatten, OpLocation location);

    void cmdStreamToRemote(ByteBuffer src, GpuBuffer dst, BufferTracker regions);

    default void cmdStreamToRemote(DataBuffer src, GpuBuffer dst) {
        cmdStreamToRemote(src.getBytes(), dst, src.getTracker());
    }

    void cmdStreamFromRemote(GpuBuffer src, ByteBuffer dst, Runnable callback);

    default void cmdStreamFromRemote(GpuBuffer src, ByteBuffer dst) {
        cmdStreamFromRemote(src, dst, null);
    }

}
