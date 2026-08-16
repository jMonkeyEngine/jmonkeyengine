package com.jme3.vulkan.images;

import com.jme3.math.Vector2i;
import com.jme3.math.IntVector;
import com.jme3.vulkan.images.newimage.EngineImage;
import com.jme3.vulkan.util.Flag;

import java.util.ArrayList;
import java.util.Collection;

public class ImageCopy {

    private final Collection<Region> regions = new ArrayList<>();

    public ImageCopy add(Vector2i srcOffset, Vector2i dstOffset, Vector2i size, int srcMipLevel, int dstMipLevel, int srcBaseLayer, int dstBaseLayer, int layerCount, Flag<EngineImage.Aspect> aspects) {
        regions.add(new Region(srcOffset.toVector3(0), dstOffset.toVector3(0), size.toVector3(1), srcMipLevel, dstMipLevel, srcBaseLayer, dstBaseLayer, layerCount, aspects));
        return this;
    }

    public ImageCopy add(int width, int height, Flag<EngineImage.Aspect> aspects) {
        regions.add(new Region(IntVector.ZERO, IntVector.ZERO, new IntVector(width, height, 1), 0, 0, 0, 0, 1, aspects));
        return this;
    }

    public ImageCopy add(Vector2i size, Flag<EngineImage.Aspect> aspects) {
        return add(size.x, size.y, aspects);
    }

    public ImageCopy add(IntVector srcOffset, IntVector dstOffset, IntVector size, int srcMipLevel, int dstMipLevel, int srcBaseLayer, int dstBaseLayer, int layerCount, Flag<EngineImage.Aspect> aspects) {
        regions.add(new Region(srcOffset, dstOffset, size, srcMipLevel, dstMipLevel, srcBaseLayer, dstBaseLayer, layerCount, aspects));
        return this;
    }

    public ImageCopy add(IntVector size, Flag<EngineImage.Aspect> aspects) {
        regions.add(new Region(IntVector.ZERO, IntVector.ZERO, size, 0, 0, 0, 0, 1, aspects));
        return this;
    }

    public ImageCopy add(int width, int height, int depth, Flag<EngineImage.Aspect> aspects) {
        return add(new IntVector(width, height, depth), aspects);
    }

    public Collection<Region> getRegions() {
        return regions;
    }

    public static class Region {

        private final IntVector srcOffset, dstOffset, size;
        private final int srcMipLevel, dstMipLevel, srcBaseLayer, dstBaseLayer, layerCount;
        private final Flag<EngineImage.Aspect> aspects;

        public Region(IntVector srcOffset, IntVector dstOffset, IntVector size, int srcMipLevel, int dstMipLevel, int srcBaseLayer, int dstBaseLayer, int layerCount, Flag<EngineImage.Aspect> aspects) {
            this.srcOffset = srcOffset;
            this.dstOffset = dstOffset;
            this.size = size;
            this.srcMipLevel = srcMipLevel;
            this.dstMipLevel = dstMipLevel;
            this.srcBaseLayer = srcBaseLayer;
            this.dstBaseLayer = dstBaseLayer;
            this.layerCount = layerCount;
            this.aspects = aspects;
        }

        public IntVector getSrcOffset() {
            return srcOffset;
        }

        public IntVector getDstOffset() {
            return dstOffset;
        }

        public IntVector getSize() {
            return size;
        }

        public int getSrcMipLevel() {
            return srcMipLevel;
        }

        public int getDstMipLevel() {
            return dstMipLevel;
        }

        public int getSrcBaseLayer() {
            return srcBaseLayer;
        }

        public int getDstBaseLayer() {
            return dstBaseLayer;
        }

        public int getLayerCount() {
            return layerCount;
        }

        public Flag<EngineImage.Aspect> getAspects() {
            return aspects;
        }

    }

}
