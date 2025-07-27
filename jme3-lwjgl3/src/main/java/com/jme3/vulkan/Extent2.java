package com.jme3.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtent2D;

public final class Extent2 {

    public int x, y;

    public Extent2() {
        this(0, 0);
    }

    public Extent2(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Extent2(VkExtent2D vk) {
        this.x = vk.width();
        this.y = vk.height();
    }

    public Extent2 set(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Extent2 set(VkExtent2D vk) {
        this.x = vk.width();
        this.y = vk.height();
        return this;
    }

    public Extent2 setX(int x) {
        this.x = x;
        return this;
    }

    public Extent2 setY(int y) {
        this.y = y;
        return this;
    }

    public VkExtent2D toStruct(MemoryStack stack) {
        return VkExtent2D.malloc(stack).set(x, y);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

}
