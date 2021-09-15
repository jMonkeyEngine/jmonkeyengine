package com.jme3.util;

import org.lwjgl.system.MemoryUtil;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.nio.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;
import java.util.logging.Logger;

/**
 * The implementation of the {@link BufferAllocator} which use {@link MemoryUtil} to manage memory.
 *
 * @author JavaSaBr
 */
public class LWJGLBufferAllocator implements BufferAllocator {

    private static final Logger LOGGER = Logger.getLogger(LWJGLBufferAllocator.class.getName());

    public static final String PROPERTY_CONCURRENT_BUFFER_ALLOCATOR = "com.jme3.lwjgl3.ConcurrentBufferAllocator";
    
    /**
     * The reference queue.
     */
    private static final ReferenceQueue<Buffer> DUMMY_QUEUE = new ReferenceQueue<>();
        
    /**
     * The cleaner thread.
     */
    private static final Thread CLEAN_THREAD = new Thread(LWJGLBufferAllocator::freeByteBuffers);

    /**
     * The map with created deallocators.
     */
    private static final Map<Long, Deallocator> DEALLOCATORS = new ConcurrentHashMap<>();

    /**
     * Threadsafe implementation of the {@link LWJGLBufferAllocator}.
     *
     * @author JavaSaBr
     */
    public static class ConcurrentLWJGLBufferAllocator extends LWJGLBufferAllocator {

        /**
         * The synchronizer.
         */
        private final StampedLock stampedLock;

        public ConcurrentLWJGLBufferAllocator() {
            this.stampedLock = new StampedLock();
        }

        @Override
        public void destroyDirectBuffer(final Buffer buffer) {
            final long stamp = stampedLock.writeLock();
            try {
                super.destroyDirectBuffer(buffer);
            } finally {
                stampedLock.unlockWrite(stamp);
            }
        }

        @Override
        public ByteBuffer allocate(final int size) {
            final long stamp = stampedLock.writeLock();
            try {
                return super.allocate(size);
            } finally {
                stampedLock.unlockWrite(stamp);
            }
        }

        @Override
        Deallocator createDeallocator(final Long address, final ByteBuffer byteBuffer) {
            return new ConcurrentDeallocator(byteBuffer, DUMMY_QUEUE, address, stampedLock);
        }
    }

    /**
     * The LWJGL byte buffer deallocator.
     */
    static class Deallocator extends PhantomReference<ByteBuffer> {

        /**
         * The address of LWJGL byte buffer.
         */
        volatile Long address;

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
            freeMemory();
            DEALLOCATORS.remove(address);
        }

        void freeMemory() {
            MemoryUtil.nmemFree(address);
        }
    }

    /**
     * The LWJGL byte buffer deallocator.
     */
    static class ConcurrentDeallocator extends Deallocator {

        final StampedLock stampedLock;

        ConcurrentDeallocator(final ByteBuffer referent, final ReferenceQueue<? super ByteBuffer> queue,
                              final Long address, final StampedLock stampedLock) {
            super(referent, queue, address);
            this.stampedLock = stampedLock;
        }

        @Override
        protected void freeMemory() {
            final long stamp = stampedLock.writeLock();
            try {
                super.freeMemory();
            } finally {
                stampedLock.unlockWrite(stamp);
            }
        }
    }

    static {
        CLEAN_THREAD.setDaemon(true);
        CLEAN_THREAD.setName("LWJGL Deallocator");
        CLEAN_THREAD.start();
    }

    /**
     * Free unnecessary LWJGL byte buffers.
     */
    static void freeByteBuffers() {
        try {
            for (;;) {
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
    long getAddress(final Buffer buffer) {

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
        DEALLOCATORS.put(address, createDeallocator(address, byteBuffer));
        return byteBuffer;
    }

    Deallocator createDeallocator(final Long address, final ByteBuffer byteBuffer) {
        return new Deallocator(byteBuffer, DUMMY_QUEUE, address);
    }
}
