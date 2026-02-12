package com.jme3.vulkan.util;

import com.jme3.scene.Spatial;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Stack;
import java.util.function.Function;

public class ScenePropertyStack <T> implements SceneStack<T> {

    private final Function<Spatial, T> propertyFetch;
    private final Deque<T> stack = new ArrayDeque<>();
    private final T main, inherit;

    public ScenePropertyStack(T main, T inherit, Function<Spatial, T> propertyFetch) {
        this.propertyFetch = propertyFetch;
        this.main = main;
        this.inherit = inherit;
    }

    @Override
    public T push(Spatial spatial) {
        T val = propertyFetch.apply(spatial);
        if (val == null || val.equals(inherit)) {
            val = stack.isEmpty() ? main : stack.peek();
        }
        stack.push(val);
        return val;
    }

    @Override
    public T pop() {
        return stack.pop();
    }

    @Override
    public T peek() {
        return stack.peek();
    }

    @Override
    public void clear() {
        stack.clear();
    }

    public T getMain() {
        return main;
    }

    public T getInherit() {
        return inherit;
    }

}
