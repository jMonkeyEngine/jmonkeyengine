package com.jme3.vulkan.frames;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

public class PerFrameResource <T> implements VersionedResource<T> {

    private final UpdateFrameManager frames;
    private final List<T> resources;

    public PerFrameResource(UpdateFrameManager frames, IntFunction<T> generator) {
        this.frames = frames;
        resources = new ArrayList<>(frames.getTotalFrames());
        for (int i = 0; i < frames.getTotalFrames(); i++) {
            resources.add(generator.apply(i));
        }
    }

    @Override
    public void set(T resource) {
        resources.set(frames.getCurrentFrame(), resource);
    }

    @Override
    public void set(int frame, T resource) {
        resources.set(frame, resource);
    }

    @Override
    public T get() {
        return resources.get(frames.getCurrentFrame());
    }

    @Override
    public T get(int frame) {
        return resources.get(frame);
    }

    @Override
    public int getNumResources() {
        return resources.size();
    }

}
