package com.jme3.util.natives;

import java.util.function.Consumer;

public class NativeWrapper <T> extends AbstractNative<T> {

    private final Consumer<T> destroyer;

    public NativeWrapper(T object, Consumer<T> destroyer) {
        this.object = object;
        this.destroyer = destroyer;
        ref = Native.get().register(this);
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> destroyer.accept(object);
    }

}
