package com.jme3.vulkan.renderexp;

import com.jme3.scene.Geometry;
import com.jme3.vulkan.render.bucket.RenderElement;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public abstract class GeometryRenderBuffer <T extends RenderElement> {

    private final List<T> elements = new LinkedList<>();

    public abstract void add(Geometry g);

    public abstract void render(Consumer<T> callback);

    public void add(T e) {
        elements.add(e);
    }

    public void sort(Comparator<? super T> comparator) {
        elements.sort(comparator);
    }

    public void clear() {
        elements.clear();
    }

}
