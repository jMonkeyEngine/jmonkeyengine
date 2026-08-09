package com.jme3.vulkan.pass;

import com.jme3.math.ColorRGBA;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.images.newimage.EngineImage;
import org.lwjgl.vulkan.VkAttachmentDescription;

public class Attachment {

    private final int position;
    private final Format format;
    private final int samples;
    private EngineImage.Load load = EngineImage.Load.DontCare;
    private EngineImage.Store store = EngineImage.Store.DontCare;
    private EngineImage.Load stencilLoad = EngineImage.Load.DontCare;
    private EngineImage.Store stencilStore = EngineImage.Store.DontCare;
    private EngineImage.Layout initialLayout = EngineImage.Layout.Undefined;
    private EngineImage.Layout finalLayout = EngineImage.Layout.General;
    private final ColorRGBA clearColor = ColorRGBA.Black.clone();
    private float clearDepth = 1f;
    private int clearStencil = 0;

    protected Attachment(int position, Format format, int samples) {
        this.position = position;
        this.format = format;
        this.samples = samples;
    }

    protected Attachment(int position, Attachment base) {
        this.position = position;
        this.format = base.format;
        this.samples = base.samples;
        this.load = base.load;
        this.store = base.store;
        this.stencilLoad = base.stencilLoad;
        this.stencilStore = base.stencilStore;
        this.initialLayout = base.initialLayout;
        this.finalLayout = base.finalLayout;
    }

    public AttachmentReference createReference(EngineImage.Layout layout) {
        return new AttachmentReference(this, layout);
    }

    public void fillStruct(VkAttachmentDescription struct) {
        struct.format(format.getEnum())
                .samples(samples)
                .loadOp(load.getEnum())
                .storeOp(store.getEnum())
                .stencilLoadOp(stencilLoad.getEnum())
                .stencilStoreOp(stencilStore.getEnum())
                .initialLayout(initialLayout.getEnum())
                .finalLayout(finalLayout.getEnum());
    }

    public void setLoad(EngineImage.Load load) {
        this.load = load;
    }

    public void setStencilLoad(EngineImage.Load stencilLoad) {
        this.stencilLoad = stencilLoad;
    }

    public void setStore(EngineImage.Store store) {
        this.store = store;
    }

    public void setStencilStore(EngineImage.Store stencilStore) {
        this.stencilStore = stencilStore;
    }

    public void setInitialLayout(EngineImage.Layout initialLayout) {
        this.initialLayout = initialLayout;
    }

    public void setFinalLayout(EngineImage.Layout finalLayout) {
        this.finalLayout = finalLayout;
    }

    public void setClearColor(ColorRGBA color) {
        this.clearColor.set(color);
    }

    public void setClearDepth(float clearDepth) {
        this.clearDepth = clearDepth;
    }

    public void setClearStencil(int clearStencil) {
        this.clearStencil = clearStencil;
    }

    public int getPosition() {
        return position;
    }

    public Format getFormat() {
        return format;
    }

    public int getSamples() {
        return samples;
    }

    public EngineImage.Load getLoad() {
        return load;
    }

    public EngineImage.Load getStencilLoad() {
        return stencilLoad;
    }

    public EngineImage.Store getStore() {
        return store;
    }

    public EngineImage.Store getStencilStore() {
        return stencilStore;
    }

    public EngineImage.Layout getInitialLayout() {
        return initialLayout;
    }

    public EngineImage.Layout getFinalLayout() {
        return finalLayout;
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

    public boolean isCompatible(Attachment a) {
        return position == a.position && format == a.format && samples == a.samples;
    }

}
