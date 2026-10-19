package com.jme3.util.struct;

import com.jme3.math.FastMath;
import com.jme3.vulkan.buffer.DataBuffer;

public interface FieldDescription <T> {

    int getSize();

    int getAlignment();

    void write(DataBuffer buffer, T value);

    T read(DataBuffer buffer, T store);

    default int getAlignedSize() {
        return FastMath.toMultipleOfPow2(getSize(), getAlignment());
    }

}
