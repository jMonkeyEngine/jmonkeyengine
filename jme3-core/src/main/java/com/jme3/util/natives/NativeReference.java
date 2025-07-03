package com.jme3.util.natives;

public interface NativeReference {

    void destroy();

    void addDependent(NativeReference reference);

}
