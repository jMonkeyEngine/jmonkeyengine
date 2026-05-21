package com.jme3.vulkan.alloc;

public interface NativeResource {

    /**
     * Destroys all native resources handled by this memory object.
     */
    void destroy();

    /**
     * Destroys all native resources in the pointer chain.
     *
     * @param mem
     */
    static void destroy(Memory mem) {
        for (int i = 0; i < 1000; i++) {
            if (mem instanceof NativeResource) {
                ((NativeResource)mem).destroy();
            }
            if (mem instanceof MemoryPointer) {
                mem = ((MemoryPointer)mem).getBoundMemory();
            } else return;
        }
        throw new IllegalStateException("Failed to exit after 1000 iterations. Possible circular reference.");
    }

}
