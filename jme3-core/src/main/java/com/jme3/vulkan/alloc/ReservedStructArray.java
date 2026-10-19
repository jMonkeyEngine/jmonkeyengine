package com.jme3.vulkan.alloc;

import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructArray;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.commands.OpLocation;

import java.util.BitSet;

public class ReservedStructArray <T extends Struct> extends StructArray<T> {

    private final BitSet reserved = new BitSet();

    public ReservedStructArray(int length, T struct) {
        super(length, struct);
    }

    public Integer acquire(CommandBuffer cmd, OpLocation location) {
        int i = reserved.nextClearBit(0);
        reserved.set(i);
        if (i >= getLength()) {
            setLength(cmd, i + 1, location);
        }
        return i;
    }

    public void release(Integer key) {
        reserved.clear(key);
    }

}
