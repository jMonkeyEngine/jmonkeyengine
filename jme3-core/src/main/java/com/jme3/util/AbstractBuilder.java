package com.jme3.util;

import org.lwjgl.system.MemoryStack;

public abstract class AbstractBuilder implements AutoCloseable {

    protected final MemoryStack stack = MemoryStack.stackPush();

    @Override
    public void close() {
        build();
        stack.pop();
    }

    protected abstract void build();

}
