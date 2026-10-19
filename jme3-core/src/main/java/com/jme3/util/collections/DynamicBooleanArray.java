package com.jme3.util.collections;

import java.util.BitSet;

public class DynamicBooleanArray implements DynamicArray<Boolean> {

    private final BitSet bits = new BitSet();

    @Override
    public void set(int i, Boolean v) {
        i <<= 1;
        bits.set(i, v != null);
        bits.set(i + 1, v != null && v);
    }

    @Override
    public Boolean get(int i) {
        i <<= 1;
        return bits.get(i) ? bits.get(i + 1) : null;
    }

    @Override
    public boolean isNull(int i) {
        return bits.get(i << 1);
    }

    @Override
    public int indexOf(Boolean v) {
        if (v == null) {
            for (int i = bits.nextClearBit(0); i >= 0; i = bits.nextClearBit(i + 1)) {
                if ((i & 1) == 0) {
                    return i >> 1;
                }
            }
            return (bits.previousSetBit(Integer.MAX_VALUE) + 2) >> 1;
        }
        for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 2)) {
            if (bits.get(i + 1) == v) {
                return i >> 1;
            }
        }
        return -1;
    }

}
