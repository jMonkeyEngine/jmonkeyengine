package com.jme3.util.natives;

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

}
