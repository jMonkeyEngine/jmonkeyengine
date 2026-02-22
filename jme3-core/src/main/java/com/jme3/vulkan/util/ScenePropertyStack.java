package com.jme3.vulkan.util;

import com.jme3.scene.Spatial;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Function;

public class ScenePropertyStack <T> implements SceneProperty<T> {

    private final Function<Spatial, T> propertyFetch;
    private final Deque<T> stack = new ArrayDeque<>();
    private final T initial, inherit;

    public ScenePropertyStack(T initial, T inherit, Function<Spatial, T> propertyFetch) {
        this.propertyFetch = propertyFetch;
        this.initial = initial;
        this.inherit = inherit;
    }

    @Override
    public void push(Spatial spatial) {
        T val = propertyFetch.apply(spatial);
        if (val == null || val.equals(inherit)) {
            val = stack.isEmpty() ? initial : stack.peek();
        }
        stack.push(val);
    }

    @Override
    public void pop() {
        stack.pop();
    }

    @Override
    public T peek() {
        return stack.peek();
    }

    public T getInitial() {
        return initial;
    }

    public T getInherit() {
        return inherit;
    }

}
