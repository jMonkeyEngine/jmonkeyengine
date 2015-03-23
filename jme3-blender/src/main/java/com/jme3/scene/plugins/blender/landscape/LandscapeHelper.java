package com.jme3.scene.plugins.blender.landscape;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.light.AmbientLight;
import com.jme3.light.Light;
import com.jme3.math.ColorRGBA;
import com.jme3.post.filters.FogFilter;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.textures.ColorBand;
import com.jme3.scene.plugins.blender.textures.CombinedTexture;
import com.jme3.scene.plugins.blender.textures.ImageUtils;
import com.jme3.scene.plugins.blender.textures.TextureHelper;
import com.jme3.scene.plugins.blender.textures.TexturePixel;
import com.jme3.scene.plugins.blender.textures.io.PixelIOFactory;
import com.jme3.scene.plugins.blender.textures.io.PixelInputOutput;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.TextureCubeMap;
import com.jme3.util.SkyFactory;

/**
 * The class that allows to load the following: <li>the ambient light of the scene <li>the sky of the scene (with or without texture)
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class LandscapeHelper extends AbstractBlenderHelper {
    private static final Logger LOGGER        = Logger.getLogger(LandscapeHelper.class.getName());

    private static final int    SKYTYPE_BLEND = 1;
    private static final int    SKYTYPE_REAL  = 2;
    private static final int    SKYTYPE_PAPER = 4;

    private static final int    MODE_MIST     = 0x01;

    public LandscapeHelper(String blenderVersion, BlenderContext blenderContext) {
        super(blenderVersion, blenderContext);
    }

    /**
     * Loads scene ambient light.
     * @param worldStructure
     *            the world's blender structure
     * @return the scene's ambient light
     */
    public Light toAmbientLight(Structure worldStructure) {
        LOGGER.fine("Loading ambient light.");
        AmbientLight ambientLight = null;
        float ambr = ((Number) worldStructure.getFieldValue("ambr")).floatValue();
        float ambg = ((Number) worldStructure.getFieldValue("ambg")).floatValue();
        float ambb = ((Number) worldStructure.getFieldValue("ambb")).floatValue();
        if (ambr > 0 || ambg > 0 || ambb > 0) {
            ambientLight = new AmbientLight();
            ColorRGBA ambientLightColor = new ColorRGBA(ambr, ambg, ambb, 0.0f);
            ambientLight.setColor(ambientLightColor);
            LOGGER.log(Level.FINE, "Loaded ambient light: {0}.", ambientLightColor);
        } else {
            LOGGER.finer("Ambient light is set to BLACK which means: no ambient light! The ambient light node will not be included in the result.");
        }
        return ambientLight;
    }

    /**
     * The method loads fog for the scene.
     * NOTICE! Remember to manually set the distance and density of the fog.
     * Unfortunately blender's fog parameters in no way fit to the JME.
     * @param worldStructure
     *            the world's structure
     * @return fog filter or null if scene does not define it
     */
    public FogFilter toFog(Structure worldStructure) {
        FogFilter result = null;
        int mode = ((Number) worldStructure.getFieldValue("mode")).intValue();
        if ((mode & MODE_MIST) != 0) {
            LOGGER.fine("Loading fog.");
            result = new FogFilter();
            result.setFogColor(this.toBackgroundColor(worldStructure));
        }
        return result;
    }

    /**
     * Loads the background color.
     * @param worldStructure
     *            the world's structure
     * @return the horizon color of the world which is used as a background color.
     */
    public ColorRGBA toBackgroundColor(Structure worldStructure) {
        float horr = ((Number) worldStructure.getFieldValue("horr")).floatValue();
        float horg = ((Number) worldStructure.getFieldValue("horg")).floatValue();
        float horb = ((Number) worldStructure.getFieldValue("horb")).floatValue();
        return new ColorRGBA(horr, horg, horb, 1);
    }

    /**
     * Loads scene's sky. Sky can be plain or textured.
     * If no sky type is selected in blender then no sky is loaded.
     * @param worldStructure
     *            the world's structure
     * @return the scene's sky
     * @throws BlenderFileException
     *             blender exception is thrown when problems with blender file occur
     */
    public Spatial toSky(Structure worldStructure) throws BlenderFileException {
        int skytype = ((Number) worldStructure.getFieldValue("skytype")).intValue();
        if (skytype == 0) {
            return null;
        }

        LOGGER.fine("Loading sky.");
        ColorRGBA horizontalColor = this.toBackgroundColor(worldStructure);

        float zenr = ((Number) worldStructure.getFieldValue("zenr")).floatValue();
        float zeng = ((Number) worldStructure.getFieldValue("zeng")).floatValue();
        float zenb = ((Number) worldStructure.getFieldValue("zenb")).floatValue();
        ColorRGBA zenithColor = new ColorRGBA(zenr, zeng, zenb, 1);

        // jutr for this case load generated textures wheather user had set it or not because those might be needed to properly load the sky
        boolean loadGeneratedTextures = blenderContext.getBlenderKey().isLoadGeneratedTextures();
        blenderContext.getBlenderKey().setLoadGeneratedTextures(true);

        TextureHelper textureHelper = blenderContext.getHelper(TextureHelper.class);
        List<CombinedTexture> loadedTextures = null;
        try {
            loadedTextures = textureHelper.readTextureData(worldStructure, new float[] { horizontalColor.r, horizontalColor.g, horizontalColor.b, horizontalColor.a }, true);
        } finally {
            blenderContext.getBlenderKey().setLoadGeneratedTextures(loadGeneratedTextures);
        }

        TextureCubeMap texture = null;
        if (loadedTextures != null && loadedTextures.size() > 0) {
            if (loadedTextures.size() > 1) {
                throw new IllegalStateException("There should be only one combined texture for sky!");
            }
            CombinedTexture combinedTexture = loadedTextures.get(0);
            texture = combinedTexture.generateSkyTexture(horizontalColor, zenithColor, blenderContext);
        } else {
            LOGGER.fine("Preparing colors for colorband.");
            int colorbandType = ColorBand.IPO_CARDINAL;
            List<ColorRGBA> colorbandColors = new ArrayList<ColorRGBA>(3);
            colorbandColors.add(horizontalColor);
            if ((skytype & SKYTYPE_BLEND) != 0) {
                if ((skytype & SKYTYPE_PAPER) != 0) {
                    colorbandType = ColorBand.IPO_LINEAR;
                }
                if ((skytype & SKYTYPE_REAL) != 0) {
                    colorbandColors.add(0, zenithColor);
                }
                colorbandColors.add(zenithColor);
            }

            int size = blenderContext.getBlenderKey().getSkyGeneratedTextureSize();

            List<Integer> positions = new ArrayList<Integer>(colorbandColors.size());
            positions.add(0);
            if (colorbandColors.size() == 2) {
                positions.add(size - 1);
            } else if (colorbandColors.size() == 3) {
                positions.add(size / 2);
                positions.add(size - 1);
            }

            LOGGER.fine("Generating sky texture.");
            float[][] values = new ColorBand(colorbandType, colorbandColors, positions, size).computeValues();

            Image image = ImageUtils.createEmptyImage(Format.RGB8, size, size, 6);
            PixelInputOutput pixelIO = PixelIOFactory.getPixelIO(image.getFormat());
            TexturePixel pixel = new TexturePixel();

            LOGGER.fine("Creating side textures.");
            int[] sideImagesIndexes = new int[] { 0, 1, 4, 5 };
            for (int i : sideImagesIndexes) {
                for (int y = 0; y < size; ++y) {
                    pixel.red = values[y][0];
                    pixel.green = values[y][1];
                    pixel.blue = values[y][2];

                    for (int x = 0; x < size; ++x) {
                        pixelIO.write(image, i, pixel, x, y);
                    }
                }
            }

            LOGGER.fine("Creating top texture.");
            pixelIO.read(image, 0, pixel, 0, image.getHeight() - 1);
            for (int y = 0; y < size; ++y) {
                for (int x = 0; x < size; ++x) {
                    pixelIO.write(image, 3, pixel, x, y);
                }
            }

            LOGGER.fine("Creating bottom texture.");
            pixelIO.read(image, 0, pixel, 0, 0);
            for (int y = 0; y < size; ++y) {
                for (int x = 0; x < size; ++x) {
                    pixelIO.write(image, 2, pixel, x, y);
                }
            }

            texture = new TextureCubeMap(image);
        }

        LOGGER.fine("Sky texture created. Creating sky.");
        return SkyFactory.createSky(blenderContext.getAssetManager(), texture, SkyFactory.EnvMapType.CubeMap);
    }
}
