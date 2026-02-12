package com.jme3.util.natives;

public interface DisposableReference {

    void destroy();

    void refresh();

    void addDependent(DisposableReference reference);

    boolean isDestroyed();

}
