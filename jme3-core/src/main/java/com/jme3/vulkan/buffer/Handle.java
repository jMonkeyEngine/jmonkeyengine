package com.jme3.vulkan.buffer;

import com.jme3.util.natives.Disposable;
import com.jme3.util.natives.DisposableManager;
import com.jme3.util.natives.DisposableReference;

public class Handle <T> implements Disposable {

    private final T handle;
    private final Runnable destroyer;
    private final DisposableReference ref;

    public Handle(T handle, Runnable destroyer) {
        this.handle = handle;
        this.destroyer = destroyer;
        ref = DisposableManager.reference(this);
    }

    @Override
    public Runnable createDestroyer() {
        return destroyer;
    }

    @Override
    public DisposableReference getReference() {
        return ref;
    }

    public T get() {
        return handle;
    }

}
