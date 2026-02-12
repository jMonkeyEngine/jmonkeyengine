package com.jme3.renderer;

import java.util.Objects;

public class ViewPortArea implements Cloneable {

    private float x, y, width, height;
    private float minDepth, maxDepth;

    public ViewPortArea() {
        minDepth = 0;
        maxDepth = 1;
    }

    public ViewPortArea(ViewPortArea area) {
        set(area);
    }

    public ViewPortArea(float x, float y, float width, float height) {
        set(x, y, width, height);
        minDepth = 0;
        maxDepth = 1;
    }

    public ViewPortArea(float x, float y, float width, float height, float minDepth, float maxDepth) {
        set(x, y, width, height, minDepth, maxDepth);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ViewPortArea that = (ViewPortArea) o;
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
    public ViewPortArea clone() {
        try {
            return (ViewPortArea)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public ScissorArea toScissor(ScissorArea store) {
        return ScissorArea.storage(store).set((int)x, (int)y, (int)width, (int)height);
    }

    public ViewPortArea set(ViewPortArea area) {
        this.x = area.x;
        this.y = area.y;
        this.width = area.width;
        this.height = area.height;
        this.minDepth = area.minDepth;
        this.maxDepth = area.maxDepth;
        return this;
    }

    public ViewPortArea set(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    public ViewPortArea set(float x, float y, float width, float height, float minDepth, float maxDepth) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.minDepth = minDepth;
        this.maxDepth = maxDepth;
        return this;
    }

    public ViewPortArea setDepthRange(float min, float max) {
        this.minDepth = min;
        this.maxDepth = max;
        return this;
    }

    public ViewPortArea toMinDepth() {
        maxDepth = minDepth;
        return this;
    }

    public ViewPortArea toMaxDepth() {
        minDepth = maxDepth;
        return this;
    }

    public ViewPortArea setX(float x) {
        this.x = x;
        return this;
    }

    public ViewPortArea setY(float y) {
        this.y = y;
        return this;
    }

    public ViewPortArea setWidth(float width) {
        this.width = width;
        return this;
    }

    public ViewPortArea setHeight(float height) {
        this.height = height;
        return this;
    }

    public ViewPortArea setMinDepth(float minDepth) {
        this.minDepth = minDepth;
        return this;
    }

    public ViewPortArea setMaxDepth(float maxDepth) {
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

    public static ViewPortArea storage(ViewPortArea store) {
        return store != null ? store : new ViewPortArea();
    }

}
