package com.jme3.util;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.ngengine.saferalloc.SaferAlloc;
import org.ngengine.saferalloc.SaferAllocFunctionPointers;
import org.ngengine.saferalloc.SaferAllocNative;

public final class SaferBufferAllocator implements BufferAllocator {
    private static final Logger logger = Logger.getLogger(SaferBufferAllocator.class.getName());
    private static final ReferenceQueue<ByteBuffer> refQueue = new ReferenceQueue<>();
    private static final ConcurrentHashMap<Long, AllocationRef> allocations = new ConcurrentHashMap<>();

    private static final Thread reaperThread = new Thread(SaferBufferAllocator::reapLoop,
            "Safer Deallocator");

    static {
        reaperThread.setDaemon(true);
        reaperThread.start();
    }

    public SaferBufferAllocator() {
        logger.info(getClass().getSimpleName() + " enabled!");
    }

    private static void reapLoop() {
        for (;;) {
            try {
                AllocationRef ref = (AllocationRef) refQueue.remove();
                SaferAllocMemoryGuard.notifyGC();
                ref.freeFromQueue();
            } catch (InterruptedException e) {
                return;
            } catch (Throwable t) {
                // Keep the reaper alive even if one cleanup fails.
                t.printStackTrace();
            }
        }
    }

    private static final class AllocationRef extends PhantomReference<ByteBuffer> {
        private final long address;
        private final AtomicBoolean retired = new AtomicBoolean(false);

        private AllocationRef(ByteBuffer referent, long address) {
            super(referent, refQueue);
            this.address = address;
        }

        /**
         * Used when native realloc has already taken care of the old allocation. Removes tracking without
         * freeing again.
         */
        private void retireWithoutFree() {
            if (!retired.compareAndSet(false, true)) {
                return;
            }
            allocations.remove(address, this);
            clear();
        }

        /**
         * Explicit free or queued phantom cleanup.
         */
        private void freeNow() {
            if (!retired.compareAndSet(false, true)) {
                return;
            }

            boolean removed = allocations.remove(address, this);
            clear();

            if (removed) {
                SaferAlloc.free(address);
            }
        }

        private void freeFromQueue() {
            freeNow();
        }
    }

    private static ByteBuffer register(ByteBuffer buffer) {
        if (buffer == null) {
            return null;
        }

        long address = SaferAlloc.address(buffer);
        if (address == 0L) {
            throw new IllegalStateException("SaferAlloc returned null address for non-null buffer");
        }

        AllocationRef ref = new AllocationRef(buffer, address);
        AllocationRef previous = allocations.put(address, ref);

        if (previous != null) {
            // This should normally not happen unless the old allocation was already
            // logically retired (for example after realloc) or bookkeeping got out of sync.
            // Never free here: the address now belongs to the new allocation.
            previous.retireWithoutFree();
        }

        return buffer;
    }

    public static long getMallocFunctionPointer() {
        return SaferAllocFunctionPointers.malloc();
    }

    public static long getCallocFunctionPointer() {
        return SaferAllocFunctionPointers.calloc();
    }

    public static long getReallocFunctionPointer() {
        return SaferAllocFunctionPointers.realloc();
    }

    public static long getFreeFunctionPointer() {
        return SaferAllocFunctionPointers.free();
    }

    public static long getAlignedAllocFunctionPointer() {
        return SaferAllocFunctionPointers.alignedAlloc();
    }

    public static long getAlignedFreeFunctionPointer() {
        return SaferAllocFunctionPointers.alignedFree();
    }

    public static long malloc(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("size < 0");
        }
        long pointer = SaferAllocNative.malloc(size);
        if (pointer == 0L && size != 0) {
            throw new OutOfMemoryError("SaferAlloc malloc failed: " + size);
        }
        return pointer;
    }

    public static long calloc(long num, long size) {
        if (num < 0 || size < 0) {
            throw new IllegalArgumentException("num/size < 0");
        }
        if (num != 0 && size > Long.MAX_VALUE / num) {
            throw new OutOfMemoryError("calloc overflow");
        }

        long pointer = SaferAllocNative.calloc(num, size);
        if (pointer == 0L && (num * size) != 0) {
            throw new OutOfMemoryError("SaferAlloc calloc failed: " + num + "*" + size);
        }
        return pointer;
    }

    public static long realloc(long ptr, long size) {
        if (size < 0) {
            throw new IllegalArgumentException("size < 0");
        }
        long pointer = SaferAllocNative.realloc(ptr, size);
        if (pointer == 0L && size != 0) {
            throw new OutOfMemoryError("SaferAlloc realloc failed: " + size);
        }
        return pointer;
    }

    public static void free(long ptr) {
        if (ptr != 0L) {
            SaferAllocNative.free(ptr);
        }
    }

    public static long alignedAlloc(long alignment, long size) {
        if (alignment <= 0) {
            throw new IllegalArgumentException("alignment <= 0");
        }
        if (size < 0) {
            throw new IllegalArgumentException("size < 0");
        }
        long pointer = SaferAllocNative.mallocAligned(size, alignment);
        if (pointer == 0L && size != 0) {
            throw new OutOfMemoryError("SaferAlloc aligned_alloc failed: " + size);
        }
        return pointer;
    }

    public static void alignedFree(long ptr) {
        if (ptr != 0L) {
            SaferAllocNative.free(ptr);
        }
    }

    @Override
    public ByteBuffer allocate(int size) {
        SaferAllocMemoryGuard.beforeAlloc(size);
        return register(SaferAlloc.calloc(1, size));
    }

    @Override
    public void destroyDirectBuffer(Buffer buffer) {
        if (buffer == null) {
            return;
        }

        long address = SaferAlloc.address((ByteBuffer) buffer);
        AllocationRef ref = allocations.get(address);

        if (ref != null) {
            ref.freeNow();
        }
    }
}
