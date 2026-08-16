package com.jme3.texture;

import com.jme3.vulkan.images.newimage.EngineImage;
import com.jme3.vulkan.images.newimage.ImageView;
import com.jme3.vulkan.images.newimage.Sampler;

/**
 * Combination of a {@link Sampler} with an {@link ImageView}. Note that platforms like
 * OpenGL samplers and image views are not seperate entities.
 */
public interface Texture {

    Sampler getSampler();

    ImageView getView();

    default EngineImage getImage() {
        return getView().getImage();
    }

}
