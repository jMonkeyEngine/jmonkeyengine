package com.jme3.util.natives;

public interface NativeReference {

    void destroy();

    void refresh();

    void addDependent(NativeReference reference);

    boolean isDestroyed();

}
