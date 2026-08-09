package com.jme3.vulkan.pipeline.framebuffer;

import com.jme3.math.ColorRGBA;
import com.jme3.vulkan.VulkanEnums;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.images.VulkanImageView;
import com.jme3.vulkan.images.newimage.EngineImage;
import com.jme3.vulkan.pipeline.graphics.ColorBlendAttachment;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;

@Deprecated
public class VulkanRenderTarget {

    private final Flag<EngineImage.Aspect> aspects;
    private VulkanImageView view;
    private EngineImage.Layout layout;
    private ColorBlendAttachment colorBlend;
    private ColorRGBA clearColor = ColorRGBA.BlackNoAlpha;
    private float clearDepth = 1f;
    private int clearStencil = 0;
    private long version = 0;

    public static VulkanRenderTarget createColorTarget(VulkanImageView view) {
        return new VulkanRenderTarget(EngineImage.Aspect.Color, view, EngineImage.Layout.ColorAttachmentOptimal);
    }

    public static VulkanRenderTarget createDepthTarget(VulkanImageView view) {
        return new VulkanRenderTarget(EngineImage.Aspect.Depth, view, EngineImage.Layout.DepthStencilAttachmentOptimal);
    }

    public static VulkanRenderTarget createStencilTarget(VulkanImageView view) {
        return new VulkanRenderTarget(EngineImage.Aspect.Stencil, view, EngineImage.Layout.DepthStencilAttachmentOptimal);
    }

    protected VulkanRenderTarget(Flag<EngineImage.Aspect> aspects, VulkanImageView view, EngineImage.Layout layout) {
        this.aspects = aspects;
        this.layout = layout;
        setView(view);
    }

    public VulkanRenderTarget setView(VulkanImageView view) {
        assert view.getAspect().contains(aspects) : "Image does not have the required aspects for this target.";
        if (this.view != view) {
            this.view = view;
            version++;
        }
        return this;
    }

    public VulkanImageView getView() {
        return view;
    }

    public RenderTarget<VulkanImageView> setColorBlend(ColorBlendAttachment colorBlend) {
        return null;
    }

    public ColorBlendAttachment getColorBlend() {
        return null;
    }

    public VkRenderingAttachmentInfo fill(VkRenderingAttachmentInfo attachment, EngineImage.Load load, EngineImage.Store store) {
        attachment.imageView(getView().getId()).imageLayout(layout.getEnum());
        attachment.loadOp(load.getEnum(VulkanEnums.instance)).storeOp(store.getEnum(VulkanEnums.instance));
        if (aspects.containsAny(EngineImage.Aspect.Color)) {
            attachment.clearValue().color().float32()
                .put(clearColor.r).put(clearColor.g).put(clearColor.b).put(clearColor.a)
                .flip();
        }
        if (aspects.containsAny(EngineImage.Aspect.DepthStencil)) {
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

    public VulkanRenderTarget setLayout(EngineImage.Layout layout) {
        if (layout != this.layout) {
            this.layout = layout;
            version++;
        }
        return this;
    }

    public VulkanRenderTarget setClearColor(ColorRGBA clearColor) {
        if (aspects.containsAny(EngineImage.Aspect.Color) && !this.clearColor.equals(clearColor)) {
            version++;
        }
        this.clearColor = clearColor.clone();
        return this;
    }

    public VulkanRenderTarget setClearDepth(float clearDepth) {
        if (aspects.containsAny(EngineImage.Aspect.Depth) && this.clearDepth != clearDepth) {
            version++;
        }
        this.clearDepth = clearDepth;
        return this;
    }

    public VulkanRenderTarget setClearStencil(int clearStencil) {
        if (aspects.containsAny(EngineImage.Aspect.Stencil) && this.clearStencil != clearStencil) {
            version++;
        }
        this.clearStencil = clearStencil;
        return this;
    }

    public Flag<EngineImage.Aspect> getAspects() {
        return aspects;
    }

    public IntEnum<EngineImage.Layout> getLayout() {
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
