package com.jme3.util.natives;

public interface DisposableManager {

    DisposableReference register(Disposable object);

    void flush();

    void clear();

    static DisposableManager get() {
        return BasicDisposableManager.getGlobalInstance();
    }

    static DisposableReference reference(Disposable object) {
        return BasicDisposableManager.getGlobalInstance().register(object);
    }

}
