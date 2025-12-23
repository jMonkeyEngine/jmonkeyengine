package com.jme3.vulkan.pipeline.framebuffer;

import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.Native;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.images.VulkanImageView;
import com.jme3.vulkan.pass.RenderPass;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK14.*;

public class VulkanFrameBuffer extends AbstractNative<Long> implements FrameBuffer<VulkanImageView> {

    public enum Create implements Flag<Create> {

        Imageless(VK_FRAMEBUFFER_CREATE_IMAGELESS_BIT);

        private final int bits;

        Create(int bits) {
            this.bits = bits;
        }

        @Override
        public int bits() {
            return bits;
        }

    }

    private final LogicalDevice<?> device;
    private final RenderPass compat;
    private final int width, height, layers;
    private final List<VulkanImageView> colorTargets = new ArrayList<>();
    private VulkanImageView depthTarget;
    private Flag<Create> flags = Flag.empty();
    private boolean updateNeeded = true;

    public VulkanFrameBuffer(LogicalDevice<?> device, RenderPass compat, int width, int height, int layers) {
        this.device = device;
        this.compat = compat;
        this.width = width;
        this.height = height;
        this.layers = layers;
    }

    @Override
    public long getId() {
        return getNativeObject();
    }



    @Override
    public Long getNativeObject() {
        if (updateNeeded) {
            build();
        }
        return super.getNativeObject();
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroyFramebuffer(device.getNativeObject(), object, null);
    }

    @Override
    public void addColorTarget(VulkanImageView image) {
        assert image.getAspect().contains(VulkanImage.Aspect.Color) : "Image must have a color aspect.";
        colorTargets.add(image);
        updateNeeded = false;
    }

    @Override
    public void setColorTarget(int i, VulkanImageView image) {
        assert image.getAspect().contains(VulkanImage.Aspect.Color) : "Image must have a color aspect.";
        if (colorTargets.set(i, image) != image) {
            updateNeeded = true;
        }
    }

    @Override
    public void removeColorTarget(int i) {
        if (i < colorTargets.size()) {
            colorTargets.remove(i);
            updateNeeded = true;
        }
    }

    @Override
    public void removeColorTarget(VulkanImageView image) {
        if (colorTargets.removeIf(c -> c == image)) {
            updateNeeded = true;
        }
    }

    @Override
    public void clearColorTargets() {
        if (!colorTargets.isEmpty()) {
            colorTargets.clear();
            updateNeeded = true;
        }
    }

    @Override
    public void setDepthTarget(VulkanImageView image) {
        assert image == null || image.getAspect().contains(VulkanImage.Aspect.Depth) : "Image must have a depth aspect.";
        if (this.depthTarget != image) {
            this.depthTarget = image;
            updateNeeded = true;
        }
    }

    @Override
    public List<VulkanImageView> getColorTargets() {
        return Collections.unmodifiableList(colorTargets);
    }

    @Override
    public VulkanImageView getColorTarget(int i) {
        return colorTargets.get(i);
    }

    @Override
    public VulkanImageView getDepthTarget() {
        return depthTarget;
    }

    public void setFlags(Flag<Create> flags) {
        if (!Flag.is(this.flags, Objects.requireNonNull(flags))) {
            this.flags = flags;
            updateNeeded = true;
        }
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public int getLayers() {
        return layers;
    }

    public Flag<Create> getFlags() {
        return flags;
    }

    public int getNumTargets() {
        return colorTargets.size() + (depthTarget != null ? 1 : 0);
    }

    protected void build() {
        if (ref != null) {
            ref.destroy();
            ref = null;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer att = stack.mallocLong(getNumTargets());
            for (VulkanImageView c : colorTargets) {
                if (c == null) {
                    throw new NullPointerException("Cannot attach null color target.");
                }
                att.put(c.getId());
            }
            if (depthTarget != null) {
                att.put(depthTarget.getId());
            }
            att.flip();
            VkFramebufferCreateInfo create = VkFramebufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                    .flags(flags.bits())
                    .renderPass(compat.getNativeObject())
                    .pAttachments(att)
                    .width(width).height(height)
                    .layers(layers);
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateFramebuffer(device.getNativeObject(), create, null, idBuf));
            object = idBuf.get(0);
        }
        ref = Native.get().register(VulkanFrameBuffer.this);
        device.getNativeReference().addDependent(ref);
        updateNeeded = false;
    }

}
