package com.jme3.vulkan.shader;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;
import org.lwjgl.system.Struct;
import org.lwjgl.system.StructBuffer;

import java.nio.ByteBuffer;

public class UniformTestStruct extends Struct<UniformTestStruct> {

    public static final int SIZEOF;
    public static final int ALIGNOF;

    public static final int X, Y, Z, W;

    static {
        Layout layout = __struct(
                __member(Float.BYTES),
                __member(Float.BYTES),
                __member(Float.BYTES),
                __member(Float.BYTES)
        );
        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();
        X = layout.offsetof(0);
        Y = layout.offsetof(1);
        Z = layout.offsetof(2);
        W = layout.offsetof(3);
    }

    public UniformTestStruct(ByteBuffer container) {
        this(MemoryUtil.memAddress(container), __checkContainer(container, SIZEOF));
    }

    protected UniformTestStruct(long address, ByteBuffer container) {
        super(address, container);
    }

    @Override
    protected UniformTestStruct create(long address, ByteBuffer container) {
        return new UniformTestStruct(address, container);
    }

    @Override
    public int sizeof() {
        return SIZEOF;
    }

    public float x() {
        return nx(address());
    }

    public void x(float x) {
        nx(address(), x);
    }

    public static float nx(long struct) {
        return MemoryUtil.memGetFloat(struct + X);
    }

    public static void nx(long struct, float x) {
        MemoryUtil.memPutFloat(struct + X, x);
    }

    public static UniformTestStruct calloc() {
        return new UniformTestStruct(MemoryUtil.nmemCallocChecked(1, SIZEOF), null);
    }

    public static class Buffer extends StructBuffer<UniformTestStruct, Buffer> implements NativeResource {

        private static final UniformTestStruct FACTORY = new UniformTestStruct(-1L, null);

        protected Buffer(ByteBuffer container, int remaining) {
            super(container, remaining);
        }

        protected Buffer(long address, ByteBuffer container, int mark, int position, int limit, int capacity) {
            super(address, container, mark, position, limit, capacity);
        }

        @Override
        protected UniformTestStruct getElementFactory() {
            return null;
        }

        @Override
        protected Buffer self() {
            return this;
        }

        @Override
        protected Buffer create(long address, ByteBuffer container, int mark, int position, int limit, int capacity) {
            return new Buffer(address, container, mark, position, limit, capacity);
        }

    }

}
