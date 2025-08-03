package com.jme3.vulkan.images;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class Sampler implements Native<Long> {

    private final LogicalDevice device;
    private final NativeReference ref;
    private final long id;

    public Sampler(LogicalDevice device, int min, int mag, int edgeMode, int mipmapMode) {
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceProperties props = device.getPhysicalDevice().getProperties(stack);
            VkSamplerCreateInfo create = VkSamplerCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                    .minFilter(min)
                    .magFilter(mag)
                    .addressModeU(edgeMode)
                    .addressModeV(edgeMode)
                    .addressModeW(edgeMode)
                    .anisotropyEnable(true)
                    .maxAnisotropy(props.limits().maxSamplerAnisotropy())
                    .borderColor(VK_BORDER_COLOR_FLOAT_OPAQUE_BLACK)
                    .unnormalizedCoordinates(false) // use (0, 1) sampler coordinates
                    .compareEnable(false)
                    .compareOp(VK_COMPARE_OP_ALWAYS)
                    .mipmapMode(mipmapMode)
                    .mipLodBias(0f)
                    .minLod(0f)
                    .maxLod(0f);
            LongBuffer idBuf = stack.mallocLong(1);
            vkCreateSampler(device.getNativeObject(), create, null, idBuf);
            id = idBuf.get(0);
        }
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
    }

    @Override
    public Long getNativeObject() {
        return id;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroySampler(device.getNativeObject(), id, null);
    }

    @Override
    public void prematureNativeDestruction() {

    }

    @Override
    public NativeReference getNativeReference() {
        return null;
    }

}
