package com.jme3.util.natives;

public interface NativeHandle <T> extends Destructable {

    T getHandle();

}
