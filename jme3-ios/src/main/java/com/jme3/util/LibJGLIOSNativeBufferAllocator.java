/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 */
package com.jme3.util;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ngengine.libjglios.core.LibJGLIOSBufferAllocator;


public final class LibJGLIOSNativeBufferAllocator implements BufferAllocator {
    private static final Logger LOGGER = Logger.getLogger(LibJGLIOSNativeBufferAllocator.class.getName());
    private static final ReferenceQueue<ByteBuffer> REFERENCE_QUEUE = new ReferenceQueue<>();
    private static final Map<Long, Deallocator> DEALLOCATORS = new ConcurrentHashMap<>();
    private static final Thread CLEAN_THREAD = new Thread(LibJGLIOSNativeBufferAllocator::freeCollectedBuffers);

    static {
        CLEAN_THREAD.setDaemon(true);
        CLEAN_THREAD.setName("libJGLIOS Native Buffer Deallocator");
        CLEAN_THREAD.start();
    }

    @Override
    public void destroyDirectBuffer(Buffer toBeDestroyed) {
        long address = LibJGLIOSBufferAllocator.baseAddress(toBeDestroyed);
        if (address == 0L) {
            LOGGER.log(Level.WARNING, "Not found address of the {0}", toBeDestroyed);
            return;
        }
        Deallocator deallocator = DEALLOCATORS.remove(address);
        if (deallocator == null) {
            LOGGER.log(Level.WARNING, "Not found a deallocator for address {0}", address);
            return;
        }
        deallocator.freeNow();
    }

    @Override
    public ByteBuffer allocate(int size) {
        ByteBuffer buffer = LibJGLIOSBufferAllocator.allocate(size);
        if (buffer == null) {
            throw new OutOfMemoryError("Could not allocate " + size + " bytes through libJGLIOS");
        }
        long address = LibJGLIOSBufferAllocator.baseAddress(buffer);
        if (address != 0L) {
            DEALLOCATORS.put(address, new Deallocator(buffer, address));
        }
        return buffer;
    }

    private static void freeCollectedBuffers() {
        for (;;) {
            try {
                Deallocator deallocator = (Deallocator) REFERENCE_QUEUE.remove();
                deallocator.freeNow();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable throwable) {
                LOGGER.log(Level.SEVERE, "Error deallocating direct buffer", throwable);
            }
        }
    }

    private static final class Deallocator extends PhantomReference<ByteBuffer> {
        private final long address;
        private final AtomicBoolean freed = new AtomicBoolean(false);

        private Deallocator(ByteBuffer referent, long address) {
            super(referent, REFERENCE_QUEUE);
            this.address = address;
        }

        private void freeNow() {
            if (!freed.compareAndSet(false, true)) {
                return;
            }
            DEALLOCATORS.remove(address, this);
            clear();
            LibJGLIOSBufferAllocator.freeAddress(address);
        }
    }
}
