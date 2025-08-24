package com.jme3.vulkan;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import org.lwjgl.system.MemoryStack;

public abstract class AbstractNative<T> implements Native<T> {

    protected T object;
    protected NativeReference ref;

    @Override
    public T getNativeObject() {
        return object;
    }

    @Override
    public void prematureNativeDestruction() {}

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }

    public static abstract class Builder <T extends AbstractNative> implements AutoCloseable {

        protected final MemoryStack stack = MemoryStack.stackPush();

        @Override
        public void close() {
            build();
            stack.pop();
        }

        protected abstract void build();

    }

}
