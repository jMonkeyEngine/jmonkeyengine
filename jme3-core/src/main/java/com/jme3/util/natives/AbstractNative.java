package com.jme3.util.natives;

public abstract class AbstractNative <T> implements Disposable {

    protected DisposableReference ref;
    protected T object;

    public T getNativeObject() {
        return object;
    }

    @Override
    public DisposableReference getReference() {
        return ref;
    }

}
