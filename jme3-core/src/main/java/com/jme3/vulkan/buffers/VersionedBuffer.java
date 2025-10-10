package com.jme3.vulkan.buffers;

import com.jme3.vulkan.frames.UpdateFrameManager;
import com.jme3.vulkan.frames.VersionedResource;
import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class VersionedBuffer <T extends GpuBuffer> implements GpuBuffer, VersionedResource<T> {

    private final UpdateFrameManager<?> frames;
    private final List<T> buffers;
    private int elements;

    public VersionedBuffer(UpdateFrameManager<?> frames, MemorySize size, Function<MemorySize, T> generator) {
        this.frames = frames;
        this.elements = size.getElements();
        List<T> bufs = new ArrayList<>(frames.getTotalFrames());
        for (int i = 0; i < frames.getTotalFrames(); i++) {
            bufs.add(generator.apply(size));
        }
        buffers = Collections.unmodifiableList(bufs);
    }

    @Override
    public PointerBuffer map(int offset, int size) {
        return updateBufferSize().map(offset, size);
    }

    @Override
    public void unmap() {
        updateBufferSize().unmap();
    }

    @Override
    public MemorySize size() {
        return updateBufferSize().size();
    }

    @Override
    public long getId() {
        return updateBufferSize().getId();
    }

    @Override
    public void resize(int elements) {
        this.elements = elements;
        updateBufferSize();
    }

    @Override
    public T get() {
        return buffers.get(frames.getCurrentFrame());
    }

    @Override
    public T get(int frame) {
        return buffers.get(frame);
    }

    @Override
    public int getNumResources() {
        return buffers.size();
    }

    @Override
    public Iterator<T> iterator() {
        return buffers.iterator();
    }

    private T updateBufferSize() {
        T buf = buffers.get(frames.getCurrentFrame());
        if (elements != buf.size().getElements()) {
            buf.resize(elements);
        }
        return buf;
    }

}
