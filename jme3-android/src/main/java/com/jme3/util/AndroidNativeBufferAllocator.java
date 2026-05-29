/*
 * Copyright (c) 2009-2022 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

/**
 * Allocates and destroys direct byte buffers using native code.
 *
 * @author pavl_g.
 */
public final class AndroidNativeBufferAllocator implements BufferAllocator {
    private static final Logger LOGGER = Logger.getLogger(AndroidNativeBufferAllocator.class.getName());
    private static final ReferenceQueue<ByteBuffer> REFERENCE_QUEUE = new ReferenceQueue<>();
    private static final Map<Long, Deallocator> DEALLOCATORS = new ConcurrentHashMap<>();
    private static final Thread CLEAN_THREAD = new Thread(AndroidNativeBufferAllocator::freeCollectedBuffers);

    static {
        System.loadLibrary("bufferallocatorjme");
        CLEAN_THREAD.setDaemon(true);
        CLEAN_THREAD.setName("Android Native Buffer Deallocator");
        CLEAN_THREAD.start();
    }

    @Override
    public void destroyDirectBuffer(Buffer toBeDestroyed) {
        long address = directBufferAddress(toBeDestroyed);
        if (address == 0L) {
            LOGGER.log(Level.WARNING, "Not found address of the {0}", toBeDestroyed);
            return;
        }
        Deallocator deallocator = DEALLOCATORS.get(address);
        if (deallocator == null) {
            LOGGER.log(Level.WARNING, "Not found a deallocator for address {0}", address);
            return;
        }
        deallocator.freeNow();
    }

    @Override
    public ByteBuffer allocate(int size) {
        ByteBuffer buffer = createDirectByteBuffer(size);
        if (buffer != null) {
            long address = directBufferAddress(buffer);
            if (address != 0L) {
                DEALLOCATORS.put(address, new Deallocator(buffer, address));
            }
        }
        return buffer;
    }

    private static void freeCollectedBuffers() {
        try {
            for (;;) {
                Deallocator deallocator = (Deallocator) REFERENCE_QUEUE.remove();
                deallocator.freeNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
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
            releaseDirectByteBufferAddress(address);
        }
    }

    /**
     * Releases the memory of a direct buffer using its native address.
     *
     * @param address the native address to release
     * @see AndroidNativeBufferAllocator#destroyDirectBuffer(Buffer)
     */
    private static native void releaseDirectByteBufferAddress(long address);

    private static native long directBufferAddress(Buffer buffer);

    /**
     * Creates a new direct byte buffer explicitly with a specific size.
     *
     * @param size the byte buffer size used for allocating the buffer.
     * @return a new direct byte buffer object.
     * @see AndroidNativeBufferAllocator#allocate(int)
     */
    private native ByteBuffer createDirectByteBuffer(long size);
}