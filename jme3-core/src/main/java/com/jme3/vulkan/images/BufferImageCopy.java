package com.jme3.vulkan.images;

import com.jme3.math.Vector2i;
import com.jme3.math.Vector3i;
import com.jme3.vulkan.images.newimage.EngineImage;
import com.jme3.vulkan.util.Flag;

import java.util.ArrayList;
import java.util.Collection;

public class BufferImageCopy {

    private final Collection<Region> regions = new ArrayList<>();

    public BufferImageCopy add(int bufferOffset, Vector2i bufferTexels, Vector3i imageOffset, Vector3i imageSize, int imageMipLevel, int imageBaseLayer, int imageLayerCount, Flag<EngineImage.Aspect> aspects) {
        regions.add(new Region(bufferOffset, bufferTexels, imageOffset, imageSize, imageMipLevel, imageBaseLayer, imageLayerCount, aspects));
        return this;
    }

    public BufferImageCopy add(int bufferOffset, Vector3i imageOffset, Vector3i imageSize, int imageMipLevel, int imageBaseLayer, int imageLayerCount, Flag<EngineImage.Aspect> aspects) {
        // imageOffset = zero = tightly packed buffer
        regions.add(new Region(bufferOffset, Vector2i.ZERO, imageOffset, imageSize, imageMipLevel, imageBaseLayer, imageLayerCount, aspects));
        return this;
    }

    public BufferImageCopy add(int bufferOffset, Vector3i imageSize, Flag<EngineImage.Aspect> aspects) {
        regions.add(new Region(bufferOffset, Vector2i.ZERO, Vector3i.ZERO, imageSize, 0, 0, 1, aspects));
        return this;
    }

    public BufferImageCopy add(int bufferOffset, Vector2i bufferTexels, Vector3i imageSize, Flag<EngineImage.Aspect> aspects) {
        regions.add(new Region(bufferOffset, bufferTexels, Vector3i.ZERO, imageSize, 0, 0, 1, aspects));
        return this;
    }

    public Collection<Region> getRegions() {
        return regions;
    }

    public static class Region {

        private final int bufferOffset;
        private final Vector2i bufferTexels;
        private final Vector3i imageOffset, imageSize;
        private final int imageMipLevel, imageBaseLayer, imageLayerCount;
        private final Flag<EngineImage.Aspect> aspects;

        public Region(int bufferOffset, Vector2i bufferTexels, Vector3i imageOffset, Vector3i imageSize, int imageMipLevel, int imageBaseLayer, int imageLayerCount, Flag<EngineImage.Aspect> aspects) {
            this.bufferOffset = bufferOffset;
            this.bufferTexels = bufferTexels;
            this.imageOffset = imageOffset;
            this.imageSize = imageSize;
            this.imageMipLevel = imageMipLevel;
            this.imageBaseLayer = imageBaseLayer;
            this.imageLayerCount = imageLayerCount;
            this.aspects = aspects;
        }

        public int getBufferOffset() {
            return bufferOffset;
        }

        public Vector2i getBufferTexels() {
            return bufferTexels;
        }

        public Vector3i getImageOffset() {
            return imageOffset;
        }

        public Vector3i getImageSize() {
            return imageSize;
        }

        public int getImageMipLevel() {
            return imageMipLevel;
        }

        public int getImageBaseLayer() {
            return imageBaseLayer;
        }

        public int getImageLayerCount() {
            return imageLayerCount;
        }

        public Flag<EngineImage.Aspect> getAspects() {
            return aspects;
        }

    }

}
