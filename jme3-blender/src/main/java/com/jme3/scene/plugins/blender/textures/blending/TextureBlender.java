package com.jme3.scene.plugins.blender.textures.blending;

import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.texture.Image;

/**
 * An interface for texture blending classes (the classes that mix the texture
 * pixels with the material colors).
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public interface TextureBlender {
    // types of blending
    int MTEX_BLEND          = 0;
    int MTEX_MUL            = 1;
    int MTEX_ADD            = 2;
    int MTEX_SUB            = 3;
    int MTEX_DIV            = 4;
    int MTEX_DARK           = 5;
    int MTEX_DIFF           = 6;
    int MTEX_LIGHT          = 7;
    int MTEX_SCREEN         = 8;
    int MTEX_OVERLAY        = 9;
    int MTEX_BLEND_HUE      = 10;
    int MTEX_BLEND_SAT      = 11;
    int MTEX_BLEND_VAL      = 12;
    int MTEX_BLEND_COLOR    = 13;
    int MTEX_NUM_BLENDTYPES = 14;

    /**
     * This method blends the given texture with material color and the defined
     * color in 'map to' panel. As a result of this method a new texture is
     * created. The input texture is NOT.
     * 
     * @param image
     *            the image we use in blending
     * @param baseImage
     *            the texture that is underneath the current texture (its pixels
     *            will be used instead of material color)
     * @param blenderContext
     *            the blender context
     * @return new image that was created after the blending
     */
    Image blend(Image image, Image baseImage, BlenderContext blenderContext);

    /**
     * Copies blending data. Used for blending type format changing.
     * 
     * @param textureBlender
     *            the blend data that should be copied
     */
    void copyBlendingData(TextureBlender textureBlender);
}
