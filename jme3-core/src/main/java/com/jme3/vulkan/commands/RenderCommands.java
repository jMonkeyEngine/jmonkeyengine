package com.jme3.vulkan.commands;

import com.jme3.vulkan.buffer.tracking.BufferTracker;
import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.EngineBuffer;

import java.nio.ByteBuffer;

@Deprecated
public interface RenderCommands {

    OpLocation cmdCopy(EngineBuffer src, int srcOffset, EngineBuffer dst, int dstOffset, int size, OpLocation location);

    OpLocation cmdCopy(EngineBuffer src, int srcOffset, EngineBuffer dst, int dstOffset, BufferTracker regions, OpLocation location);

    OpLocation cmdFlatCopy(EngineBuffer src, int srcOffset, EngineBuffer dst, int dstOffset, BufferTracker regions, boolean flatten, OpLocation location);

    void cmdStreamToRemote(ByteBuffer src, EngineBuffer dst, BufferTracker regions);

    default void cmdStreamToRemote(DataBuffer src, EngineBuffer dst) {
        cmdStreamToRemote(src.getBytes(), dst, src.getTracker());
    }

    void cmdStreamFromRemote(EngineBuffer src, ByteBuffer dst, Runnable callback);

    default void cmdStreamFromRemote(EngineBuffer src, ByteBuffer dst) {
        cmdStreamFromRemote(src, dst, null);
    }

}
