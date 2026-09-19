package com.jme3.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SaferAllocMemoryGuardTest {

    private static final long MIB = 1024L * 1024L;
    private static final long SECOND = 1_000_000_000L;

    @BeforeEach
    public void setUp() {
        SaferAllocMemoryGuard.resetStateForTests();
        SaferAllocMemoryGuard.setTestHooks(null, null, null);
    }

    @AfterEach
    public void tearDown() {
        SaferAllocMemoryGuard.resetStateForTests();
        SaferAllocMemoryGuard.setTestHooks(null, null, null);
    }

    @Test
    public void shouldAdaptBudgetUpThenDown() {
        AtomicLong now = new AtomicLong(0L);
        AtomicLong currentBytes = new AtomicLong();
        AtomicInteger gcCalls = new AtomicInteger(0);

        SaferAllocMemoryGuard.setTestHooks(currentBytes::get, now::get, gcCalls::incrementAndGet);

        long initialBudget = SaferAllocMemoryGuard.getSoftBudgetForTests();

        currentBytes.set((long) (initialBudget * 0.95f));
        SaferAllocMemoryGuard.beforeAlloc(0L);
        now.set(3L * SECOND);
        SaferAllocMemoryGuard.beforeAlloc(0L);
        now.set(6L * SECOND);
        SaferAllocMemoryGuard.beforeAlloc(0L);

        long grownBudget = SaferAllocMemoryGuard.getSoftBudgetForTests();
        assertTrue(grownBudget > initialBudget, "Expected budget to grow under sustained high pressure");
        assertEquals(0, gcCalls.get(), "Should not request GC while still below budget");

        currentBytes.set(0L);
        for (int i = 0; i < 8; i++) {
            now.addAndGet(3L * SECOND);
            SaferAllocMemoryGuard.beforeAlloc(0L);
        }

        long shrunkBudget = SaferAllocMemoryGuard.getSoftBudgetForTests();
        assertTrue(shrunkBudget < grownBudget, "Expected budget to shrink under sustained low pressure");
    }

    @Test
    public void shouldRequestGcOnBurstEvenWithAdaptiveBudget() {
        AtomicLong now = new AtomicLong(0L);
        AtomicLong currentBytes = new AtomicLong();
        AtomicInteger gcCalls = new AtomicInteger(0);

        SaferAllocMemoryGuard.setTestHooks(currentBytes::get, now::get, gcCalls::incrementAndGet);

        long initialBudget = SaferAllocMemoryGuard.getSoftBudgetForTests();

        currentBytes.set((long) (initialBudget * 0.95f));
        SaferAllocMemoryGuard.beforeAlloc(0L);
        now.set(3L * SECOND);
        SaferAllocMemoryGuard.beforeAlloc(0L);
        now.set(6L * SECOND);
        SaferAllocMemoryGuard.beforeAlloc(0L);

        long grownBudget = SaferAllocMemoryGuard.getSoftBudgetForTests();
        assertTrue(grownBudget > initialBudget);

        currentBytes.set(grownBudget + 64L * MIB);
        now.addAndGet(3L * SECOND);
        SaferAllocMemoryGuard.beforeAlloc(0L);

        assertTrue(gcCalls.get() >= 2, "Expected explicit GC request on over-budget burst");
    }

    @Test
    public void shouldRequestMaintenanceGcAfterSilence() {
        AtomicLong now = new AtomicLong(0L);
        AtomicLong currentBytes = new AtomicLong();
        AtomicInteger gcCalls = new AtomicInteger(0);

        SaferAllocMemoryGuard.setTestHooks(currentBytes::get, now::get, gcCalls::incrementAndGet);

        long budget = SaferAllocMemoryGuard.getSoftBudgetForTests();
        currentBytes.set((long) (budget * 0.50f));

        SaferAllocMemoryGuard.beforeAlloc(0L);
        assertEquals(0, gcCalls.get());

        now.set(61L * SECOND);
        SaferAllocMemoryGuard.beforeAlloc(0L);

        assertTrue(gcCalls.get() >= 2, "Expected maintenance GC after long silence under non-trivial usage");
    }

    @Test
    public void nativeWrapperAllocationsShouldParticipateInMemoryPressure() {
        AtomicLong now = new AtomicLong(2L * SECOND);
        AtomicLong currentBytes = new AtomicLong(SaferAllocMemoryGuard.getSoftBudgetForTests());
        AtomicInteger gcCalls = new AtomicInteger(0);

        SaferAllocMemoryGuard.setTestHooks(currentBytes::get, now::get, gcCalls::incrementAndGet);

        long pointer = SaferBufferAllocator.malloc(1L);
        SaferBufferAllocator.free(pointer);
        assertEquals(2, gcCalls.get(), "malloc should request GC through the guard");

        resetHooks(currentBytes, now, gcCalls);
        pointer = SaferBufferAllocator.calloc(1L, 1L);
        SaferBufferAllocator.free(pointer);
        assertEquals(2, gcCalls.get(), "calloc should request GC through the guard");

        resetHooks(currentBytes, now, gcCalls);
        pointer = SaferBufferAllocator.alignedAlloc(8L, 1L);
        SaferBufferAllocator.alignedFree(pointer);
        assertEquals(2, gcCalls.get(), "alignedAlloc should request GC through the guard");

        pointer = SaferBufferAllocator.malloc(1L);
        resetHooks(currentBytes, now, gcCalls);
        pointer = SaferBufferAllocator.realloc(pointer, 2L);
        SaferBufferAllocator.free(pointer);
        assertEquals(2, gcCalls.get(), "realloc should request GC through the guard");
    }

    @Test
    public void callocOverflowShouldNotWrapPressureAccounting() {
        assertThrows(OutOfMemoryError.class, () -> SaferBufferAllocator.calloc(Long.MAX_VALUE, 2L));
    }

    @Test
    public void concurrentGcRequestsShouldBeThrottledAtomically() throws InterruptedException {
        AtomicLong now = new AtomicLong(2L * SECOND);
        AtomicLong currentBytes = new AtomicLong(SaferAllocMemoryGuard.getSoftBudgetForTests() + 1L);
        AtomicInteger gcCalls = new AtomicInteger(0);
        int workers = 16;
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        List<Throwable> failures = new ArrayList<>();

        SaferAllocMemoryGuard.setTestHooks(currentBytes::get, now::get, gcCalls::incrementAndGet);

        for (int i = 0; i < workers; i++) {
            executor.execute(() -> {
                try {
                    ready.countDown();
                    start.await();
                    SaferAllocMemoryGuard.beforeAlloc(1L);
                } catch (Throwable throwable) {
                    synchronized (failures) {
                        failures.add(throwable);
                    }
                }
            });
        }

        assertTrue(ready.await(5L, TimeUnit.SECONDS));
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5L, TimeUnit.SECONDS));
        assertTrue(failures.isEmpty(), "Worker failures: " + failures);
        assertEquals(2, gcCalls.get(), "Only one thread should win the GC request interval");
    }

    @Test
    public void adaptiveShrinkShouldObservePostFreeAllocatedBytes() {
        AtomicLong now = new AtomicLong(0L);
        AtomicLong currentBytes = new AtomicLong();
        AtomicInteger gcCalls = new AtomicInteger(0);

        SaferAllocMemoryGuard.setTestHooks(currentBytes::get, now::get, gcCalls::incrementAndGet);

        long initialBudget = SaferAllocMemoryGuard.getSoftBudgetForTests();
        currentBytes.set((long) (initialBudget * 0.95f));
        SaferAllocMemoryGuard.beforeAlloc(0L);
        now.set(3L * SECOND);
        SaferAllocMemoryGuard.beforeAlloc(0L);
        now.set(6L * SECOND);
        SaferAllocMemoryGuard.beforeAlloc(0L);
        long grownBudget = SaferAllocMemoryGuard.getSoftBudgetForTests();
        assertTrue(grownBudget > initialBudget);

        currentBytes.set(0L);
        for (int i = 0; i < 8; i++) {
            now.addAndGet(3L * SECOND);
            SaferAllocMemoryGuard.notifyGC();
        }

        assertTrue(SaferAllocMemoryGuard.getSoftBudgetForTests() < grownBudget,
                "Post-free notifications should allow the adaptive budget to shrink");
    }

    @Test
    public void monotonicTimeHookShouldDriveElapsedIntervalsDeterministically() {
        AtomicLong now = new AtomicLong(999_000_000L);
        AtomicLong currentBytes = new AtomicLong(SaferAllocMemoryGuard.getSoftBudgetForTests() + 1L);
        AtomicInteger gcCalls = new AtomicInteger(0);

        SaferAllocMemoryGuard.setTestHooks(currentBytes::get, now::get, gcCalls::incrementAndGet);

        SaferAllocMemoryGuard.beforeAlloc(1L);
        assertEquals(0, gcCalls.get());

        now.set(1_000_000_000L);
        SaferAllocMemoryGuard.beforeAlloc(1L);
        assertEquals(2, gcCalls.get());
    }

    private static void resetHooks(AtomicLong currentBytes, AtomicLong now, AtomicInteger gcCalls) {
        SaferAllocMemoryGuard.resetStateForTests();
        now.set(2L * SECOND);
        gcCalls.set(0);
        SaferAllocMemoryGuard.setTestHooks(currentBytes::get, now::get, gcCalls::incrementAndGet);
    }
}
