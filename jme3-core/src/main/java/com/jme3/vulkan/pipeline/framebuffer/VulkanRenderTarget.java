package com.jme3.vulkan.pipeline.framebuffer;

import com.jme3.math.ColorRGBA;
import com.jme3.vulkan.VulkanEnums;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.images.VulkanImageView;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;

public class VulkanRenderTarget implements RenderTarget<VulkanImageView> {

    private final Flag<VulkanImage.Aspect> aspects;
    private VulkanImageView view;
    private VulkanImage.Layout layout;
    private ColorRGBA clearColor = ColorRGBA.BlackNoAlpha;
    private float clearDepth = 1f;
    private int clearStencil = 0;
    private long version = 0;

    public static VulkanRenderTarget createColorTarget(VulkanImageView view) {
        return new VulkanRenderTarget(VulkanImage.Aspect.Color, view, VulkanImage.Layout.ColorAttachmentOptimal);
    }

    public static VulkanRenderTarget createDepthTarget(VulkanImageView view) {
        return new VulkanRenderTarget(VulkanImage.Aspect.Depth, view, VulkanImage.Layout.DepthStencilAttachmentOptimal);
    }

    public static VulkanRenderTarget createStencilTarget(VulkanImageView view) {
        return new VulkanRenderTarget(VulkanImage.Aspect.Stencil, view, VulkanImage.Layout.DepthStencilAttachmentOptimal);
    }

    protected VulkanRenderTarget(Flag<VulkanImage.Aspect> aspects, VulkanImageView view, VulkanImage.Layout layout) {
        this.aspects = aspects;
        this.layout = layout;
        setView(view);
    }

    @Override
    public VulkanRenderTarget setView(VulkanImageView view) {
        assert view.getAspect().contains(aspects) : "Image does not have the required aspects for this target.";
        if (this.view != view) {
            this.view = view;
            version++;
        }
        return this;
    }

    @Override
    public VulkanImageView getView() {
        return view;
    }

    public VkRenderingAttachmentInfo fill(VkRenderingAttachmentInfo attachment, VulkanImage.Load load, VulkanImage.Store store) {
        attachment.imageView(getView().getId()).imageLayout(layout.getEnum());
        attachment.loadOp(load.getEnum(VulkanEnums.instance)).storeOp(store.getEnum(VulkanEnums.instance));
        if (aspects.containsAny(VulkanImage.Aspect.Color)) {
            attachment.clearValue().color().float32()
                .put(clearColor.r).put(clearColor.g).put(clearColor.b).put(clearColor.a)
                .flip();
        }
        if (aspects.containsAny(VulkanImage.Aspect.DepthStencil)) {
            attachment.clearValue().depthStencil().set(clearDepth, clearStencil);
        }
        return attachment;
    }

    public void transition(CommandBuffer cmd) {
        getView().getImage().transitionLayout(cmd, layout);
    }

    public long getVersion() {
        return version;
    }

    public VulkanRenderTarget setLayout(VulkanImage.Layout layout) {
        if (layout != this.layout) {
            this.layout = layout;
            version++;
        }
        return this;
    }

    public VulkanRenderTarget setClearColor(ColorRGBA clearColor) {
        if (aspects.containsAny(VulkanImage.Aspect.Color) && !this.clearColor.equals(clearColor)) {
            version++;
        }
        this.clearColor = clearColor.clone();
        return this;
    }

    public VulkanRenderTarget setClearDepth(float clearDepth) {
        if (aspects.containsAny(VulkanImage.Aspect.Depth) && this.clearDepth != clearDepth) {
            version++;
        }
        this.clearDepth = clearDepth;
        return this;
    }

    public VulkanRenderTarget setClearStencil(int clearStencil) {
        if (aspects.containsAny(VulkanImage.Aspect.Stencil) && this.clearStencil != clearStencil) {
            version++;
        }
        this.clearStencil = clearStencil;
        return this;
    }

    public Flag<VulkanImage.Aspect> getAspects() {
        return aspects;
    }

    public IntEnum<VulkanImage.Layout> getLayout() {
        return layout;
    }

    public ColorRGBA getClearColor() {
        return clearColor;
    }

    public float getClearDepth() {
        return clearDepth;
    }

    public int getClearStencil() {
        return clearStencil;
    }

}
