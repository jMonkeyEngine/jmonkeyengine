package com.jme3.vulkan;

import com.jme3.renderer.vulkan.VulkanUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Struct;
import org.lwjgl.vulkan.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.lwjgl.vulkan.VK10.*;

public class RenderPassBuilder implements AutoCloseable {

    private final MemoryStack stack = MemoryStack.stackPush();
    private final List<VkAttachmentDescription> attachments = new ArrayList<>();
    private final List<VkSubpassDescription> subpasses = new ArrayList<>();
    private final List<VkSubpassDependency> dependencies = new ArrayList<>();

    @Override
    public void close() {
        attachments.clear();
        subpasses.clear();
        dependencies.clear();
        stack.pop();
    }

    public RenderPass build(LogicalDevice device) {
        VkAttachmentDescription.Buffer attBuf = VulkanUtils.accumulate(attachments, n -> VkAttachmentDescription.malloc(n, stack));
        VkSubpassDescription.Buffer subBuf = VulkanUtils.accumulate(subpasses, n -> VkSubpassDescription.malloc(n, stack));
        VkSubpassDependency.Buffer depBuf = VulkanUtils.accumulate(dependencies, n -> VkSubpassDependency.malloc(n, stack));
        VkRenderPassCreateInfo create = VkRenderPassCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                .pAttachments(attBuf)
                .pSubpasses(subBuf)
                .pDependencies(depBuf);
        return new RenderPass(device, create);
    }

    public int createAttachment(Consumer<VkAttachmentDescription> config) {
        VkAttachmentDescription a = VkAttachmentDescription.calloc(stack);
        config.accept(a);
        return addAttachment(a);
    }

    public int createSubpass(Consumer<VkSubpassDescription> config) {
        VkSubpassDescription s = VkSubpassDescription.calloc(stack);
        config.accept(s);
        return addSubpass(s);
    }

    public VkSubpassDependency createDependency() {
        return addDependency(VkSubpassDependency.calloc(stack));
    }

    public int addAttachment(VkAttachmentDescription attachment) {
        attachments.add(attachment);
        return attachments.size() - 1;
    }

    public int addSubpass(VkSubpassDescription subpass) {
        subpasses.add(subpass);
        return subpasses.size() - 1;
    }

    public VkSubpassDependency addDependency(VkSubpassDependency dependency) {
        dependencies.add(dependency);
        return dependency;
    }

    public MemoryStack getStack() {
        return stack;
    }

}
