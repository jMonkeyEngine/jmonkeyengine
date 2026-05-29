package com.jme3.util;

import org.lwjgl.system.MemoryUtil;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.nio.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
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
     * Deprecated compatibility alias for {@link LWJGLBufferAllocator}.
     *
     * @author JavaSaBr
     * @deprecated {@link LWJGLBufferAllocator} is thread-safe for allocation bookkeeping.
     */
    @Deprecated
    public static class ConcurrentLWJGLBufferAllocator extends LWJGLBufferAllocator {
    }

    /**
     * The LWJGL byte buffer deallocator.
     */
    static class Deallocator extends PhantomReference<ByteBuffer> {

        /**
         * The address of LWJGL byte buffer.
         */
        final Long address;
        final AtomicBoolean freed = new AtomicBoolean(false);

        Deallocator(final ByteBuffer referent, final ReferenceQueue<? super ByteBuffer> queue, final Long address) {
            super(referent, queue);
            this.address = address;
        }

        /**
         * Retire this deallocator when the caller will free the memory itself.
         *
         * @return true if the caller now owns the native free
         */
        boolean retireForExternalFree() {
            if (!freed.compareAndSet(false, true)) {
                return false;
            }
            DEALLOCATORS.remove(address, this);
            clear();
            return true;
        }

        /**
         * Free memory.
         */
        void free() {
            if (!freed.compareAndSet(false, true)) {
                return;
            }
            DEALLOCATORS.remove(address, this);
            clear();
            MemoryUtil.nmemFree(address);
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
        for (;;) {
            try {
                final Deallocator deallocator = (Deallocator) DUMMY_QUEUE.remove();
                deallocator.free();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (final Throwable throwable) {
                LOGGER.log(Level.SEVERE, "Error deallocating direct buffer", throwable);
            }
        }
    }

    @Override
    public void destroyDirectBuffer(final Buffer buffer) {

        final long address = getAddress(buffer);

        if (address == -1) {
            LOGGER.log(Level.WARNING, "Not found address of the {0}", buffer);
            return;
        }

        final Deallocator deallocator = DEALLOCATORS.remove(address);

        if (deallocator == null) {
            LOGGER.log(Level.WARNING, "Not found a deallocator for address {0}", address);
            return;
        }

        if (deallocator.retireForExternalFree()) {
            MemoryUtil.nmemFree(address);
        }
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
        final Long address = MemoryUtil.nmemCallocChecked(size, 1);
        final ByteBuffer byteBuffer = MemoryUtil.memByteBuffer(address, size);
        DEALLOCATORS.put(address, createDeallocator(address, byteBuffer));
        return byteBuffer;
    }

    Deallocator createDeallocator(final Long address, final ByteBuffer byteBuffer) {
        return new Deallocator(byteBuffer, DUMMY_QUEUE, address);
    }
}
