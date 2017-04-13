package com.jme3.util;

import org.lwjgl.system.MemoryUtil;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.nio.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * The implementation of the {@link BufferAllocator} which use {@link MemoryUtil} to manage memory.
 *
 * @author JavaSaBr
 */
public class LWJGLBufferAllocator implements BufferAllocator {

    private static final Logger LOGGER = Logger.getLogger(LWJGLBufferAllocator.class.getName());

    /**
     * The reference queue.
     */
    private static final ReferenceQueue<Buffer> DUMMY_QUEUE = new ReferenceQueue<Buffer>();

    /**
     * The LWJGL byte buffer deallocator.
     */
    private static class Deallocator extends PhantomReference<ByteBuffer> {

        /**
         * The address of LWJGL byte buffer.
         */
        private volatile Long address;

        Deallocator(final ByteBuffer referent, final ReferenceQueue<? super ByteBuffer> queue, final Long address) {
            super(referent, queue);
            this.address = address;
        }

        /**
         * @param address the address of LWJGL byte buffer.
         */
        void setAddress(final Long address) {
            this.address = address;
        }

        /**
         * Free memory.
         */
        void free() {
            if (address == null) return;
            MemoryUtil.nmemFree(address);
            DEALLOCATORS.remove(address);
        }
    }

    /**
     * The cleaner thread.
     */
    private static final Thread CLEAN_THREAD = new Thread(LWJGLBufferAllocator::freeByteBuffers);

    /**
     * The map with created deallocators.
     */
    private static final Map<Long, Deallocator> DEALLOCATORS = new ConcurrentHashMap<>();

    static {
        CLEAN_THREAD.setDaemon(true);
        CLEAN_THREAD.setName("Thread to free LWJGL byte buffers");
        CLEAN_THREAD.start();
    }

    /**
     * Free unnecessary LWJGL byte buffers.
     */
    private static void freeByteBuffers() {
        try {
            for (; ; ) {
                final Deallocator deallocator = (Deallocator) DUMMY_QUEUE.remove();
                deallocator.free();
            }
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void destroyDirectBuffer(final Buffer buffer) {

        final long address = getAddress(buffer);

        if (address == -1) {
            LOGGER.warning("Not found address of the " + buffer);
            return;
        }

        // disable deallocator
        final Deallocator deallocator = DEALLOCATORS.remove(address);

        if (deallocator == null) {
            LOGGER.warning("Not found a deallocator for address " + address);
            return;
        }

        deallocator.setAddress(null);

        MemoryUtil.memFree(buffer);
    }

    /**
     * Get memory address of the buffer.
     *
     * @param buffer the buffer.
     * @return the address or -1.
     */
    private long getAddress(final Buffer buffer) {

        if (buffer instanceof ByteBuffer) {
            return MemoryUtil.memAddress((ByteBuffer) buffer, 0);
        } else if (buffer instanceof ShortBuffer) {
            return MemoryUtil.memAddress((ShortBuffer) buffer, 0);
        } else if (buffer instanceof CharBuffer) {
            return MemoryUtil.memAddress((CharBuffer) buffer, 0);
        } else if (buffer instanceof IntBuffer) {
            return MemoryUtil.memAddress((IntBuffer) buffer, 0);
        } else if (buffer instanceof FloatBuffer) {
            return MemoryUtil.memAddress((FloatBuffer) buffer, 0);
        } else if (buffer instanceof LongBuffer) {
            return MemoryUtil.memAddress((LongBuffer) buffer, 0);
        } else if (buffer instanceof DoubleBuffer) {
            return MemoryUtil.memAddress((DoubleBuffer) buffer, 0);
        }

        return -1;
    }

    @Override
    public ByteBuffer allocate(final int size) {
        final Long address = MemoryUtil.nmemAlloc(size);
        final ByteBuffer byteBuffer = MemoryUtil.memByteBuffer(address, size);
        DEALLOCATORS.put(address, new Deallocator(byteBuffer, DUMMY_QUEUE, address));
        return byteBuffer;
    }
}
