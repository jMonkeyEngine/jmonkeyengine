package com.jme3.vulkan.commands;

import java.util.ArrayDeque;
import java.util.Deque;

public abstract class RenderSetting<T> {

    private final Deque<T> states = new ArrayDeque<>();
    private boolean updateNeeded = false;

    public void push(T value) {
        updateNeeded = updateNeeded || states.isEmpty() || !states.peek().equals(value);
        states.push(value);
    }

    public T peek() {
        return states.peek();
    }

    public T pop() {
        T current = states.pop();
        updateNeeded = updateNeeded || (!states.isEmpty() && !current.equals(states.peek()));
        return current;
    }

    public void apply() {
        if (updateNeeded) {
            apply(states.peek());
            updateNeeded = false;
        }
    }

    protected abstract void apply(T value);

}
