package com.jme3.vulkan;

import org.lwjgl.system.Struct;

import java.nio.ByteBuffer;

public class Vertex extends Struct<Vertex> {

    // todo: experiment with implementing Structs

    protected Vertex(long address, ByteBuffer container) {
        super(address, container);
    }

    @Override
    protected Vertex create(long l, ByteBuffer byteBuffer) {
        return null;
    }

    @Override
    public int sizeof() {
        return 0;
    }

}
