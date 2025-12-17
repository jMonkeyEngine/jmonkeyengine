package com.jme3.vulkan.util;

import com.jme3.scene.Spatial;

import java.util.Stack;
import java.util.function.Function;

public class ScenePropertyStack <T> {

    private final Function<Spatial, T> propertyFetch;
    private final Stack<T> stack = new Stack<>();
    private final T main, inherit;

    public ScenePropertyStack(T main, T inherit, Function<Spatial, T> propertyFetch) {
        this.propertyFetch = propertyFetch;
        this.main = main;
        this.inherit = inherit;
    }

    public T push(Spatial spatial) {
        T val = propertyFetch.apply(spatial);
        if (val == null || val.equals(inherit)) {
            val = stack.isEmpty() ? main : stack.peek();
        }
        stack.push(val);
        return val;
    }

    public T pop() {
        return stack.pop();
    }

    public T peek() {
        return stack.peek();
    }

    public T getMain() {
        return main;
    }

    public T getInherit() {
        return inherit;
    }

}
