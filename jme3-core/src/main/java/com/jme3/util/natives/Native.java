package com.jme3.util.natives;

import org.lwjgl.system.MemoryUtil;

public interface Native <T> extends AutoCloseable {

    T getNativeObject();

    Runnable createNativeDestroyer();

    void prematureNativeDestruction();

    NativeReference getNativeReference();

    @Override
    default void close() {
        getNativeReference().destroy();
    }

    static void set(NativeManager manager) {
        BasicNativeManager.setGlobalInstance(manager);
    }

    static NativeManager get() {
        return BasicNativeManager.getGlobalInstance();
    }

    static <T> T getObject(Native<T> n) {
        return n != null ? n.getNativeObject() : null;
    }

    static long getId(Native<Long> n) {
        return n != null ? n.getNativeObject() : MemoryUtil.NULL;
    }

}
