package com.jme3.vulkan.pipeline.framebuffer;

import com.jme3.math.ColorRGBA;
import com.jme3.texture.ImageView;
import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.pipeline.graphics.ColorBlendAttachment;
import com.jme3.vulkan.util.Flag;

import java.util.Objects;

public class RenderTarget <T extends ImageView> implements Cloneable {

    private final Flag<VulkanImage.Aspect> aspects;
    private final ColorBlendAttachment colorBlend = new ColorBlendAttachment(false);
    private final ColorRGBA clearColor = new ColorRGBA(0, 0, 0, 0);

    private T view;
    private VulkanImage.Layout layout;
    private VulkanImage.Load load = VulkanImage.Load.Clear;
    private VulkanImage.Store store = VulkanImage.Store.Store;
    private float clearDepth = 1f;
    private int clearStencil = 0;

    public static <T extends ImageView> RenderTarget<T> createColorTarget(T view) {
        return new RenderTarget<>(view, VulkanImage.Aspect.Color, VulkanImage.Layout.ColorAttachmentOptimal);
    }

    public static <T extends ImageView> RenderTarget<T> createDepthTarget(T view) {
        return new RenderTarget<>(view, VulkanImage.Aspect.Depth, VulkanImage.Layout.DepthStencilAttachmentOptimal);
    }

    public static <T extends ImageView> RenderTarget<T> createDepthStencilTarget(T view) {
        return new RenderTarget<>(view, VulkanImage.Aspect.DepthStencil, VulkanImage.Layout.DepthStencilAttachmentOptimal);
    }

    public RenderTarget(T view, Flag<VulkanImage.Aspect> aspects, VulkanImage.Layout layout) {
        this.aspects = aspects;
        this.layout = layout;
        setImage(view);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RenderTarget<?> that = (RenderTarget<?>) o;
        return view == that.view
                && layout == that.layout
                && load == that.load
                && store == that.store
                && clearStencil == that.clearStencil
                && Flag.equals(aspects, that.aspects)
                && Float.compare(clearDepth, that.clearDepth) == 0
                && Objects.equals(colorBlend, that.colorBlend)
                && Objects.equals(clearColor, that.clearColor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(System.identityHashCode(view), aspects, colorBlend,
                clearColor, layout, load, store, clearDepth, clearStencil);
    }

    @Override
    public RenderTarget<T> clone() {
        try {
            RenderTarget<T> t = (RenderTarget<T>)super.clone();
            t.colorBlend.set(colorBlend);
            t.clearColor.set(clearColor);
            return t;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public void setImage(T view) {
        this.view = view;
    }

    public T getView() {
        return view;
    }

    public void setLayout(VulkanImage.Layout layout) {
        this.layout = layout;
    }

    public void setLoad(VulkanImage.Load load) {
        this.load = load;
    }

    public void setStore(VulkanImage.Store store) {
        this.store = store;
    }

    public void setClearDepth(float clearDepth) {
        this.clearDepth = clearDepth;
    }

    public void setClearStencil(int clearStencil) {
        this.clearStencil = clearStencil;
    }

    public Flag<VulkanImage.Aspect> getAspects() {
        return aspects;
    }

    public VulkanImage.Layout getLayout() {
        return layout;
    }

    public ColorBlendAttachment getColorBlend() {
        return colorBlend;
    }

    public VulkanImage.Load getLoad() {
        return load;
    }

    public VulkanImage.Store getStore() {
        return store;
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

    public int getWidth() {
        return view.getImage().getWidth();
    }

    public int getHeight() {
        return view.getImage().getHeight();
    }

}
