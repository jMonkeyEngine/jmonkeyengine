package com.jme3.vulkan.frames;

import com.jme3.vulkan.update.Command;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

public class UpdateFrameManager <T extends UpdateFrame> {

    private final List<T> frames = new ArrayList<>();
    private int currentFrame = 0;

    public UpdateFrameManager(int frames, IntFunction<T> generator) {
        for (int i = 0; i < frames; i++) {
            this.frames.add(generator.apply(i));
        }
    }

    public void update(float tpf) {
        frames.get(currentFrame).update(this, tpf);
        if (++currentFrame >= frames.size()) {
            currentFrame = 0;
        }
    }

    public int getTotalFrames() {
        return frames.size();
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    public T getFrame(int index) {
        return frames.get(index);
    }

    public <R> PerFrameResource<R> perFrame(IntFunction<R> generator) {
        return new PerFrameResource<>(this, generator);
    }

    public <C extends Command> PerFrameCommand<C> perFrameCommand(IntFunction<C> generator) {
        return new PerFrameCommand<>(this, generator);
    }

}
