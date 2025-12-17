package com.jme3.util;

import org.lwjgl.system.MemoryStack;

public abstract class AbstractNativeBuilder <T> {

    protected final MemoryStack stack = MemoryStack.stackPush();

    public T build() {
        T obj = construct();
        stack.pop();
        return obj;
    }

    protected abstract T construct();

}
