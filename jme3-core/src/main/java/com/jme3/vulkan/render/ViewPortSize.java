package com.jme3.vulkan.render;

import java.util.Objects;

public class ViewPortSize implements Cloneable {

    public float x, y, width, height;
    public float minDepth, maxDepth;

    public ViewPortSize(float x, float y, float width, float height) {
        this(x, y, width, height, 0f, 1f);
    }

    public ViewPortSize(float x, float y, float width, float height, float minDepth, float maxDepth) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.minDepth = minDepth;
        this.maxDepth = maxDepth;
    }

    public ViewPortSize(ViewPortSize size) {
        this(size.x, size.y, size.width, size.height, size.minDepth, size.maxDepth);
    }

    public ViewPortSize set(ViewPortSize size) {
        x = size.x;
        y = size.y;
        width = size.width;
        height = size.height;
        minDepth = size.minDepth;
        maxDepth = size.maxDepth;
        return this;
    }

    public ViewPortSize set(float x, float y, float width, float height, float minDepth, float maxDepth) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.minDepth = minDepth;
        this.maxDepth = maxDepth;
        return this;
    }

    public ViewPortSize set(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    public ViewPortSize setDepth(float min, float max) {
        this.minDepth = min;
        this.maxDepth = max;
        return this;
    }

    public ViewPortSize setX(float x) {
        this.x = x;
        return this;
    }

    public ViewPortSize setY(float y) {
        this.y = y;
        return this;
    }

    public ViewPortSize setWidth(float width) {
        this.width = width;
        return this;
    }

    public ViewPortSize setHeight(float height) {
        this.height = height;
        return this;
    }

    public ViewPortSize setMinDepth(float minDepth) {
        this.minDepth = minDepth;
        return this;
    }

    public ViewPortSize setMaxDepth(float maxDepth) {
        this.maxDepth = maxDepth;
        return this;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getMinDepth() {
        return minDepth;
    }

    public float getMaxDepth() {
        return maxDepth;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ViewPortSize that = (ViewPortSize) o;
        return Float.compare(x, that.x) == 0
                && Float.compare(y, that.y) == 0
                && Float.compare(width, that.width) == 0
                && Float.compare(height, that.height) == 0
                && Float.compare(minDepth, that.minDepth) == 0
                && Float.compare(maxDepth, that.maxDepth) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, width, height, minDepth, maxDepth);
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public static ViewPortSize copy(ViewPortSize src, ViewPortSize dst) {
        if (dst == null) {
            return new ViewPortSize(src);
        }
        return dst.set(src);
    }

}
