package com.jme3.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SaferAllocMemoryGuardTest {

    private static final long MIB = 1024L * 1024L;

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
        now.set(3_000L);
        SaferAllocMemoryGuard.beforeAlloc(0L);
        now.set(6_000L);
        SaferAllocMemoryGuard.beforeAlloc(0L);

        long grownBudget = SaferAllocMemoryGuard.getSoftBudgetForTests();
        assertTrue(grownBudget > initialBudget, "Expected budget to grow under sustained high pressure");
        assertEquals(0, gcCalls.get(), "Should not request GC while still below budget");

        currentBytes.set(0L);
        for (int i = 0; i < 8; i++) {
            now.addAndGet(3_000L);
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
        now.set(3_000L);
        SaferAllocMemoryGuard.beforeAlloc(0L);
        now.set(6_000L);
        SaferAllocMemoryGuard.beforeAlloc(0L);

        long grownBudget = SaferAllocMemoryGuard.getSoftBudgetForTests();
        assertTrue(grownBudget > initialBudget);

        currentBytes.set(grownBudget + 64L * MIB);
        now.addAndGet(3_000L);
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

        now.set(61_000L);
        SaferAllocMemoryGuard.beforeAlloc(0L);

        assertTrue(gcCalls.get() >= 2, "Expected maintenance GC after long silence under non-trivial usage");
    }
}
