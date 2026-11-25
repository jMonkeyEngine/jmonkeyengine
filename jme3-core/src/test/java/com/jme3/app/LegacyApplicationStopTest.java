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
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package com.jme3.app;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.system.AppSettings;
import com.jme3.system.NullContext;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyApplicationStopTest {

    private Thread.UncaughtExceptionHandler originalUncaughtExceptionHandler;

    @BeforeEach
    void saveUncaughtExceptionHandler() {
        originalUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @AfterEach
    void restoreUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(originalUncaughtExceptionHandler);
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void stopTrueFromUpdateDoesNotDeadlock() throws InterruptedException {
        StopFromUpdateApplication application = new StopFromUpdateApplication();
        TestNullContext context = startApplication(application);

        boolean returned = application.stopReturned.await(5, TimeUnit.SECONDS);
        if (!returned) {
            dumpThreads();
            // Let a failing implementation unwind so the test process cannot leak its render thread.
            context.releaseWaiters();
        }
        context.destroy(true);

        assertTrue(returned, "stop(true) blocked the context's update thread");
        assertTrue(application.destroyed.await(5, TimeUnit.SECONDS),
                "context shutdown did not invoke Application.destroy()");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void stopTrueDuringInitializeDoesNotDeadlock() throws InterruptedException {
        StopDuringInitializeApplication application = new StopDuringInitializeApplication();
        TestNullContext context = startApplication(application);

        boolean returned = application.stopReturned.await(5, TimeUnit.SECONDS);
        if (!returned) {
            dumpThreads();
            context.releaseWaiters();
        }
        assertTrue(returned,
                "stop(true) blocked the context's initialize callback");
        context.destroy(true);
        assertTrue(application.destroyed.await(5, TimeUnit.SECONDS),
                "context shutdown did not invoke Application.destroy()");
    }

    @RepeatedTest(10)
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void externalStopWaitsForCleanupAndConcurrentRequestsComplete() throws InterruptedException {
        BlockingDestroyApplication application = new BlockingDestroyApplication();
        TestNullContext context = startApplication(application);

        Thread[] callers = new Thread[8];
        CountDownLatch callersDone = new CountDownLatch(callers.length);
        try {
            assertTrue(application.stopRequested.await(5, TimeUnit.SECONDS),
                    "stop(false) did not return from the update thread");
            assertTrue(application.destroyStarted.await(5, TimeUnit.SECONDS),
                    "context cleanup did not start");

            for (int i = 0; i < callers.length; i++) {
                callers[i] = new Thread(() -> {
                    application.stop(true);
                    callersDone.countDown();
                }, "external-stop-" + i);
                callers[i].start();
            }

            assertTrue(context.externalWaitStarted.await(5, TimeUnit.SECONDS),
                    "external stop requests did not enter the context wait");
            assertFalse(callersDone.await(100, TimeUnit.MILLISECONDS),
                    "external stop returned before cleanup completed");
            application.allowDestroy.countDown();

            assertTrue(callersDone.await(5, TimeUnit.SECONDS),
                    "concurrent external stop requests did not complete");
            assertTrue(application.destroyed.await(5, TimeUnit.SECONDS),
                    "Application.destroy() did not complete");

            // Repeated requests after teardown must be harmless and return promptly.
            application.stop(true);
        } finally {
            application.allowDestroy.countDown();
            for (Thread caller : callers) {
                if (caller != null) caller.join(1000);
            }
            context.destroy(false);
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void externalStopCompletesWhenApplicationCleanupThrows() throws InterruptedException {
        ThrowingDestroyApplication application = new ThrowingDestroyApplication();
        TestNullContext context = startApplication(application);

        assertTrue(application.stopRequested.await(5, TimeUnit.SECONDS),
                "stop(false) did not return from the update thread");
        context.destroy(true);

        assertTrue(application.destroyStarted.await(5, TimeUnit.SECONDS),
                "Application.destroy() was not invoked");
        assertFalse(context.isCreated(), "context remained created after cleanup threw");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void shutdownCompletesWhenInitializationThrows() throws InterruptedException {
        ThrowingInitializeApplication application = new ThrowingInitializeApplication();
        TestNullContext context = startApplication(application);

        context.destroy(true);

        assertTrue(application.destroyed.await(5, TimeUnit.SECONDS),
                "Application.destroy() was not invoked after initialization failed");
        assertFalse(context.isCreated(), "context remained created after initialization failed");
    }

    private static TestNullContext startApplication(LegacyApplication application) {
        AppSettings settings = new AppSettings(true);
        settings.setAudioRenderer(null);
        settings.setUseInput(false);

        application.setSettings(settings);
        application.setAssetManager(new DesktopAssetManager(true));

        TestNullContext context = new TestNullContext();
        context.setSettings(settings);
        context.setSystemListener(application);
        application.context = context;
        context.create(false);
        return context;
    }

    private static final class StopFromUpdateApplication extends LegacyApplication {

        private final CountDownLatch stopReturned = new CountDownLatch(1);
        private final CountDownLatch destroyed = new CountDownLatch(1);

        @Override
        public void update() {
            super.update();
            stop(true);
            stopReturned.countDown();
        }

        @Override
        public void destroy() {
            try {
                super.destroy();
            } finally {
                destroyed.countDown();
            }
        }
    }

    private static final class StopDuringInitializeApplication extends LegacyApplication {

        private final CountDownLatch stopReturned = new CountDownLatch(1);
        private final CountDownLatch destroyed = new CountDownLatch(1);

        @Override
        public void initialize() {
            super.initialize();
            stop(true);
            stopReturned.countDown();
        }

        @Override
        public void destroy() {
            try {
                super.destroy();
            } finally {
                destroyed.countDown();
            }
        }
    }

    private static final class BlockingDestroyApplication extends LegacyApplication {

        private final CountDownLatch stopRequested = new CountDownLatch(1);
        private final CountDownLatch destroyStarted = new CountDownLatch(1);
        private final CountDownLatch allowDestroy = new CountDownLatch(1);
        private final CountDownLatch destroyed = new CountDownLatch(1);

        @Override
        public void update() {
            super.update();
            stop(false);
            stopRequested.countDown();
        }

        @Override
        public void destroy() {
            destroyStarted.countDown();
            try {
                allowDestroy.await();
                super.destroy();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                destroyed.countDown();
            }
        }
    }

    private static final class ThrowingDestroyApplication extends LegacyApplication {

        private final CountDownLatch stopRequested = new CountDownLatch(1);
        private final CountDownLatch destroyStarted = new CountDownLatch(1);

        @Override
        public void update() {
            super.update();
            stop(false);
            stopRequested.countDown();
        }

        @Override
        public void destroy() {
            destroyStarted.countDown();
            throw new IllegalStateException("expected cleanup failure");
        }

        @Override
        public void handleError(String message, Throwable throwable) {
            // The test expects cleanup to fail and checks that shutdown still releases waiters.
        }
    }

    private static final class ThrowingInitializeApplication extends LegacyApplication {

        private final CountDownLatch destroyed = new CountDownLatch(1);

        @Override
        public void initialize() {
            throw new IllegalStateException("expected initialization failure");
        }

        @Override
        public void destroy() {
            try {
                super.destroy();
            } finally {
                destroyed.countDown();
            }
        }

        @Override
        public void handleError(String message, Throwable throwable) {
            // The test checks that failed initialization still reaches cleanup.
        }
    }

    private static final class TestNullContext extends NullContext {

        private final CountDownLatch externalWaitStarted = new CountDownLatch(8);

        @Override
        public void destroy(boolean waitFor) {
            if (waitFor && !isMainThread()) {
                externalWaitStarted.countDown();
            }
            super.destroy(waitFor);
        }

        void releaseWaiters() {
            synchronized (createdLock) {
                created.set(false);
                createdLock.notifyAll();
            }
        }
    }

    private static void dumpThreads() {
        for (ThreadInfo threadInfo : ManagementFactory.getThreadMXBean().dumpAllThreads(true, true)) {
            System.err.println(threadInfo);
        }
    }
}
