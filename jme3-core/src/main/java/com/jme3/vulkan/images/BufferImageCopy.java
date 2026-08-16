package com.jme3.vulkan.images;

import com.jme3.math.Vector2i;
import com.jme3.math.IntVector;
import com.jme3.vulkan.images.newimage.EngineImage;
import com.jme3.vulkan.util.Flag;

import java.util.ArrayList;
import java.util.Collection;

public class BufferImageCopy {

    private final Collection<Region> regions = new ArrayList<>();

    public BufferImageCopy add(int bufferOffset, IntVector imageOffset, IntVector imageSize, int imageMipLevel, int imageBaseLayer, int imageLayerCount, Flag<EngineImage.Aspect> aspects) {
        // imageOffset = zero = tightly packed buffer
        regions.add(new Region(bufferOffset, IntVector.ZERO, imageOffset, imageSize, imageMipLevel, imageBaseLayer, imageLayerCount, aspects));
        return this;
    }

    public BufferImageCopy add(int bufferOffset, IntVector imageSize, Flag<EngineImage.Aspect> aspects) {
        regions.add(new Region(bufferOffset, IntVector.ZERO, IntVector.ZERO, imageSize, 0, 0, 1, aspects));
        return this;
    }

    public Collection<Region> getRegions() {
        return regions;
    }

    public static class Region {

        private final int bufferOffset;
        private final IntVector bufferTexels;
        private final IntVector imageOffset, imageSize;
        private final int imageMipLevel, imageBaseLayer, imageLayerCount;
        private final Flag<EngineImage.Aspect> aspects;

        public Region(int bufferOffset, IntVector bufferTexels, IntVector imageOffset, IntVector imageSize, int imageMipLevel, int imageBaseLayer, int imageLayerCount, Flag<EngineImage.Aspect> aspects) {
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

        public IntVector getBufferTexels() {
            return bufferTexels;
        }

        public IntVector getImageOffset() {
            return imageOffset;
        }

        public IntVector getImageSize() {
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
