package com.jme3.vulkan.pipeline.framebuffer;

import com.jme3.math.ColorRGBA;
import com.jme3.texture.ImageView;
import com.jme3.vulkan.images.newimage.EngineImage;
import com.jme3.vulkan.pipeline.graphics.ColorBlendAttachment;
import com.jme3.vulkan.util.Flag;

import java.util.Objects;

public class RenderTarget <T extends ImageView> implements Cloneable {

    private final Flag<EngineImage.Aspect> aspects;
    private final ColorBlendAttachment colorBlend = new ColorBlendAttachment(false);
    private final ColorRGBA clearColor = new ColorRGBA(0, 0, 0, 0);

    private T view;
    private EngineImage.Layout layout;
    private EngineImage.Load load = EngineImage.Load.Clear;
    private EngineImage.Store store = EngineImage.Store.Store;
    private float clearDepth = 1f;
    private int clearStencil = 0;

    public static <T extends ImageView> RenderTarget<T> createColorTarget(T view) {
        return new RenderTarget<>(view, EngineImage.Aspect.Color, EngineImage.Layout.ColorAttachmentOptimal);
    }

    public static <T extends ImageView> RenderTarget<T> createDepthTarget(T view) {
        return new RenderTarget<>(view, EngineImage.Aspect.Depth, EngineImage.Layout.DepthStencilAttachmentOptimal);
    }

    public static <T extends ImageView> RenderTarget<T> createDepthStencilTarget(T view) {
        return new RenderTarget<>(view, EngineImage.Aspect.DepthStencil, EngineImage.Layout.DepthStencilAttachmentOptimal);
    }

    public RenderTarget(T view, Flag<EngineImage.Aspect> aspects, EngineImage.Layout layout) {
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

    public void setLayout(EngineImage.Layout layout) {
        this.layout = layout;
    }

    public void setLoad(EngineImage.Load load) {
        this.load = load;
    }

    public void setStore(EngineImage.Store store) {
        this.store = store;
    }

    public void setClearDepth(float clearDepth) {
        this.clearDepth = clearDepth;
    }

    public void setClearStencil(int clearStencil) {
        this.clearStencil = clearStencil;
    }

    public Flag<EngineImage.Aspect> getAspects() {
        return aspects;
    }

    public EngineImage.Layout getLayout() {
        return layout;
    }

    public ColorBlendAttachment getColorBlend() {
        return colorBlend;
    }

    public EngineImage.Load getLoad() {
        return load;
    }

    public EngineImage.Store getStore() {
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
