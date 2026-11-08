/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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
package com.jme3.app;


import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A queued application task. Invocation executes the callable at most once;
 * completion and waiting are managed by {@link FutureTask}. Cancellation never
 * interrupts the executing thread, which may be an engine-owned render or physics
 * thread. An {@link Error} is recorded for waiting callers and rethrown by
 * {@link #invoke()}.
 *
 * @param <V> the result type
 * @author Matthew D. Hicks, lazloh
 */
public class AppTask<V> implements Future<V> {
    private static final Logger logger = Logger.getLogger(AppTask.class.getName());
    private final Callable<V> callable;
    private final FutureTask<V> future;

    /**
     * Creates a task without executing it.
     *
     * @param callable the non-null work to execute
     */
    public AppTask(Callable<V> callable) {
        this.callable = Objects.requireNonNull(callable, "callable");
        future = new FutureTask<>(() -> {
            try {
                return callable.call();
            } catch (Exception failure) {
                logger.logp(Level.SEVERE, getClass().toString(), "invoke()", "Exception", failure);
                throw failure;
            }
        }) {
            @Override
            protected void setException(Throwable failure) {
                super.setException(failure);
                // Preserve Error propagation to the engine's error handler after waking waiters.
                if (failure instanceof Error) {
                    throw (Error) failure;
                }
            }
        };
    }

    /**
     * Cancels the task without interrupting the thread that executes it.
     * A callable already running is allowed to finish, but its result is discarded.
     *
     * @param mayInterruptIfRunning ignored; engine-owned threads are never interrupted
     * @return true if the task was canceled, false if it was already completed or canceled
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(false);
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    @Override
    public V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    public Callable<V> getCallable() {
        return callable;
    }

    /**
     * Executes the callable on this thread, unless already invoked or canceled.
     *
     * @throws Error if the callable throws an Error; its failure is also available through get()
     */
    public void invoke() {
        future.run();
    }
}
