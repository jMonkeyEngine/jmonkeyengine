package com.jme3.vulkan.mesh;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Struct;
import org.lwjgl.system.StructBuffer;
import org.lwjgl.vulkan.VkViewport;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.*;

public class MeshTriangle extends Struct<MeshTriangle> {

    private static final int SIZEOF, ALIGNOF;
    private static final int[] INDICES = new int[3];

    static {
        Layout layout = __struct(
                __member(Integer.BYTES),
                __member(Integer.BYTES),
                __member(Integer.BYTES)
        );
        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();
        INDICES[0] = layout.offsetof(0);
        INDICES[1] = layout.offsetof(1);
        INDICES[2] = layout.offsetof(2);
    }

    protected MeshTriangle(long address, ByteBuffer container) {
        super(address, container);
    }

    @Override
    protected MeshTriangle create(long address, ByteBuffer container) {
        return new MeshTriangle(address, container);
    }

    @Override
    public int sizeof() {
        return SIZEOF;
    }

    public MeshTriangle index(int i, int index) {
        nindex(address, i, index);
        return this;
    }

    public int index(int i) {
        return nindex(address, i);
    }

    public static void nindex(long address, int i, int index) {
        memPutInt(address + INDICES[i], index);
    }

    public static int nindex(long address, int i) {
        return memGetInt(address + INDICES[i]);
    }

    public static MeshTriangle malloc() {
        return new MeshTriangle(nmemAllocChecked(SIZEOF), null);
    }

    public static MeshTriangle calloc() {
        return new MeshTriangle(nmemCallocChecked(1, SIZEOF), null);
    }

    public static MeshTriangle create() {
        ByteBuffer container = BufferUtils.createByteBuffer(SIZEOF);
        return new MeshTriangle(memAddress(container), container);
    }

    public static MeshTriangle create(long address) {
        return new MeshTriangle(address, null);
    }

    public static MeshTriangle createSafe(long address) {
        return address == NULL ? null : create(address);
    }

    public static Buffer malloc(int capacity) {
        return new Buffer(nmemAllocChecked(__checkMalloc(capacity, SIZEOF)), capacity);
    }

    public static Buffer calloc(int capacity) {
        return new Buffer(nmemCallocChecked(capacity, SIZEOF), capacity);
    }

    public static Buffer create(int capacity) {
        ByteBuffer container = __create(capacity, SIZEOF);
        return new Buffer(memAddress(container), container, -1, 0, capacity, capacity);
    }

    public static Buffer create(long address, int capacity) {
        return new Buffer(address, capacity);
    }

    public static Buffer createSafe(long address, int capacity) {
        return address == NULL ? null : new Buffer(address, capacity);
    }

    public static MeshTriangle malloc(MemoryStack stack) {
        return new MeshTriangle(stack.nmalloc(ALIGNOF, SIZEOF), null);
    }

    public static MeshTriangle calloc(MemoryStack stack) {
        return new MeshTriangle(stack.ncalloc(ALIGNOF, 1, SIZEOF), null);
    }

    public static Buffer malloc(int capacity, MemoryStack stack) {
        return new Buffer(stack.nmalloc(ALIGNOF, capacity * SIZEOF), capacity);
    }

    public static Buffer calloc(int capacity, MemoryStack stack) {
        return new Buffer(stack.ncalloc(ALIGNOF, capacity, SIZEOF), capacity);
    }

    public static class Buffer extends StructBuffer<MeshTriangle, Buffer> {

        private static final MeshTriangle ELEMENT_FACTORY = MeshTriangle.create(-1L);

        public Buffer(ByteBuffer container) {
            super(container, container.remaining() / SIZEOF);
        }

        public Buffer(long address, int cap) {
            super(address, null, -1, 0, cap, cap);
        }

        protected Buffer(ByteBuffer container, int remaining) {
            super(container, remaining);
        }

        protected Buffer(long address, ByteBuffer container, int mark, int position, int limit, int capacity) {
            super(address, container, mark, position, limit, capacity);
        }

        @Override
        protected MeshTriangle getElementFactory() {
            return null;
        }

        @Override
        protected Buffer self() {
            return null;
        }

        @Override
        protected Buffer create(long address, @org.jspecify.annotations.Nullable ByteBuffer container, int mark, int position, int limit, int capacity) {
            return null;
        }

    }

}
