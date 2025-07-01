/*
 * Copyright (c) 2017-2025 jMonkeyEngine
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

import com.jme3.profile.AppProfiler;
import com.jme3.profile.AppStep;
import com.jme3.profile.SpStep;
import com.jme3.profile.VpStep;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A detailed profiler implementation that tracks CPU and GPU times for various
 * application, viewport, and scene processing steps. It provides per-frame
 * statistics and also maintains a history for average calculations.
 *
 * @author Nehon on 25/01/2017.
 */
public class DetailedProfiler implements AppProfiler {

    /**
     * The maximum number of frames to keep statistics for average calculations.
     */
    private static final int MAX_FRAMES = 100;
    /**
     * Stores the current frame's profiling data, mapping step paths to StatLine objects.
     * This map is cleared at the beginning of each frame.
     */
    private Map<String, StatLine> data;
    /**
     * A pool of StatLine objects, mapping step paths to StatLine objects.
     * This pool is used to reuse StatLine instances across frames to reduce garbage collection.
     */
    private Map<String, StatLine> pool;
    /**
     * Not currently used for timing, potentially a leftover from earlier implementation.
     */
    private long startFrame;
    /**
     * The current frame number. Increments with each completed frame.
     */
    private static int currentFrame = 0;
    /**
     * Stores the path of the previously processed profiling step within the current frame.
     * Used to calculate the duration of the previous step.
     */
    private String prevPath = null;
    /**
     * Flag indicating if the current frame has officially ended processing.
     */
    private boolean frameEnded = false;
    /**
     * The renderer instance used for GPU profiling.
     */
    private Renderer renderer;
    /**
     * Flag indicating if a GPU profiling task is currently ongoing (i.e., between startProfiling and stopProfiling).
     */
    private boolean ongoingGpuProfiling = false;
    /**
     * The path component for the current application step.
     */
    private String curAppPath = null;
    /**
     * The path component for the current viewport step.
     */
    private String curVpPath = null;
    /**
     * The path component for the current scene processing step.
     */
    private String curSpPath = null;
    /**
     * The last viewport step encountered. Used for path construction logic.
     */
    private VpStep lastVpStep = null;
    /**
     * StringBuilder used for constructing the full profiling path for app steps.
     */
    private final StringBuilder path = new StringBuilder(256);
    /**
     * StringBuilder used for constructing the viewport-specific profiling path.
     */
    private final StringBuilder vpPath = new StringBuilder(256);
    /**
     * A pool of available GPU profiling task IDs.
     */
    private final Deque<Integer> idsPool = new ArrayDeque<>(100);
    /**
     * StatLine object specifically for tracking the total frame time (CPU).
     */
    StatLine frameTime;


    /**
     * Records the time taken for various application-level steps.
     * This method is called at predefined points in the application lifecycle.
     *
     * @param step The application step being profiled.
     */
    @Override
    public void appStep(AppStep step) {

        curAppPath = step.name();

        if (step == AppStep.BeginFrame) {
            if (data == null) {
                data = new LinkedHashMap<>();
                pool = new HashMap<>();
                frameTime = new StatLine(currentFrame);
            }
            if (frameTime.isActive()) {
                frameTime.setValueCpu(System.nanoTime() - frameTime.getValueCpu());
                frameTime.closeFrame();

            }
            frameTime.setNewFrameValueCpu(System.nanoTime());

            frameEnded = false;
            for (StatLine statLine : data.values()) {
                for (Iterator<Integer> i = statLine.taskIds.iterator(); i.hasNext(); ) {
                    int id = i.next();
                    if (renderer.isTaskResultAvailable(id)) {
                        long val = renderer.getProfilingTime(id);
                        statLine.setValueGpu(val);
                        i.remove();
                        idsPool.push(id);
                    }
                }
            }
            data.clear();
        }

        if (data != null) {
            String path = getPath(step.name());
            if (step == AppStep.EndFrame) {
                if (frameEnded) {
                    return;
                }
                addStep(path, System.nanoTime());
                StatLine end = data.get(path);
                end.setValueCpu(System.nanoTime() - startFrame);
                frameEnded = true;
            } else {
                addStep(path, System.nanoTime());
            }
        }
        if (step == AppStep.EndFrame) {
            closeFrame();
        }
    }

    /**
     * Records the time taken for application sub-steps, providing additional hierarchical detail.
     *
     * @param additionalInfo Optional strings to further qualify the sub-step path.
     */
    @Override
    public void appSubStep(String... additionalInfo) {
        if (data != null) {
            path.setLength(0);
            path.append(curAppPath);
            appendSubPath(path, additionalInfo); // Reusing helper for sub-path append
            addStep(path.toString(), System.nanoTime());
        }
    }

    /**
     * Completes the profiling for the current frame.
     * This involves stopping any ongoing GPU profiling and closing all active StatLine entries.
     */
    private void closeFrame() {
        if (data != null) {
            if (ongoingGpuProfiling && renderer != null) {
                renderer.stopProfiling();
                ongoingGpuProfiling = false;
            }
            prevPath = null;

            for (StatLine statLine : data.values()) {
                statLine.closeFrame();
            }
            currentFrame++;
        }
    }

    /**
     * Records the time taken for viewport-specific steps, including rendering buckets.
     *
     * @param step   The viewport step being profiled.
     * @param vp     The ViewPort associated with this step.
     * @param bucket The render queue bucket, if applicable (null for non-bucket steps).
     */
    @Override
    public void vpStep(VpStep step, ViewPort vp, RenderQueue.Bucket bucket) {

        if (data != null) {
            vpPath.setLength(0);
            vpPath.append(vp.getName()).append("/");
            if (bucket == null) {
                vpPath.append(step.name());
            } else {
                vpPath.append(bucket.name()).append(" Bucket");
            }

            path.setLength(0);
            // Optimized path construction
            path.append(curAppPath).append("/");

            if ((lastVpStep == VpStep.PostQueue || lastVpStep == VpStep.PostFrame) && bucket != null) {
                path.append(curVpPath).append(curSpPath).append("/").append(vpPath);
                curVpPath = vpPath.toString();
            } else {
                if (bucket != null) {
                    path.append(curVpPath).append("/").append(bucket.name()).append(" Bucket");
                } else {
                    path.append(vpPath);
                    curVpPath = vpPath.toString();
                }
            }
            lastVpStep = step;

            addStep(path.toString(), System.nanoTime());
        }
    }

    /**
     * Records the time taken for scene processing steps, allowing for additional detail.
     *
     * @param step           The scene processing step being profiled.
     * @param additionalInfo Optional strings to further qualify the step path.
     */
    @Override
    public void spStep(SpStep step, String... additionalInfo) {
        if (data != null) {
            curSpPath = getPath("", additionalInfo);
            path.setLength(0);
            path.append(curAppPath).append("/").append(curVpPath).append(curSpPath);
            addStep(path.toString(), System.nanoTime());
        }
    }

    /**
     * Returns a map of the collected statistics for the current frame.
     * The keys are the hierarchical paths of the profiled steps, and the values
     * are {@link StatLine} objects containing CPU and GPU time data.
     *
     * @return A Map of StatLine objects, or null if profiling has not started.
     */
    public Map<String, StatLine> getStats() {
        return data;
    }

    /**
     * Calculates the average CPU time for the entire frame.
     *
     * @return The average frame CPU time in nanoseconds.
     */
    public double getAverageFrameTime() {
        return frameTime.getAverageCpu();
    }

    /**
     * Adds a new profiling step or updates an existing one with its start time.
     * Handles stopping the previous GPU profiling task and starting a new one.
     *
     * @param path  The hierarchical path of the profiling step.
     * @param value The system nano time when this step began.
     */
    private void addStep(String path, long value) {
        if (ongoingGpuProfiling && renderer != null) {
            renderer.stopProfiling();
            ongoingGpuProfiling = false;
        }

        if (prevPath != null) {
            StatLine prevLine = data.get(prevPath);
            if (prevLine != null) {
                prevLine.setValueCpu(value - prevLine.getValueCpu());
            }
        }

        StatLine line = pool.get(path);
        if (line == null) {
            line = new StatLine(currentFrame);
            pool.put(path, line);
        }
        data.put(path, line);
        line.setNewFrameValueCpu(value);
        if (renderer != null) {
            int id = getUnusedTaskId();
            line.taskIds.add(id);
            renderer.startProfiling(id);
        }
        ongoingGpuProfiling = true;
        prevPath = path;
    }

    private String getPath(String step, String... subPath) {
        StringBuilder sb = new StringBuilder(step);
        appendSubPath(sb, subPath);
        return sb.toString();
    }

    private void appendSubPath(StringBuilder pathBuilder, String... subPath) {
        if (subPath != null) {
            for (String s : subPath) {
                pathBuilder.append("/").append(s);
            }
        }
    }

    /**
     * Sets the {@link Renderer} instance to be used for GPU profiling.
     * This method should be called once the renderer is available.
     *
     * @param renderer The renderer instance.
     */
    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
        // Initialize the pool of GPU profiling task IDs
        poolTaskIds(renderer);
    }

    private void poolTaskIds(Renderer renderer) {
        int[] ids = renderer.generateProfilingTasks(MAX_FRAMES);
        for (int id : ids) {
            idsPool.push(id);
        }
    }

    private int getUnusedTaskId() {
        if (idsPool.isEmpty()) {
            poolTaskIds(renderer);
        }

        return idsPool.pop();
    }

    /**
     * Represents a single line of statistics for a profiled step,
     * tracking CPU and GPU times over a set number of frames.
     */
    public static class StatLine {

        private final long[] cpuTimes = new long[MAX_FRAMES];
        private final long[] gpuTimes = new long[MAX_FRAMES];
        private int startCursor = 0;
        private int cpuCursor = 0;
        private int gpuCursor = 0;
        private long cpuSum = 0;
        private long gpuSum = 0;
        private long lastValue = 0;
        private int nbFramesCpu;
        private int nbFramesGpu;
        List<Integer> taskIds = new ArrayList<>();


        private StatLine(int currentFrame) {
            startCursor = currentFrame % MAX_FRAMES;
            cpuCursor = startCursor;
            gpuCursor = startCursor;
        }

        private void setNewFrameValueCpu(long value) {
            int newCursor = currentFrame % MAX_FRAMES;
            if (nbFramesCpu == 0) {
                startCursor = newCursor;
            }
            cpuCursor = newCursor;
            lastValue = value;
        }

        private void setValueCpu(long val) {
            lastValue = val;
        }

        private long getValueCpu() {
            return lastValue;
        }

        private void closeFrame() {
            if (isActive()) {
                cpuSum -= cpuTimes[cpuCursor];
                cpuTimes[cpuCursor] = lastValue;
                cpuSum += lastValue;
                nbFramesCpu++;
            } else {
                nbFramesCpu = 0;
            }
        }

        public void setValueGpu(long value) {
            gpuSum -= gpuTimes[gpuCursor];
            gpuTimes[gpuCursor] = value;
            gpuSum += value;
            nbFramesGpu++;
            gpuCursor = (gpuCursor + 1) % MAX_FRAMES;
        }

        public boolean isActive() {
            return cpuCursor >= currentFrame % MAX_FRAMES - 1;
        }

        public double getAverageCpu() {
            if (nbFramesCpu == 0) {
                return 0;
            }
            return cpuSum / (double) Math.min(nbFramesCpu, MAX_FRAMES);
        }

        public double getAverageGpu() {
            if (nbFramesGpu == 0) {
                return 0;
            }

            return gpuSum / (double) Math.min(nbFramesGpu, MAX_FRAMES);
        }
    }

}
