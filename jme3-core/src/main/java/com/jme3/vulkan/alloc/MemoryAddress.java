package com.jme3.vulkan.alloc;

import java.nio.ByteBuffer;

public interface MemoryAddress {

    /**
     * Creates a new address that is a slice of this address.
     *
     * @param offset slice offset within this address
     * @param size size of the slice
     * @return new address
     * @throws java.nio.BufferOverflowException if the defined region
     * falls outside this address's region.
     */
    MemoryAddress slice(int offset, int size);

    /**
     * Gets the size of the addressed region in bytes.
     *
     * @return size in bytes
     */
    int size();

    /**
     * Returns a ByteBuffer that is the bytes contained by this address.
     *
     * @return byte buffer of this address
     * @throws UnsupportedOperationException if this address not CPU accessible
     */
    ByteBuffer getBytes();

}
