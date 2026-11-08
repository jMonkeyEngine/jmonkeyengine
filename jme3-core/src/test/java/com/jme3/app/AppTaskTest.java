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
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AppTaskTest {

    @Test
    void completedNullCanBeReadWithZeroTimeout() throws Exception {
        AppTask<Object> task = new AppTask<>(() -> null);
        task.invoke();

        assertNull(task.get(0, TimeUnit.NANOSECONDS));
    }

    @Test
    void completedValueCanBeReadWithZeroTimeout() throws Exception {
        Object value = new Object();
        AppTask<Object> task = new AppTask<>(() -> value);
        task.invoke();

        assertSame(value, task.get(0, TimeUnit.NANOSECONDS));
    }

    @Test
    void unfinishedTaskTimesOutImmediately() {
        AppTask<Object> task = new AppTask<>(() -> null);

        assertThrows(TimeoutException.class, () -> task.get(0, TimeUnit.NANOSECONDS));
    }

    @Test
    void delayedNullCompletionWakesTimedWaiter() throws Exception {
        CountDownLatch callableStarted = new CountDownLatch(1);
        CountDownLatch allowCompletion = new CountDownLatch(1);
        CountDownLatch getterStarted = new CountDownLatch(1);
        AppTask<Object> task = new AppTask<>(() -> {
            callableStarted.countDown();
            allowCompletion.await();
            return null;
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> invocation = executor.submit(task::invoke);
            assertTrue(callableStarted.await(5, TimeUnit.SECONDS));
            Future<Object> getter = executor.submit(() -> {
                getterStarted.countDown();
                return task.get(5, TimeUnit.SECONDS);
            });
            assertTrue(getterStarted.await(5, TimeUnit.SECONDS));
            allowCompletion.countDown();

            assertNull(getter.get(5, TimeUnit.SECONDS));
            invocation.get(5, TimeUnit.SECONDS);
        } finally {
            allowCompletion.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void unfinishedTimedWaitPropagatesInterruption() {
        AppTask<Object> task = new AppTask<>(() -> null);
        try {
            Thread.currentThread().interrupt();
            assertThrows(InterruptedException.class, () -> task.get(5, TimeUnit.SECONDS));
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void timedGetPreservesCallableExceptionCause() {
        IllegalStateException cause = new IllegalStateException("expected");
        AppTask<Object> task = new AppTask<>(() -> {
            throw cause;
        });
        task.invoke();

        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> task.get(0, TimeUnit.NANOSECONDS));
        assertSame(cause, exception.getCause());
    }

    @Test
    void cancellationBeforeInvocationSkipsCallableAndTimedGetThrows() {
        AtomicBoolean callableInvoked = new AtomicBoolean();
        AppTask<Object> task = new AppTask<>(() -> {
            callableInvoked.set(true);
            return null;
        });

        assertTrue(task.cancel(false));
        assertTrue(task.isCancelled());
        assertTrue(task.isDone());
        assertThrows(CancellationException.class, task::get);
        assertThrows(CancellationException.class,
                () -> task.get(0, TimeUnit.NANOSECONDS));
        task.invoke();
        assertFalse(callableInvoked.get());
    }

    @Test
    void completedNullAndFailedTasksCannotBeCanceled() throws Exception {
        AppTask<Object> nullTask = new AppTask<>(() -> null);
        nullTask.invoke();
        assertFalse(nullTask.cancel(true));
        assertNull(nullTask.get());

        AppTask<Object> failedTask = new AppTask<>(() -> {
            throw new IllegalStateException("expected");
        });
        failedTask.invoke();
        assertFalse(failedTask.cancel(true));
        assertThrows(ExecutionException.class, failedTask::get);
    }

    @Test
    void cancelTrueDoesNotInterruptRunningCallable() throws Exception {
        assertRunningCancellationDoesNotInterrupt(true);
    }

    @Test
    void cancelFalseDoesNotInterruptRunningCallable() throws Exception {
        assertRunningCancellationDoesNotInterrupt(false);
    }

    private void assertRunningCancellationDoesNotInterrupt(boolean mayInterruptIfRunning)
            throws Exception {
        CountDownLatch callableStarted = new CountDownLatch(1);
        CountDownLatch allowCallableToFinish = new CountDownLatch(1);
        AtomicBoolean callableInterrupted = new AtomicBoolean();
        AppTask<Object> task = new AppTask<>(() -> {
            callableStarted.countDown();
            try {
                allowCallableToFinish.await();
            } catch (InterruptedException exception) {
                callableInterrupted.set(true);
            }
            return null;
        });
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> invocation = executor.submit(task::invoke);
            assertTrue(callableStarted.await(5, TimeUnit.SECONDS));

            assertTrue(task.cancel(mayInterruptIfRunning));
            assertTrue(task.isCancelled());
            assertTrue(task.isDone());
            assertThrows(CancellationException.class, task::get);
            assertThrows(CancellationException.class, () -> task.get(0, TimeUnit.NANOSECONDS));

            allowCallableToFinish.countDown();
            invocation.get(5, TimeUnit.SECONDS);
            assertFalse(callableInterrupted.get());
            assertThrows(CancellationException.class, task::get);
        } finally {
            allowCallableToFinish.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void invocationIsAtMostOnceWhenRepeatedConcurrentAndReentrant() throws Exception {
        AtomicInteger callCount = new AtomicInteger();
        AtomicReference<AppTask<Integer>> taskReference = new AtomicReference<>();
        CountDownLatch callableStarted = new CountDownLatch(1);
        CountDownLatch allowCallableToFinish = new CountDownLatch(1);
        AppTask<Integer> task = new AppTask<>(() -> {
            callCount.incrementAndGet();
            taskReference.get().invoke();
            callableStarted.countDown();
            allowCallableToFinish.await();
            return 42;
        });
        taskReference.set(task);

        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            Future<?> firstInvocation = executor.submit(task::invoke);
            assertTrue(callableStarted.await(5, TimeUnit.SECONDS));

            Future<?> secondInvocation = executor.submit(task::invoke);
            Future<?> thirdInvocation = executor.submit(task::invoke);
            secondInvocation.get(5, TimeUnit.SECONDS);
            thirdInvocation.get(5, TimeUnit.SECONDS);

            allowCallableToFinish.countDown();
            firstInvocation.get(5, TimeUnit.SECONDS);
            task.invoke();

            assertEquals(1, callCount.get());
            assertEquals(42, task.get());
        } finally {
            allowCallableToFinish.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void errorPropagatesFromInvokeAndRemainsAvailableFromGet() {
        AssertionError failure = new AssertionError("expected");
        AppTask<Object> task = new AppTask<>(() -> {
            throw failure;
        });

        AssertionError thrown = assertThrows(AssertionError.class, task::invoke);
        assertSame(failure, thrown);
        assertTrue(task.isDone());
        assertFalse(task.isCancelled());

        ExecutionException exception = assertThrows(ExecutionException.class, task::get);
        assertSame(failure, exception.getCause());
        assertFalse(task.cancel(true));

        task.invoke();
        assertTrue(task.isDone());
    }
}
