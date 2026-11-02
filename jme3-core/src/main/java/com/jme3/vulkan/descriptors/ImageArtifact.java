package com.jme3.vulkan.descriptors;

import java.util.Objects;

/**
 * Snapshot of an image view, sampler, and layout bound to an image descriptor.
 */
public final class ImageArtifact {

    private final long view, sampler;
    private final int layout;

    public ImageArtifact(long view, long sampler, int layout) {
        this.view = view;
        this.sampler = sampler;
        this.layout = layout;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ImageArtifact that = (ImageArtifact) o;
        return view == that.view && sampler == that.sampler && layout == that.layout;
    }

    @Override
    public int hashCode() {
        return Objects.hash(view, sampler, layout);
    }

    public long getView() {
        return view;
    }

    public long getSampler() {
        return sampler;
    }

    public int getLayout() {
        return layout;
    }

}
