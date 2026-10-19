package com.jme3.util.struct;

import com.jme3.math.FastMath;
import com.jme3.vulkan.alloc.BufferDescription;

/**
 * Struct member that serializes and deserializes values to native memory like a standard
 * C-style struct member.
 */
public interface StructField extends BufferDescription {

    /**
     * Sets this field up to use {@code layout}.
     *
     * @param layout layout to use
     */
    void setupLayout(StructLayout layout);

    /**
     * Gets the alignment in bytes of this field.
     *
     * @return alignment in bytes
     */
    int alignment();

    /**
     * Gets the size of this field aligned up to the nearest multiple of its alignment.
     *
     * @return aligned size in bytes
     */
    default int alignedSize() {
        return FastMath.toMultipleOfPow2(size(), alignment());
    }

}
