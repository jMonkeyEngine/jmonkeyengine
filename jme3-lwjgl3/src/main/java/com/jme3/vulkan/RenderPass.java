package com.jme3.vulkan;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;

import java.nio.LongBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class RenderPass implements Native<Long> {

    private final LogicalDevice device;
    private final NativeReference ref;
    private LongBuffer id = MemoryUtil.memAllocLong(1);

    public RenderPass(LogicalDevice device, VkRenderPassCreateInfo create) {
        this.device = device;
        check(vkCreateRenderPass(device.getNativeObject(), create, null, id),
                "Failed to create render pass.");
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
    }

    @Override
    public Long getNativeObject() {
        return id != null ? id.get(0) : null;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> {
            vkDestroyRenderPass(device.getNativeObject(), id.get(0), null);
            MemoryUtil.memFree(id);
        };
    }

    @Override
    public void prematureNativeDestruction() {
        id = null;
    }

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }

    public void begin(CommandBuffer cmd, FrameBuffer fbo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkClearValue.Buffer clear = VkClearValue.calloc(1, stack);
            clear.color().float32(stack.floats(0f, 0f, 0f, 1f));
            VkRenderPassBeginInfo begin = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(id.get(0))
                    .framebuffer(fbo.getNativeObject())
                    .clearValueCount(clear.limit())
                    .pClearValues(clear);
            begin.renderArea().offset().set(0, 0);
            begin.renderArea().extent().width(fbo.getWidth()).height(fbo.getHeight());
            vkCmdBeginRenderPass(cmd.getBuffer(), begin, VK_SUBPASS_CONTENTS_INLINE);
        }
    }

    public LogicalDevice getDevice() {
        return device;
    }

}
