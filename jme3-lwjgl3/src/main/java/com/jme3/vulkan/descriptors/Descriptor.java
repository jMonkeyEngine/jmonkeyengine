package com.jme3.vulkan.descriptors;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;

public class Descriptor implements Native<Long> {

    @Override
    public Long getNativeObject() {
        return 0L;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return null;
    }

    @Override
    public void prematureNativeDestruction() {

    }

    @Override
    public NativeReference getNativeReference() {
        return null;
    }

}
