package com.jme3.renderer;

import com.jme3.export.*;

import java.io.IOException;
import java.util.Objects;

public class ViewPortArea implements Cloneable, Savable {

    private float width, height;
    private float left = 0;
    private float right = 1;
    private float top = 0;
    private float bottom = 1;
    private float minDepth = 0;
    private float maxDepth = 1;

    public ViewPortArea(ViewPortArea area) {
        set(area);
    }

    public ViewPortArea(float width, float height) {
        setSize(width, height);
    }

    public ViewPortArea(float width, float height, float left, float right, float top, float bottom) {
        setSize(width, height).setArea(left, right, top, bottom);
    }

    public ViewPortArea(float width, float height, float left, float right, float top, float bottom, float minDepth, float maxDepth) {
        set(width, height, left, right, top, bottom, minDepth, maxDepth);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(width, "width", 0f);
        out.write(height, "height", 0f);
        out.write(left, "left", 0f);
        out.write(right, "right", 1f);
        out.write(top, "top", 0f);
        out.write(bottom, "bottom", 1f);
        out.write(minDepth, "minDepth", 0f);
        out.write(maxDepth, "maxDepth", 1f);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        width = in.readFloat("width", 0f);
        height = in.readFloat("height", 0f);
        left = in.readFloat("left", 0f);
        right = in.readFloat("right", 1f);
        top = in.readFloat("top", 0f);
        bottom = in.readFloat("bottom", 1f);
        minDepth = in.readFloat("minDepth", 0f);
        maxDepth = in.readFloat("maxDepth", 1f);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ViewPortArea that = (ViewPortArea) o;
        return Float.compare(width, that.width) == 0
                && Float.compare(height, that.height) == 0
                && Float.compare(left, that.left) == 0
                && Float.compare(right, that.right) == 0
                && Float.compare(top, that.top) == 0
                && Float.compare(bottom, that.bottom) == 0
                && Float.compare(minDepth, that.minDepth) == 0
                && Float.compare(maxDepth, that.maxDepth) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height, left, right, top, bottom, minDepth, maxDepth);
    }

    @Override
    public ViewPortArea clone() {
        try {
            return (ViewPortArea)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    public ScissorArea toScissor(ScissorArea store) {
        return ScissorArea.storage(store).set(0, 0, (int)width, (int)height);
    }

    public ViewPortArea set(ViewPortArea area) {
        this.width = area.width;
        this.height = area.height;
        this.left = area.left;
        this.right = area.right;
        this.top = area.top;
        this.bottom = area.bottom;
        this.minDepth = area.minDepth;
        this.maxDepth = area.maxDepth;
        return this;
    }

    public ViewPortArea set(float width, float height, float left, float right, float top, float bottom, float minDepth, float maxDepth) {
        this.width = width;
        this.height = height;
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
        this.minDepth = minDepth;
        this.maxDepth = maxDepth;
        return this;
    }

    public ViewPortArea setSize(float width, float height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public ViewPortArea setArea(float left, float right, float top, float bottom) {
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
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

    public ViewPortArea setWidth(float width) {
        this.width = width;
        return this;
    }

    public ViewPortArea setHeight(float height) {
        this.height = height;
        return this;
    }

    public ViewPortArea setLeft(float left) {
        this.left = left;
        return this;
    }

    public ViewPortArea setRight(float right) {
        this.right = right;
        return this;
    }

    public ViewPortArea setTop(float top) {
        this.top = top;
        return this;
    }

    public ViewPortArea setBottom(float bottom) {
        this.bottom = bottom;
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

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getLeft() {
        return left;
    }

    public float getRight() {
        return right;
    }

    public float getTop() {
        return top;
    }

    public float getBottom() {
        return bottom;
    }

    public float getMinDepth() {
        return minDepth;
    }

    public float getMaxDepth() {
        return maxDepth;
    }

    public float getViewX() {
        return width * left;
    }

    public float getViewY() {
        return height * top;
    }

    public float getViewWidth() {
        return width * (right - left);
    }

    public float getViewHeight() {
        return height * (bottom - top);
    }

}
