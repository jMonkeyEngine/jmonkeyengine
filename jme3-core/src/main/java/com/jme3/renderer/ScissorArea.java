package com.jme3.renderer;

import java.util.Objects;

public class ScissorArea implements Cloneable {

    private int x, y, width, height;

    public ScissorArea() {}

    public ScissorArea(ScissorArea area) {
        this(area.x, area.y, area.width, area.height);
    }

    public ScissorArea(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ScissorArea that = (ScissorArea) o;
        return x == that.x && y == that.y && width == that.width && height == that.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, width, height);
    }

    @Override
    public ScissorArea clone() {
        try {
            return (ScissorArea)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public ScissorArea set(ScissorArea area) {
        this.x = area.x;
        this.y = area.y;
        this.width = area.width;
        this.height = area.height;
        return this;
    }

    public ScissorArea set(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    public ScissorArea setX(int x) {
        this.x = x;
        return this;
    }

    public ScissorArea setY(int y) {
        this.y = y;
        return this;
    }

    public ScissorArea setWidth(int width) {
        this.width = width;
        return this;
    }

    public ScissorArea setHeight(int height) {
        this.height = height;
        return this;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public static ScissorArea storage(ScissorArea store) {
        return store != null ? store : new ScissorArea();
    }

}
