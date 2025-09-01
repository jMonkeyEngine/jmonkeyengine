package com.jme3.vulkan.data;

import com.jme3.vulkan.frames.UpdateFrameManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.IntFunction;

public class PerFrameData <T> implements DataPipe<T> {

    private final UpdateFrameManager frames;
    private final ArrayList<T> data = new ArrayList<>();

    @SafeVarargs
    public PerFrameData(UpdateFrameManager frames, T... data) {
        this.frames = frames;
        Collections.addAll(this.data, data);
        if (this.data.size() != frames.getTotalFrames()) {
            throw new IllegalArgumentException("Must have one data entry per frame.");
        }
    }

    public PerFrameData(UpdateFrameManager frames, IntFunction<T> generator) {
        this.frames = frames;
        for (int i = 0; i < frames.getTotalFrames(); i++) {
            data.add(generator.apply(i));
        }
    }

    @Override
    public T execute() {
        return data.get(frames.getCurrentFrame());
    }

    private void verifyFrameInBounds(int frame) {
        if (frame >= frames.getTotalFrames()) {
            throw new IndexOutOfBoundsException("Frame index " + frame + " out of bounds for " + frames.getTotalFrames() + " frames.");
        }
    }

    public void set(int frame, T data) {
        verifyFrameInBounds(frame);
        this.data.set(frame, data);
    }

    public void set(T data) {
        this.data.set(frames.getCurrentFrame(), data);
    }

    public T get(int frame) {
        verifyFrameInBounds(frame);
        return data.get(frame);
    }

    public T get() {
        return data.get(frames.getCurrentFrame());
    }

}
