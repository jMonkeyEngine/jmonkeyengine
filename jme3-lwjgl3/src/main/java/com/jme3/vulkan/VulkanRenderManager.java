package com.jme3.vulkan;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

public class VulkanRenderManager {

    private final IntFunction<Runnable> frameFactory;
    private final List<Runnable> frames = new ArrayList<>();
    private int currentFrame = 0;

    public VulkanRenderManager(int frames, IntFunction<Runnable> frameFactory) {
        this.frameFactory = frameFactory;
        setFrames(frames);
    }

    public void render(float tpf) {
        frames.get(currentFrame).run();
        if (++currentFrame >= frames.size()) {
            currentFrame = 0;
        }
    }

    public void setFrames(int n) {
        while (n > frames.size()) {
            frames.add(frameFactory.apply(frames.size()));
        }
        while (n < frames.size()) {
            frames.remove(frames.size() - 1);
        }
    }

}
