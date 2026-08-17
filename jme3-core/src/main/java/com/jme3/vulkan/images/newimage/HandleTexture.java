package com.jme3.vulkan.images.newimage;

import com.jme3.texture.Texture;

/**
 * Texture that can be accessed using a 64 bit handle (for OpenGL) or 32 bit handle (for Vulkan)
 * inside programmable GPU shaders. The exact method used for fetching the texture depends on the
 * backend, which HandleTextures must conform to.
 */
@Deprecated
public class HandleTexture implements Texture {

    private final Texture baseTexture;
    private final long id;

    public HandleTexture(Texture baseTexture, long id) {
        this.baseTexture = baseTexture;
        this.id = id;
    }

    @Override
    public Sampler getSampler() {
        return baseTexture.getSampler();
    }

    @Override
    public ImageView getView() {
        return baseTexture.getView();
    }

    public Texture getBaseTexture() {
        return baseTexture;
    }

    public long getTextureHandle() {
        return id;
    }

}
