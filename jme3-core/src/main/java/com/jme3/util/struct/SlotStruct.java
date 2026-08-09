package com.jme3.util.struct;

import java.util.BitSet;
import java.util.function.IntFunction;

public class SlotStruct <T extends StructField> extends Struct<T> {

    private final int length;
    private final BitSet usedSlots = new BitSet();

    public SlotStruct(int length) {
        this.length = length;
    }

    public int acquireSlot() {
        int i = usedSlots.nextClearBit(0);
        usedSlots.set(i);
        return i;
    }

    public int length() {
        return length;
    }

}
