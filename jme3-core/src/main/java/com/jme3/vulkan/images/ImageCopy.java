package com.jme3.vulkan.images;

import com.jme3.math.Vector2i;
import com.jme3.math.Vector3i;
import com.jme3.vulkan.util.Flag;

import java.util.ArrayList;
import java.util.Collection;

public class ImageCopy {

    private final Collection<Region> regions = new ArrayList<>();

    public ImageCopy add(Vector2i srcOffset, Vector2i dstOffset, Vector2i size, int srcMipLevel, int dstMipLevel, int srcBaseLayer, int dstBaseLayer, int layerCount, Flag<VulkanImage.Aspect> aspects) {
        regions.add(new Region(srcOffset.toVector3(0), dstOffset.toVector3(0), size.toVector3(1), srcMipLevel, dstMipLevel, srcBaseLayer, dstBaseLayer, layerCount, aspects));
        return this;
    }

    public ImageCopy add(int width, int height, Flag<VulkanImage.Aspect> aspects) {
        regions.add(new Region(Vector3i.ZERO, Vector3i.ZERO, new Vector3i(width, height, 1), 0, 0, 0, 0, 1, aspects));
        return this;
    }

    public ImageCopy add(Vector2i size, Flag<VulkanImage.Aspect> aspects) {
        return add(size.x, size.y, aspects);
    }

    public ImageCopy add(Vector3i srcOffset, Vector3i dstOffset, Vector3i size, int srcMipLevel, int dstMipLevel, int srcBaseLayer, int dstBaseLayer, int layerCount, Flag<VulkanImage.Aspect> aspects) {
        regions.add(new Region(srcOffset, dstOffset, size, srcMipLevel, dstMipLevel, srcBaseLayer, dstBaseLayer, layerCount, aspects));
        return this;
    }

    public ImageCopy add(Vector3i size, Flag<VulkanImage.Aspect> aspects) {
        regions.add(new Region(Vector3i.ZERO, Vector3i.ZERO, size, 0, 0, 0, 0, 1, aspects));
        return this;
    }

    public ImageCopy add(int width, int height, int depth, Flag<VulkanImage.Aspect> aspects) {
        return add(new Vector3i(width, height, depth), aspects);
    }

    public Collection<Region> getRegions() {
        return regions;
    }

    public static class Region {

        private final Vector3i srcOffset, dstOffset, size;
        private final int srcMipLevel, dstMipLevel, srcBaseLayer, dstBaseLayer, layerCount;
        private final Flag<VulkanImage.Aspect> aspects;

        public Region(Vector3i srcOffset, Vector3i dstOffset, Vector3i size, int srcMipLevel, int dstMipLevel, int srcBaseLayer, int dstBaseLayer, int layerCount, Flag<VulkanImage.Aspect> aspects) {
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

        public Vector3i getSrcOffset() {
            return srcOffset;
        }

        public Vector3i getDstOffset() {
            return dstOffset;
        }

        public Vector3i getSize() {
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

        public Flag<VulkanImage.Aspect> getAspects() {
            return aspects;
        }

    }

}
