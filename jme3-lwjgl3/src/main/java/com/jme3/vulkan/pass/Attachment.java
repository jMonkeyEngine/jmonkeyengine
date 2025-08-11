package com.jme3.vulkan.pass;

import com.jme3.vulkan.images.Image;
import org.lwjgl.vulkan.VkAttachmentDescription;

/**
 * Immutable definition for render pass attachments.
 */
public class Attachment {

    private final int position;
    private final Image.Format format;
    private final int samples;
    private Image.Load load = Image.Load.Clear;
    private Image.Store store = Image.Store.Store;
    private Image.Load stencilLoad = Image.Load.DontCare;
    private Image.Store stencilStore = Image.Store.DontCare;
    private Image.Layout initialLayout = Image.Layout.Undefined;
    private Image.Layout finalLayout = Image.Layout.General;

    protected Attachment(int position, Image.Format format, int samples) {
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

    public AttachmentReference createReference(Image.Layout layout) {
        return new AttachmentReference(this, layout);
    }

    public void fillStruct(VkAttachmentDescription struct) {
        struct.format(format.getVkEnum())
                .samples(samples)
                .loadOp(load.getVkEnum())
                .storeOp(store.getVkEnum())
                .stencilLoadOp(stencilLoad.getVkEnum())
                .stencilStoreOp(stencilStore.getVkEnum())
                .initialLayout(initialLayout.getVkEnum())
                .finalLayout(finalLayout.getVkEnum());
    }

    public void setLoad(Image.Load load) {
        this.load = load;
    }

    public void setStencilLoad(Image.Load stencilLoad) {
        this.stencilLoad = stencilLoad;
    }

    public void setStore(Image.Store store) {
        this.store = store;
    }

    public void setStencilStore(Image.Store stencilStore) {
        this.stencilStore = stencilStore;
    }

    public void setInitialLayout(Image.Layout initialLayout) {
        this.initialLayout = initialLayout;
    }

    public void setFinalLayout(Image.Layout finalLayout) {
        this.finalLayout = finalLayout;
    }

    public int getPosition() {
        return position;
    }

    public Image.Format getFormat() {
        return format;
    }

    public int getSamples() {
        return samples;
    }

    public Image.Load getLoad() {
        return load;
    }

    public Image.Load getStencilLoad() {
        return stencilLoad;
    }

    public Image.Store getStore() {
        return store;
    }

    public Image.Store getStencilStore() {
        return stencilStore;
    }

    public Image.Layout getInitialLayout() {
        return initialLayout;
    }

    public Image.Layout getFinalLayout() {
        return finalLayout;
    }

    public boolean isCompatible(Attachment a) {
        return position == a.position && format == a.format && samples == a.samples;
    }

}
