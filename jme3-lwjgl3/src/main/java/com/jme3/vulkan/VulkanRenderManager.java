package com.jme3.vulkan;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public class VulkanRenderManager {

    private final IntFunction<Consumer<Float>> frameFactory;
    private final List<Consumer<Float>> frames = new ArrayList<>();
    private int currentFrame = 0;

    public VulkanRenderManager(int frames, IntFunction<Consumer<Float>> frameFactory) {
        this.frameFactory = frameFactory;
        setFrames(frames);
    }

    public void render(float tpf) {
        frames.get(currentFrame).accept(tpf);
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
