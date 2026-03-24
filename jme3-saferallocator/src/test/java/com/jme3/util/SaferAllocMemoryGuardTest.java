package com.jme3.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SaferAllocMemoryGuardTest {

    private static final long MIB = 1024L * 1024L;

    @Before
    public void setUp() {
        SaferAllocMemoryGuard.resetStateForTests();
        SaferAllocMemoryGuard.setTestHooks(null, null, null);
    }

    @After
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
        Assert.assertTrue("Expected budget to grow under sustained high pressure", grownBudget > initialBudget);
        Assert.assertEquals("Should not request GC while still below budget", 0, gcCalls.get());

        currentBytes.set(0L);
        for (int i = 0; i < 8; i++) {
            now.addAndGet(3_000L);
            SaferAllocMemoryGuard.beforeAlloc(0L);
        }

        long shrunkBudget = SaferAllocMemoryGuard.getSoftBudgetForTests();
        Assert.assertTrue("Expected budget to shrink under sustained low pressure", shrunkBudget < grownBudget);
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
        Assert.assertTrue(grownBudget > initialBudget);

        currentBytes.set(grownBudget + 64L * MIB);
        now.addAndGet(3_000L);
        SaferAllocMemoryGuard.beforeAlloc(0L);

        Assert.assertTrue("Expected explicit GC request on over-budget burst", gcCalls.get() >= 2);
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
        Assert.assertEquals(0, gcCalls.get());

        now.set(61_000L);
        SaferAllocMemoryGuard.beforeAlloc(0L);

        Assert.assertTrue("Expected maintenance GC after long silence under non-trivial usage", gcCalls.get() >= 2);
    }
}
