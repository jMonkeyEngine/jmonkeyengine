package com.jme3.vulkan.buffers;

import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.frames.VersionedResource;
import com.jme3.vulkan.frames.UpdateFrameManager;
import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class VersionedBuffer <T extends VulkanBuffer> implements VulkanBuffer, VersionedResource<T> {

    private final UpdateFrameManager frames;
    private final MemorySize size;
    private final List<T> buffers;

    public VersionedBuffer(UpdateFrameManager frames, MemorySize size, Function<MemorySize, T> factory) {
        this.frames = frames;
        this.size = size;
        ArrayList<T> bufferList = new ArrayList<>();
        for (int i = 0; i < frames.getTotalFrames(); i++) {
            bufferList.add(factory.apply(size));
        }
        this.buffers = Collections.unmodifiableList(bufferList);
    }

    @Override
    public PointerBuffer map(int offset, int size) {
        return getVersion().map(offset, size);
    }

    @Override
    public void unmap() {
        getVersion().unmap();
    }

    @Override
    public void freeMemory() {
        buffers.forEach(GpuBuffer::freeMemory);
    }

    @Override
    public MemorySize size() {
        return size;
    }

    @Override
    public long getId() {
        return getVersion().getId();
    }

    @Override
    public T getVersion() {
        return getVersion(frames.getCurrentFrame());
    }

    @Override
    public T getVersion(int i) {
        return buffers.get(Math.min(i, buffers.size() - 1));
    }

    @Override
    public int getNumVersions() {
        return buffers.size();
    }

    @Override
    public int getCurrentVersionIndex() {
        return Math.min(frames.getCurrentFrame(), buffers.size() - 1);
    }

    public List<T> getInternalBuffers() {
        return buffers;
    }

    @Override
    public LogicalDevice<?> getDevice() {
        return getVersion().getDevice();
    }

}
