package com.jme3.vulkan.descriptors.uniforms;

import com.jme3.texture.Texture;
import com.jme3.vulkan.images.newimage.HandleTexture;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;

import java.util.BitSet;

/**
 * A quality-of-life extension for TextureBinding that emits a {@link HandleTexture} when
 * a texture is submitted to the binding.
 */
@Deprecated
public class BindlessTextureArray extends TextureBinding {

    private final BitSet usedDescriptors = new BitSet();

    public BindlessTextureArray(int descriptors, Flag<ShaderStage> stages) {
        super(descriptors, stages);
    }

    @Override
    public void set(int descriptor, Texture resource) {
        super.set(descriptor, resource);
        if (resource == null) {
            usedDescriptors.clear(descriptor);
        }
    }

    public HandleTexture create(Texture texture) {
        int i = usedDescriptors.nextClearBit(0);
        usedDescriptors.set(i, texture != null);
        super.set(i, texture);
        return new HandleTexture(texture, i);
    }

}
