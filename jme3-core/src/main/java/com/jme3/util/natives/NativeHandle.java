package com.jme3.util.natives;

import com.jme3.util.destruction.Destructable;

public interface NativeHandle <T> extends Destructable {

    T getHandle();

}
