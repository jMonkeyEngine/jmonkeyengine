package com.jme3.vulkan.buffers;

import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.*;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentBuffer <T extends MappableBuffer> implements MappableBuffer {

    private final T buffer;
    private BufferMapping mapping;
    private int numMappings = 0;
    private final Lock mappingLock = new ReentrantLock();

    public ConcurrentBuffer(T buffer) {
        this.buffer = Objects.requireNonNull(buffer, "Buffer cannot be null.");
    }

    @Override
    public BufferMapping map(long offset, long size) {
        mappingLock.lock();
        BufferMapping m = new ConcurrentMapping(offset, size);
        mappingLock.unlock();
        return m;
    }

    @Override
    public void stage(long offset, long size) {
        buffer.stage(offset, size);
    }

    @Override
    public ResizeResult resize(MemorySize size) {
        mappingLock.lock();
        ResizeResult result = buffer.resize(size);
        mappingLock.unlock();
        return result;
    }

    @Override
    public MemorySize size() {
        return buffer.size();
    }

    public T getBuffer() {
        return buffer;
    }

    public int getNumConcurrentMappings() {
        return numMappings;
    }

    private class ConcurrentMapping implements BufferMapping {

        private final long offset, size;
        private final long address;
        private ByteBuffer bytes;
        private ShortBuffer shorts;
        private IntBuffer ints;
        private FloatBuffer floats;
        private DoubleBuffer doubles;
        private LongBuffer longs;
        private PointerBuffer pointers;

        public ConcurrentMapping(long offset, long size) {
            if (mapping == null) {
                mapping = buffer.map();
            }
            this.offset = offset;
            this.size = size;
            this.address = MemoryUtil.memAddress(mapping.getBytes(), (int)offset);
            numMappings++;
        }

        @Override
        public void close() {
            mappingLock.lock();
            if (numMappings-- == 0) {
                mapping.close();
                mapping = null;
            }
            mappingLock.unlock();
        }

        @Override
        public void push(long offset, long size) {
            mapping.push(this.offset + offset, size);
        }

        @Override
        public long getAddress() {
            return address;
        }

        @Override
        public long getSize() {
            return size;
        }

        @Override
        public ByteBuffer getBytes() {
            if (bytes == null) {
                bytes = MemoryUtil.memByteBuffer(address, (int)size);
            }
            return bytes;
        }

        @Override
        public ShortBuffer getShorts() {
            if (shorts == null) {
                shorts = MemoryUtil.memShortBuffer(address, (int)size);
            }
            return shorts;
        }

        @Override
        public IntBuffer getInts() {
            if (ints == null) {
                ints = MemoryUtil.memIntBuffer(address, (int)size);
            }
            return ints;
        }

        @Override
        public FloatBuffer getFloats() {
            if (floats == null) {
                floats = MemoryUtil.memFloatBuffer(address, (int)size);
            }
            return floats;
        }

        @Override
        public DoubleBuffer getDoubles() {
            if (doubles == null) {
                doubles = MemoryUtil.memDoubleBuffer(address, (int)size);
            }
            return doubles;
        }

        @Override
        public LongBuffer getLongs() {
            if (longs == null) {
                longs = MemoryUtil.memLongBuffer(address, (int)size);
            }
            return longs;
        }

        @Override
        public PointerBuffer getPointers() {
            if (pointers == null) {
                pointers = MemoryUtil.memPointerBuffer(address, (int)size);
            }
            return pointers;
        }

    }

}
