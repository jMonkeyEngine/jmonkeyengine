package com.jme3.vulkan.frames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntFunction;

public class VersionWrapper <T> implements VersionedResource<T> {

    private final UpdateFrameManager frames;
    private final List<T> versions;

    public VersionWrapper(UpdateFrameManager frames, IntFunction<T> generator) {
        this.frames = frames;
        ArrayList<T> versionList = new ArrayList<>(frames.getTotalFrames());
        for (int i = 0; i < frames.getTotalFrames(); i++) {
            versionList.add(generator.apply(i));
        }
        versions = Collections.unmodifiableList(versionList);
    }

    @Override
    public T getVersion() {
        return getVersion(frames.getCurrentFrame());
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
        return frames.getCurrentFrame();
    }

}
