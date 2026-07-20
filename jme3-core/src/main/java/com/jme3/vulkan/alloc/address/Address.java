package com.jme3.vulkan.alloc.address;

@Deprecated
public interface Address {

    /**
     * Creates an address that is a slice of this address.
     *
     * @param offset offset from this address in bytes
     * @param size size of the created address
     * @return created address
     */
    Address slice(int offset, int size);

    /**
     * Gets the size of the memory region represented by this address.
     *
     * @return size in bytes
     */
    int size();

}
