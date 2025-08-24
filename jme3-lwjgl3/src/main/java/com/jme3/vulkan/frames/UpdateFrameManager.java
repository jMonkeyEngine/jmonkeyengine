package com.jme3.vulkan.frames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    public <T> VersionedResource<T> wrap(IntFunction<T> generator) {
        return new VersionPerFrame<>(generator);
    }

    public <T> VersionedResource<T> wrap(T resource) {
        return new SingleVersion<>(resource);
    }

    private class VersionPerFrame <T> implements VersionedResource<T> {

        private final List<T> versions;

        public VersionPerFrame(IntFunction<T> generator) {
            ArrayList<T> versionList = new ArrayList<>(getTotalFrames());
            for (int i = 0; i < getTotalFrames(); i++) {
                versionList.add(generator.apply(i));
            }
            versions = Collections.unmodifiableList(versionList);
        }

        @Override
        public T getVersion() {
            return getVersion(currentFrame);
        }

        @Override
        public T getVersion(int i) {
            return versions.get(i);
        }

        @Override
        public int getNumVersions() {
            return versions.size();
        }

        @Override
        public int getCurrentVersionIndex() {
            return currentFrame;
        }

    }

    private class SingleVersion <T> implements VersionedResource<T> {

        private final T version;

        public SingleVersion(T version) {
            this.version = version;
        }

        @Override
        public T getVersion() {
            return version;
        }

        @Override
        public T getVersion(int i) {
            return version;
        }

        @Override
        public int getNumVersions() {
            return 1;
        }

        @Override
        public int getCurrentVersionIndex() {
            return 0;
        }

    }

}
