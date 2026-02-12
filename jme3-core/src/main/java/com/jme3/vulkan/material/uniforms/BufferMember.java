package com.jme3.vulkan.material.uniforms;

import com.jme3.vulkan.struct.StdLayoutType;

import java.nio.ByteBuffer;

public interface BufferMember <T> {

    void set(T value);

    T get();

    void write(ByteBuffer buffer);

    StdLayoutType getLayoutType();

    long getVersion();

}
