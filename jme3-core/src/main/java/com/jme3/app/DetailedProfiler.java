package com.jme3.app;

import com.jme3.profile.*;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;

import java.util.*;

/**
 * Created by Nehon on 25/01/2017.
 */
public class DetailedProfiler implements AppProfiler {

    private final static int MAX_FRAMES = 100;
    private Map<String, StatLine> data;
    private Map<String, StatLine> pool;
    private long startFrame;
    private static int currentFrame = 0;
    private String prevPath = null;
    private boolean frameEnded = false;
    private Renderer renderer;
    private boolean ongoingGpuProfiling = false;


    private String curAppPath = null;
    private String curVpPath = null;
    private String curSpPath = null;
    private VpStep lastVpStep = null;

    private StringBuilder path = new StringBuilder(256);
    private StringBuilder vpPath = new StringBuilder(256);

    private Deque<Integer> idsPool = new ArrayDeque<>(100);

    StatLine frameTime;


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

    private void closeFrame() {
        //close frame
        if (data != null) {

            prevPath = null;

            for (StatLine statLine : data.values()) {
                statLine.closeFrame();
            }
            currentFrame++;
        }
    }

    @Override
    public void vpStep(VpStep step, ViewPort vp, RenderQueue.Bucket bucket) {

        if (data != null) {
            vpPath.setLength(0);
            vpPath.append(vp.getName()).append("/").append((bucket == null ? step.name() : bucket.name() + " Bucket"));
            path.setLength(0);
            if ((lastVpStep == VpStep.PostQueue || lastVpStep == VpStep.PostFrame) && bucket != null) {
                path.append(curAppPath).append("/").append(curVpPath).append(curSpPath).append("/").append(vpPath);
                curVpPath = vpPath.toString();
            } else {
                if (bucket != null) {
                    path.append(curAppPath).append("/").append(curVpPath).append("/").append(bucket.name() + " Bucket");
                } else {
                    path.append(curAppPath).append("/").append(vpPath);
                    curVpPath = vpPath.toString();
                }
            }
            lastVpStep = step;

            addStep(path.toString(), System.nanoTime());
        }
    }

    @Override
    public void spStep(SpStep step, String... additionalInfo) {

        if (data != null) {
            curSpPath = getPath("", additionalInfo);
            path.setLength(0);
            path.append(curAppPath).append("/").append(curVpPath).append(curSpPath);
            addStep(path.toString(), System.nanoTime());
        }

    }

    public Map<String, StatLine> getStats() {
        if (data != null) {
            return data;//new LinkedHashMap<>(data);
        }
        return null;
    }

    public double getAverageFrameTime() {
        return frameTime.getAverageCpu();
    }


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
        StringBuilder path = new StringBuilder(step);
        if (subPath != null) {
            for (String s : subPath) {
                path.append("/").append(s);
            }
        }
        return path.toString();
    }

    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
        poolTaskIds(renderer);
    }

    private void poolTaskIds(Renderer renderer) {
        int[] ids = renderer.generateProfilingTasks(100);
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

    public static class StatLine {
        private long[] cpuTimes = new long[MAX_FRAMES];
        private long[] gpuTimes = new long[MAX_FRAMES];
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
            return (double) cpuSum / (double) Math.min(nbFramesCpu, MAX_FRAMES);
        }

        public double getAverageGpu() {
            if (nbFramesGpu == 0) {
                return 0;
            }

            return (double) gpuSum / (double) Math.min(nbFramesGpu, MAX_FRAMES);
        }
    }

}
