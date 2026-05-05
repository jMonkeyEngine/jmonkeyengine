package com.jme3.util;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.saferalloc.SaferAlloc;
import org.ngengine.saferalloc.SaferAllocNative;

public class SaferAllocMemoryGuard {
    private static final Logger LOGGER = Logger.getLogger(SaferAllocMemoryGuard.class.getName());
    private static final String propertyPrefix = "saferalloc.";

    // saferalloc.softBudget.minBytes
    // Minimum adaptive soft budget in bytes.
    private static final long minSoftBudget = readLongProperty(
            propertyPrefix + "softBudget.minBytes",
            128L * 1024L * 1024L,
            1L,
            Long.MAX_VALUE
    );
    // saferalloc.softBudget.initialBytes
    // Initial soft budget in bytes. Clamped to at least softBudget.minBytes.
    private static final long initialSoftBudget = Math.max(
            minSoftBudget,
            readLongProperty(
                    propertyPrefix + "softBudget.initialBytes",
                    512L * 1024L * 1024L,
                    1L,
                    Long.MAX_VALUE
            )
    );

    // saferalloc.gc.intervalMillis
    // Minimum delay between explicit GC requests.
    private static final int gcIntervalMillis = readIntProperty(
            propertyPrefix + "gc.intervalMillis",
            1000,
            0,
            Integer.MAX_VALUE
    );
    // saferalloc.gc.maintenanceIntervalMillis
    // If > 0, request maintenance GC when this much time passes without explicit GC.
    private static final int maintenanceGcIntervalMillis = readIntProperty(
            propertyPrefix + "gc.maintenanceIntervalMillis",
            60_000,
            0,
            Integer.MAX_VALUE
    );
    // saferalloc.gc.maintenanceMinUsageRatio
    // Minimum current usage ratio (currentBytes / softBudget) needed before maintenance GC is considered.
    private static final float maintenanceGcMinUsageRatio = readFloatProperty(
            propertyPrefix + "gc.maintenanceMinUsageRatio",
            0.25f,
            0f,
            1f
    );

    // saferalloc.adapt.intervalMillis
    // Minimum delay between adaptive budget updates.
    private static final int adaptIntervalMillis = readIntProperty(
            propertyPrefix + "adapt.intervalMillis",
            2000,
            1,
            Integer.MAX_VALUE
    );
    // saferalloc.adapt.growWhenOverRatio
    // Mark high pressure when projected usage reaches/exceeds this ratio of soft budget.
    private static final float growWhenOverRatio = readFloatProperty(
            propertyPrefix + "adapt.growWhenOverRatio",
            0.90f,
            0f,
            Float.MAX_VALUE
    );
    // saferalloc.adapt.shrinkWhenUnderRatio
    // Mark low pressure when current usage is at/below this ratio of soft budget.
    private static final float shrinkWhenUnderRatio = readFloatProperty(
            propertyPrefix + "adapt.shrinkWhenUnderRatio",
            0.35f,
            0f,
            1f
    );
    // saferalloc.adapt.growTriggerCount
    // Consecutive high-pressure observations required before growing soft budget.
    private static final int growTriggerCount = readIntProperty(
            propertyPrefix + "adapt.growTriggerCount",
            3,
            1,
            Integer.MAX_VALUE
    );
    // saferalloc.adapt.shrinkTriggerCount
    // Consecutive low-pressure observations required before shrinking soft budget.
    private static final int shrinkTriggerCount = readIntProperty(
            propertyPrefix + "adapt.shrinkTriggerCount",
            8,
            1,
            Integer.MAX_VALUE
    );
    // saferalloc.adapt.growCurrentFactor
    // Growth factor applied to current soft budget when growing.
    private static final float growCurrentFactor = readFloatProperty(
            propertyPrefix + "adapt.growCurrentFactor",
            1.25f,
            1f,
            Float.MAX_VALUE
    );
    // saferalloc.adapt.growDemandFactor
    // Growth factor applied to projected usage when growing.
    private static final float growDemandFactor = readFloatProperty(
            propertyPrefix + "adapt.growDemandFactor",
            1.10f,
            1f,
            Float.MAX_VALUE
    );
    // saferalloc.adapt.shrinkFactor
    // Multiplicative factor applied to soft budget when shrinking.
    private static final float shrinkFactor = readFloatProperty(
            propertyPrefix + "adapt.shrinkFactor",
            0.90f,
            0f,
            1f
    );

    private static final AtomicLong softBudget = new AtomicLong(initialSoftBudget);
    private static final AtomicLong lastGCRun = new AtomicLong(0L);
    private static final AtomicLong lastAdaptUpdate = new AtomicLong(0L);
    private static final AtomicLong highPressureCount = new AtomicLong(0L);
    private static final AtomicLong lowPressureCount = new AtomicLong(0L);
    private static volatile LongSupplier allocatedBytesSupplier = SaferAlloc::currentAllocatedBytes;
    private static volatile LongSupplier nowSupplier = System::currentTimeMillis;
    private static volatile Runnable gcInvoker = System::gc;

    public static void beforeAlloc(long size){
        if (size < 0) return;
        long now = nowSupplier.getAsLong();
        long currentBytes = allocatedBytesSupplier.getAsLong();
        long currentSoftBudget = softBudget.get();
        long projectedBytes = safeAdd(currentBytes, size);

        adaptSoftBudget(currentBytes, projectedBytes, currentSoftBudget);
        currentSoftBudget = softBudget.get();

        if(LOGGER.isLoggable(Level.FINER)){
            float softBudgetPercent = projectedBytes / (float)currentSoftBudget * 100f;
            LOGGER.log(Level.FINER, "\n"+
                "       Requested " + human(size) + "\n" +
                "       Currently allocated: " + human(currentBytes) + "\n" +
                "       Soft budget: " + human(currentSoftBudget) + "\n" +
                "       Soft budget used: " + String.format(Locale.ROOT, "%.2f %%", softBudgetPercent)
            );
        }

        if (projectedBytes > currentSoftBudget) {
            requestGC(now);
            return;
        }

        if (maintenanceGcIntervalMillis > 0) {
            long minimumBytesForMaintenanceGC = (long) (currentSoftBudget * maintenanceGcMinUsageRatio);
            if (currentBytes >= minimumBytesForMaintenanceGC) {
                long last = lastGCRun.get();
                if (now - last >= maintenanceGcIntervalMillis) {
                    requestGC(now);
                }
            }
        }
    }

    public static void notifyGC(){
        long now = nowSupplier.getAsLong();
        long currentBytes = allocatedBytesSupplier.getAsLong();
        lastGCRun.set(now);
        adaptSoftBudget(currentBytes, currentBytes, softBudget.get());
    }

    private static void adaptSoftBudget(long currentBytes, long projectedBytes, long currentSoftBudget) {
        if (projectedBytes >= (long)(currentSoftBudget * growWhenOverRatio)) {
            highPressureCount.incrementAndGet();
            lowPressureCount.set(0L);
        } else if (currentBytes <= (long)(currentSoftBudget * shrinkWhenUnderRatio)) {
            lowPressureCount.incrementAndGet();
            highPressureCount.set(0L);
        } else {
            highPressureCount.set(0L);
            lowPressureCount.set(0L);
        }

        long now = nowSupplier.getAsLong();
        long lastUpdate = lastAdaptUpdate.get();
        if (now - lastUpdate < adaptIntervalMillis) {
            return;
        }
        if (!lastAdaptUpdate.compareAndSet(lastUpdate, now)) {
            return;
        }

        long highs = highPressureCount.get();
        if (highs >= growTriggerCount) {
            long grownFromCurrent = (long)(currentSoftBudget * growCurrentFactor);
            long grownFromDemand = (long)(projectedBytes * growDemandFactor);
            long newBudget = clampBudget(Math.max(grownFromCurrent, grownFromDemand));
            if (newBudget > currentSoftBudget && softBudget.compareAndSet(currentSoftBudget, newBudget)) {
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.log(Level.FINER, ">>> Adaptive soft budget up: {0} -> {1}",
                            new Object[]{human(currentSoftBudget), human(newBudget)});
                }
            }
            highPressureCount.set(0L);
            return;
        }

        long lows = lowPressureCount.get();
        if (lows >= shrinkTriggerCount) {
            long reduced = clampBudget((long)(currentSoftBudget * shrinkFactor));
            if (reduced < currentSoftBudget && softBudget.compareAndSet(currentSoftBudget, reduced)) {
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.log(Level.FINER, ">>> Adaptive soft budget down: {0} -> {1}",
                            new Object[]{human(currentSoftBudget), human(reduced)});
                }
            }
            lowPressureCount.set(0L);
        }
    }

    private static void requestGC(long now) {
        if (now - lastGCRun.get() >= gcIntervalMillis) {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.log(Level.FINER, "!!! Requesting GC...");
            }

            // Calling gc() twice is a common heuristic to increase the likelihood of a full
            // garbage collection cycle, which is important for timely release of native memory.
            gcInvoker.run();
            gcInvoker.run();

            lastGCRun.updateAndGet(v -> {
                if (v < now) return now;
                return v;
            });
        }
    }

    // Test-only hooks to keep unit tests deterministic without real native allocations or GC calls.
    static void setTestHooks(LongSupplier allocatedBytes, LongSupplier now, Runnable gcAction) {
        allocatedBytesSupplier = allocatedBytes != null ? allocatedBytes : SaferAllocNative::currentAllocatedBytes;
        nowSupplier = now != null ? now : System::currentTimeMillis;
        gcInvoker = gcAction != null ? gcAction : System::gc;
    }

    static void resetStateForTests() {
        softBudget.set(initialSoftBudget);
        lastGCRun.set(0L);
        lastAdaptUpdate.set(0L);
        highPressureCount.set(0L);
        lowPressureCount.set(0L);
    }

    static long getSoftBudgetForTests() {
        return softBudget.get();
    }

    private static long clampBudget(long candidate) {
        if (candidate < minSoftBudget) {
            return minSoftBudget;
        }
        return candidate;
    }

    private static long safeAdd(long a, long b) {
        if (b > 0 && a > Long.MAX_VALUE - b) {
            return Long.MAX_VALUE;
        }
        return a + b;
    }

    private static String human(long bytes) {
        if (bytes >= 1024L * 1024L * 1024L) {
            return String.format(Locale.ROOT, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
        if (bytes >= 1024L * 1024L) {
            return String.format(Locale.ROOT, "%.2f MB", bytes / (1024.0 * 1024.0));
        }
        if (bytes >= 1024L) {
            return String.format(Locale.ROOT, "%.2f KB", bytes / 1024.0);
        }
        return bytes + " B";
    }

    private static long readLongProperty(String key, long defaultValue, long minValue, long maxValue) {
        String raw = System.getProperty(key);
        if (raw == null || raw.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            long value = Long.parseLong(raw.trim());
            return clamp(value, minValue, maxValue);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid value for {0}: {1}. Using default {2}.",
                    new Object[]{key, raw, defaultValue});
            return defaultValue;
        }
    }

    private static int readIntProperty(String key, int defaultValue, int minValue, int maxValue) {
        String raw = System.getProperty(key);
        if (raw == null || raw.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return clamp(value, minValue, maxValue);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid value for {0}: {1}. Using default {2}.",
                    new Object[]{key, raw, defaultValue});
            return defaultValue;
        }
    }

    private static float readFloatProperty(String key, float defaultValue, float minValue, float maxValue) {
        String raw = System.getProperty(key);
        if (raw == null || raw.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            float value = Float.parseFloat(raw.trim());
            return clamp(value, minValue, maxValue);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid value for {0}: {1}. Using default {2}.",
                    new Object[]{key, raw, defaultValue});
            return defaultValue;
        }
    }

    private static long clamp(long value, long minValue, long maxValue) {
        if (value < minValue) {
            return minValue;
        }
        if (value > maxValue) {
            return maxValue;
        }
        return value;
    }

    private static int clamp(int value, int minValue, int maxValue) {
        if (value < minValue) {
            return minValue;
        }
        if (value > maxValue) {
            return maxValue;
        }
        return value;
    }

    private static float clamp(float value, float minValue, float maxValue) {
        if (value < minValue) {
            return minValue;
        }
        if (value > maxValue) {
            return maxValue;
        }
        return value;
    }
}
