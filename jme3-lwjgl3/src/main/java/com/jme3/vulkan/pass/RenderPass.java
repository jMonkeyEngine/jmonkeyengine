package com.jme3.vulkan.pass;

import com.jme3.util.natives.Native;
import com.jme3.vulkan.CommandBuffer;
import com.jme3.vulkan.VulkanObject;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.images.Image;
import com.jme3.vulkan.pipelines.FrameBuffer;
import com.jme3.vulkan.pipelines.PipelineBindPoint;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class RenderPass extends VulkanObject<Long> {

    private final LogicalDevice<?> device;
    private final List<Attachment> attachments = new ArrayList<>();
    private final List<Subpass> subpasses = new ArrayList<>();
    private final List<SubpassDependency> dependencies = new ArrayList<>();
    private boolean built = false;

    public RenderPass(LogicalDevice<?> device) {
        this.device = device;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroyRenderPass(device.getNativeObject(), object, null);
    }

    public void begin(CommandBuffer cmd, FrameBuffer fbo) {
        begin(cmd, fbo, true);
    }

    public void begin(CommandBuffer cmd, FrameBuffer fbo, boolean inline) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkClearValue.Buffer clear = VkClearValue.calloc(2, stack);
            clear.get(0).color().float32(stack.floats(0f, 0f, 0f, 1f));
            clear.get(1).depthStencil().set(1.0f, 0);
            VkRenderPassBeginInfo begin = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(object)
                    .framebuffer(fbo.getNativeObject())
                    .clearValueCount(clear.limit())
                    .pClearValues(clear);
            begin.renderArea().offset().set(0, 0);
            begin.renderArea().extent().width(fbo.getWidth()).height(fbo.getHeight());
            vkCmdBeginRenderPass(cmd.getBuffer(), begin, inline ? VK_SUBPASS_CONTENTS_INLINE : VK_SUBPASS_CONTENTS_SECONDARY_COMMAND_BUFFERS);
        }
    }

    public void nextSubpass(CommandBuffer cmd) {
        nextSubpass(cmd, true);
    }

    public void nextSubpass(CommandBuffer cmd, boolean inline) {
        vkCmdNextSubpass(cmd.getBuffer(), inline ? VK_SUBPASS_CONTENTS_INLINE : VK_SUBPASS_CONTENTS_SECONDARY_COMMAND_BUFFERS);
    }

    public void end(CommandBuffer cmd) {
        vkCmdEndRenderPass(cmd.getBuffer());
    }

    public LogicalDevice getDevice() {
        return device;
    }

    public boolean isCompatible(RenderPass pass) {
        if (this == pass) {
            return true;
        }
        if (attachments.size() != pass.attachments.size()) {
            return false;
        }
        if (subpasses.size() != pass.subpasses.size()) {
            return false;
        }
        if (dependencies.size() != pass.dependencies.size()) {
            return false;
        }
        for (int i = 0; i < attachments.size(); i++) {
            if (!attachments.get(i).isCompatible(pass.attachments.get(i))) {
                return false;
            }
        }
        for (int i = 0; i < subpasses.size(); i++) {
            if (!subpasses.get(i).isCompatible(pass.subpasses.get(i))) {
                return false;
            }
        }
        for (int i = 0; i < dependencies.size(); i++) {
            if (!dependencies.get(i).isCompatible(pass.dependencies.get(i))) {
                return false;
            }
        }
        return true;
    }

    public Builder build() {
        if (built) {
            throw new IllegalStateException("Render pass has already been built or is being built.");
        }
        built = true;
        return new Builder();
    }

    public Builder buildCopyOf(RenderPass base) {
        return new Builder(base);
    }

    public class Builder extends VulkanObject.Builder<RenderPass> {

        public Builder() {}

        public Builder(RenderPass base) {
            for (Attachment a : base.attachments) {
                attachments.add(new Attachment(attachments.size(), a));
            }
            for (Subpass s : base.subpasses) {
                subpasses.add(new Subpass(subpasses.size(), s, attachments));
            }
            for (SubpassDependency d : base.dependencies) {
                dependencies.add(new SubpassDependency(d, subpasses));
            }
        }

        @Override
        protected void build() {
            VkRenderPassCreateInfo create = VkRenderPassCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
            if (!attachments.isEmpty()) {
                VkAttachmentDescription.Buffer buf = VkAttachmentDescription.calloc(attachments.size(), stack);
                for (Attachment a : attachments) {
                    a.fillStruct(buf.get());
                }
                create.pAttachments(buf.flip());
            }
            if (!subpasses.isEmpty()) {
                VkSubpassDescription.Buffer buf = VkSubpassDescription.calloc(subpasses.size(), stack);
                for (Subpass s : subpasses) {
                    s.fillStruct(stack, buf.get());
                }
                create.pSubpasses(buf.flip());
            }
            if (!dependencies.isEmpty()) {
                VkSubpassDependency.Buffer buf = VkSubpassDependency.calloc(dependencies.size(), stack);
                for (SubpassDependency d : dependencies) {
                    d.fillStruct(buf.get());
                }
                create.pDependencies(buf.flip());
            }
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateRenderPass(device.getNativeObject(), create, null, idBuf), "Failed to create render pass.");
            object = idBuf.get(0);
            ref = Native.get().register(RenderPass.this);
            device.getNativeReference().addDependent(ref);
        }

        public Attachment createAttachment(Image.Format format, int samples) {
            Attachment a = new Attachment(attachments.size(), format, samples);
            attachments.add(a);
            return a;
        }

        public Attachment createAttachment(Image.Format format, int samples, Consumer<Attachment> config) {
            Attachment a = createAttachment(format, samples);
            config.accept(a);
            return a;
        }

        public Attachment getAttachment(int i) {
            return attachments.get(i);
        }

        public Attachment getAttachment(int i, Consumer<Attachment> config) {
            Attachment a = getAttachment(i);
            config.accept(a);
            return a;
        }

        public List<Attachment> getAttachments() {
            return Collections.unmodifiableList(attachments);
        }

        public Subpass createSubpass(PipelineBindPoint bindPoint) {
            Subpass p = new Subpass(subpasses.size(), bindPoint);
            subpasses.add(p);
            return p;
        }

        public Subpass createSubpass(PipelineBindPoint bindPoint, Consumer<Subpass> config) {
            Subpass p = createSubpass(bindPoint);
            config.accept(p);
            return p;
        }

        public Subpass getSubpass(int i) {
            return subpasses.get(i);
        }

        public Subpass getSubpass(int i, Consumer<Subpass> config) {
            Subpass p = getSubpass(i);
            config.accept(p);
            return p;
        }

        public List<Subpass> getSubpasses() {
            return Collections.unmodifiableList(subpasses);
        }

        public SubpassDependency createDependency(Subpass src, Subpass dst) {
            SubpassDependency d = new SubpassDependency(src, dst);
            dependencies.add(d);
            return d;
        }

        public SubpassDependency createDependency(Subpass src, Subpass dst, Consumer<SubpassDependency> config) {
            SubpassDependency d = createDependency(src, dst);
            config.accept(d);
            return d;
        }

        public SubpassDependency getDependency(int i) {
            return dependencies.get(i);
        }

        public SubpassDependency getDependency(int i, Consumer<SubpassDependency> config) {
            SubpassDependency d = getDependency(i);
            config.accept(d);
            return d;
        }

        public List<SubpassDependency> getDependencies() {
            return Collections.unmodifiableList(dependencies);
        }

        public MemoryStack getStack() {
            return stack;
        }

    }

}
