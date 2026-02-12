package com.jme3.vulkan.pass;

import com.jme3.util.AbstractNativeBuilder;
import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.DisposableManager;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.pipeline.framebuffer.FrameBuffer;
import com.jme3.vulkan.pipeline.PipelineBindPoint;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class RenderPass extends AbstractNative<Long> {

    private final LogicalDevice<?> device;
    private final List<Attachment> attachments = new ArrayList<>();
    private final List<Subpass> subpasses = new ArrayList<>();
    private final List<SubpassDependency> dependencies = new ArrayList<>();

    public RenderPass(LogicalDevice<?> device) {
        this.device = device;
    }

    @Override
    public Runnable createDestroyer() {
        return () -> vkDestroyRenderPass(device.getNativeObject(), object, null);
    }

    public void begin(CommandBuffer cmd, FrameBuffer<?> fbo) {
        begin(cmd, fbo, true);
    }

    public void begin(CommandBuffer cmd, FrameBuffer<?> fbo, boolean inline) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkClearValue.Buffer clear = VkClearValue.calloc(attachments.size(), stack);
            for (Attachment a : attachments) {
                VkClearValue clr = clear.get();
                clr.color().float32(a.getClearColor().toBuffer(stack));
                clr.depthStencil().set(a.getClearDepth(), a.getClearStencil());
            }
            clear.flip();
            VkRenderPassBeginInfo begin = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(object)
                    .framebuffer(fbo.getId())
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

    public Subpass getSubpass(int i) {
        return subpasses.get(i);
    }

    public LogicalDevice getDevice() {
        return device;
    }

    public List<Attachment> getAttachments() {
        return Collections.unmodifiableList(attachments);
    }

    public List<Subpass> getSubpasses() {
        return Collections.unmodifiableList(subpasses);
    }

    public List<SubpassDependency> getDependencies() {
        return Collections.unmodifiableList(dependencies);
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

    public static RenderPass build(LogicalDevice<?> device, Consumer<Builder> config) {
        Builder b = new RenderPass(device).new Builder();
        config.accept(b);
        return b.build();
    }

    public static RenderPass build(LogicalDevice<?> device, RenderPass base, Consumer<Builder> config) {
        Builder b = new RenderPass(device).new Builder(base);
        config.accept(b);
        return b.build();
    }

    public class Builder extends AbstractNativeBuilder<RenderPass> {

        public Builder() {}

        public Builder(RenderPass base) {
            for (Attachment a : base.attachments) {
                attachments.add(new Attachment(attachments.size(), a));
            }
            for (Subpass s : base.subpasses) {
                subpasses.add(new Subpass(RenderPass.this, subpasses.size(), s, attachments));
            }
            for (SubpassDependency d : base.dependencies) {
                dependencies.add(new SubpassDependency(d, subpasses));
            }
        }

        @Override
        protected RenderPass construct() {
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
            ref = DisposableManager.reference(RenderPass.this);
            device.getReference().addDependent(ref);
            return RenderPass.this;
        }

        public Attachment createAttachment(Format format, int samples) {
            Attachment a = new Attachment(attachments.size(), format, samples);
            attachments.add(a);
            return a;
        }

        public Attachment createAttachment(Format format, int samples, Consumer<Attachment> config) {
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
            Subpass p = new Subpass(RenderPass.this, subpasses.size(), bindPoint);
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
