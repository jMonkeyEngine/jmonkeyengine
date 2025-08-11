package com.jme3.util.natives;

import org.lwjgl.system.NativeResource;

public interface NativeManager {

    NativeReference register(Native object);

    int flush();

    int clear();

}
