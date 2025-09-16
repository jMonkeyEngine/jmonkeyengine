package com.jme3.vulkan.struct;

import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.Struct;

import java.nio.ByteBuffer;

public class Structure extends Struct<Structure> implements MappedStruct {

    public Structure(long address, ByteBuffer container) {
        super(address, container);
        Layout layout = __struct(
            __member(4),
            __member(4),
            __member(4)
        );
        layout.offsetof(0);
    }

    @Override
    public void set(String name, Object value) {

    }

    @Override
    protected Structure create(long address, ByteBuffer container) {
        return null;
    }

    @Override
    public int sizeof() {
        return 0;
    }

    @Override
    public <T> T get(String name) {
        return null;
    }

    @Override
    public PointerBuffer map(int offset, int size) {
        return null;
    }

    @Override
    public void unmap() {

    }

    @Override
    public void freeMemory() {

    }

    @Override
    public MemorySize size() {
        return null;
    }

    @Override
    public long getId() {
        return 0;
    }

}
