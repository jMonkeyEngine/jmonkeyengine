package com.jme3.vulkan.frames;

import com.jme3.vulkan.update.Command;

import java.util.function.IntFunction;

public class UpdateFrameManager {

    private final UpdateFrame[] frames;
    private int currentFrame = 0;

    public UpdateFrameManager(int frames, IntFunction<UpdateFrame> generator) {
        this.frames = new UpdateFrame[frames];
        for (int i = 0; i < frames; i++) {
            this.frames[i] = generator.apply(i);
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

    public <T> PerFrameResource<T> perFrame(IntFunction<T> generator) {
        return new PerFrameResource<>(this, generator);
    }

    public <T extends Command> PerFrameCommand<T> perFrameCommand(IntFunction<T> generator) {
        return new PerFrameCommand<>(this, generator);
    }

}
