package com.jme3.vulkan.buffer.alloc;

import com.jme3.vulkan.util.Flag;

import static org.lwjgl.vulkan.VK14.*;

public enum BufferCreate implements Flag<BufferCreate> {

    SparseAliased(VK_BUFFER_CREATE_SPARSE_ALIASED_BIT),
    SparseBinding(VK_BUFFER_CREATE_SPARSE_BINDING_BIT),
    SparseResidency(VK_BUFFER_CREATE_SPARSE_RESIDENCY_BIT),
    Protected(VK_BUFFER_CREATE_PROTECTED_BIT),
    DeviceAddressCaptureReplay(VK_BUFFER_CREATE_DEVICE_ADDRESS_CAPTURE_REPLAY_BIT);

    private final int bits;

    BufferCreate(int bits) {
        this.bits = bits;
    }

    @Override
    public int bits() {
        return bits;
    }
}
