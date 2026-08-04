/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
package com.jme3.system.lwjgl;

import java.awt.EventQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

/**
 * Prevents an unbounded AWT canvas render loop from starving Swing's event
 * dispatch thread without imposing a fixed frame-rate limit.
 */
final class AwtEventQueueFairness {

    static final long PROBE_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(4);
    static final long EDT_STALL_NANOS = TimeUnit.MILLISECONDS.toNanos(2);
    static final long BACKOFF_NANOS = TimeUnit.MICROSECONDS.toNanos(100);

    private final LongSupplier nanoTime;
    private final Consumer<Runnable> edtExecutor;
    private final LongConsumer parker;
    private final AtomicBoolean probePending = new AtomicBoolean();

    private long lastProbeNanos = Long.MIN_VALUE;
    private volatile long pendingSinceNanos;

    AwtEventQueueFairness() {
        this(System::nanoTime, EventQueue::invokeLater, LockSupport::parkNanos);
    }

    AwtEventQueueFairness(LongSupplier nanoTime, Consumer<Runnable> edtExecutor,
            LongConsumer parker) {
        this.nanoTime = nanoTime;
        this.edtExecutor = edtExecutor;
        this.parker = parker;
    }

    static boolean isRequired(boolean vsyncPacedFrame, int frameRateLimit) {
        return !vsyncPacedFrame && frameRateLimit <= 0;
    }

    void afterFrame() {
        long now = nanoTime.getAsLong();
        if (probePending.get()) {
            if (now - pendingSinceNanos >= EDT_STALL_NANOS) {
                parker.accept(BACKOFF_NANOS);
            }
            return;
        }

        if (lastProbeNanos != Long.MIN_VALUE
                && now - lastProbeNanos < PROBE_INTERVAL_NANOS) {
            return;
        }

        if (probePending.compareAndSet(false, true)) {
            lastProbeNanos = now;
            pendingSinceNanos = now;
            try {
                edtExecutor.accept(() -> probePending.set(false));
            } catch (RuntimeException exception) {
                probePending.set(false);
            }
        }
    }
}
