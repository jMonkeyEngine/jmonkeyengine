package com.jme3.vulkan.frames;

import com.jme3.vulkan.data.PerFrameData;

import java.util.function.IntFunction;

public class UpdateFrameManager {

    private final UpdateFrame[] frames;
    private int currentFrame = 0;

    public UpdateFrameManager(int frames, IntFunction<UpdateFrame> factory) {
        this.frames = new UpdateFrame[frames];
        for (int i = 0; i < frames; i++) {
            this.frames[i] = factory.apply(i);
        }
    }

    public void update(float tpf) {
        frames[currentFrame].update(this, tpf);
        if (++currentFrame >= frames.length) {
            currentFrame = 0;
        }
    }

    public int getTotalFrames() {
        return frames.length;
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    public <T> PerFrameData<T> perFrame(IntFunction<T> generator) {
        return new PerFrameData<>(this, generator);
    }

}
