package com.jme3.scene.plugins.blender.textures.generating;

import com.jme3.scene.plugins.blender.textures.TextureHelper;

public class TextureGeneratorFactory {

    private NoiseGenerator noiseGenerator = new NoiseGenerator();

    public TextureGenerator createTextureGenerator(int generatedTexture) {
        switch (generatedTexture) {
            case TextureHelper.TEX_BLEND:
                return new TextureGeneratorBlend(noiseGenerator);
            case TextureHelper.TEX_CLOUDS:
                return new TextureGeneratorClouds(noiseGenerator);
            case TextureHelper.TEX_DISTNOISE:
                return new TextureGeneratorDistnoise(noiseGenerator);
            case TextureHelper.TEX_MAGIC:
                return new TextureGeneratorMagic(noiseGenerator);
            case TextureHelper.TEX_MUSGRAVE:
                return new TextureGeneratorMusgrave(noiseGenerator);
            case TextureHelper.TEX_NOISE:
                return new TextureGeneratorNoise(noiseGenerator);
            case TextureHelper.TEX_STUCCI:
                return new TextureGeneratorStucci(noiseGenerator);
            case TextureHelper.TEX_VORONOI:
                return new TextureGeneratorVoronoi(noiseGenerator);
            default:
                throw new IllegalStateException("Unknown generated texture type: " + generatedTexture);
        }
    }
}
