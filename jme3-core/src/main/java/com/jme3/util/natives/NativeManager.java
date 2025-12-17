package com.jme3.util.natives;

public interface NativeManager {

    NativeReference register(Native object);

    int flush();

    int clear();

}
