package com.jme3.vulkan.pass;

import com.jme3.math.ColorRGBA;
import com.jme3.vulkan.Format;
import com.jme3.vulkan.images.VulkanImage;
import org.lwjgl.vulkan.VkAttachmentDescription;

public class Attachment {

    private final int position;
    private final Format format;
    private final int samples;
    private VulkanImage.Load load = VulkanImage.Load.DontCare;
    private VulkanImage.Store store = VulkanImage.Store.DontCare;
    private VulkanImage.Load stencilLoad = VulkanImage.Load.DontCare;
    private VulkanImage.Store stencilStore = VulkanImage.Store.DontCare;
    private VulkanImage.Layout initialLayout = VulkanImage.Layout.Undefined;
    private VulkanImage.Layout finalLayout = VulkanImage.Layout.General;
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

    public AttachmentReference createReference(VulkanImage.Layout layout) {
        return new AttachmentReference(this, layout);
    }

    public void fillStruct(VkAttachmentDescription struct) {
        struct.format(format.getVkEnum())
                .samples(samples)
                .loadOp(load.getEnum())
                .storeOp(store.getEnum())
                .stencilLoadOp(stencilLoad.getEnum())
                .stencilStoreOp(stencilStore.getEnum())
                .initialLayout(initialLayout.getEnum())
                .finalLayout(finalLayout.getEnum());
    }

    public void setLoad(VulkanImage.Load load) {
        this.load = load;
    }

    public void setStencilLoad(VulkanImage.Load stencilLoad) {
        this.stencilLoad = stencilLoad;
    }

    public void setStore(VulkanImage.Store store) {
        this.store = store;
    }

    public void setStencilStore(VulkanImage.Store stencilStore) {
        this.stencilStore = stencilStore;
    }

    public void setInitialLayout(VulkanImage.Layout initialLayout) {
        this.initialLayout = initialLayout;
    }

    public void setFinalLayout(VulkanImage.Layout finalLayout) {
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

    public VulkanImage.Load getLoad() {
        return load;
    }

    public VulkanImage.Load getStencilLoad() {
        return stencilLoad;
    }

    public VulkanImage.Store getStore() {
        return store;
    }

    public VulkanImage.Store getStencilStore() {
        return stencilStore;
    }

    public VulkanImage.Layout getInitialLayout() {
        return initialLayout;
    }

    public VulkanImage.Layout getFinalLayout() {
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
