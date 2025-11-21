package com.jme3.vulkan.buffers;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.frames.UpdateFrameManager;
import com.jme3.vulkan.frames.VersionedResource;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.update.Command;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Maintains an internal buffer (versions) for each possible frame.
 *
 * <p>CPU-side changes are tracked by an {@link NioBuffer}. On command, the
 * local changes are copied to the current version if changes were made since
 * the last copy operation to that version. For consistency, the current version
 * is updated when this buffer is {@link #unmap() unmapped}.</p>
 *
 * @param <T>
 */
@Deprecated
public class PerFrameBuffer <T extends GpuBuffer> implements GpuBuffer, VersionedResource<T>, Command {

    private final UpdateFrameManager<?> frames;
    private final List<T> buffers;
    private final NioBuffer cpuBuffer;
    private final BufferRegion[] dirtyRegions;
    private int elements;

    public PerFrameBuffer(UpdateFrameManager<?> frames, MemorySize size, Function<MemorySize, T> generator) {
        this.frames = frames;
        this.elements = size.getElements();
        List<T> buffers = new ArrayList<>(frames.getTotalFrames());
        for (int i = 0; i < frames.getTotalFrames(); i++) {
            buffers.add(generator.apply(size));
        }
        this.buffers = Collections.unmodifiableList(buffers);
        this.cpuBuffer = new NioBuffer(size, 10);
        this.dirtyRegions = new BufferRegion[frames.getTotalFrames()];
    }

    @Override
    public PointerBuffer map(int offset, int size) {
        // mark mapped region as dirty on all versioned buffers
        for (int i = 0; i < dirtyRegions.length; i++) {
            if (dirtyRegions[i] == null) {
                dirtyRegions[i] = new BufferRegion(offset, size);
            } else {
                dirtyRegions[i].unionLocal(offset, size);
            }
        }
        //return updateBufferSize().map(offset, size);
        return cpuBuffer.map(offset, size);
    }

    @Override
    public void unmap() {
        cpuBuffer.unmap();
        updateCurrentBuffer();
    }

    @Override
    public MemorySize size() {
        return cpuBuffer.size();
    }

    @Override
    public long getId() {
        return updateCurrentBuffer().getId();
    }

    @Override
    public boolean resize(MemorySize size) {
        return false;
    }

    @Override
    public boolean resize(int elements) {
        this.elements = elements;
        int length = elements * size().getBytesPerElement();
        // adjust dirty regions
        for (int i = 0; i < dirtyRegions.length; i++) {
            BufferRegion d = dirtyRegions[i];
            // if the full dirty region is now outside the buffer, nullify the dirty region
            if (d != null && d.getEnd() > length && !d.setEnd(length)) {
                dirtyRegions[i] = null;
            }
        }
        boolean resized = cpuBuffer.resize(elements);
        updateCurrentBuffer();
        return resized;
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

    @Override
    public boolean requiresCommandBuffer(int frame) {
        return false;
    }

    @Override
    public void run(CommandBuffer cmd, int frame) {
        updateBuffer(frame);
    }

    private T updateBuffer(int frame) {
        T buf = get(frame);
        if (elements != buf.size().getElements()) {
            buf.resize(elements);
        }
        // copy staged data to the current version on the dirty region
        BufferRegion d = dirtyRegions[frame];
        if (d != null) {
            ByteBuffer src = cpuBuffer.mapBytes(d.getOffset(), d.getSize());
            ByteBuffer dst = buf.mapBytes(d.getOffset(), d.getSize());
            MemoryUtil.memCopy(src, dst);
            cpuBuffer.unmap();
            buf.unmap();
            dirtyRegions[frame] = null; // dirty region has been cleaned
        }
        return buf;
    }

    private T updateCurrentBuffer() {
        return updateBuffer(frames.getCurrentFrame());
    }

}
