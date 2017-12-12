/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.scene.plugins.blender.textures;

import java.awt.geom.AffineTransform;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoadException;
import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.asset.BlenderKey;
import com.jme3.asset.GeneratedTextureKey;
import com.jme3.asset.TextureKey;
import com.jme3.math.Vector2f;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedDataType;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.DynamicArray;
import com.jme3.scene.plugins.blender.file.FileBlockHeader;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.materials.MaterialContext;
import com.jme3.scene.plugins.blender.textures.UVCoordinatesGenerator.UVCoordinatesType;
import com.jme3.scene.plugins.blender.textures.blending.TextureBlender;
import com.jme3.scene.plugins.blender.textures.blending.TextureBlenderFactory;
import com.jme3.scene.plugins.blender.textures.generating.TextureGeneratorFactory;
import com.jme3.scene.plugins.blender.textures.io.PixelIOFactory;
import com.jme3.scene.plugins.blender.textures.io.PixelInputOutput;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import com.jme3.util.PlaceholderAssets;

/**
 * A class that is used in texture calculations.
 * 
 * @author Marcin Roguski
 */
public class TextureHelper extends AbstractBlenderHelper {
    private static final Logger     LOGGER                  = Logger.getLogger(TextureHelper.class.getName());

    // texture types
    public static final int         TEX_NONE                = 0;
    public static final int         TEX_CLOUDS              = 1;
    public static final int         TEX_WOOD                = 2;
    public static final int         TEX_MARBLE              = 3;
    public static final int         TEX_MAGIC               = 4;
    public static final int         TEX_BLEND               = 5;
    public static final int         TEX_STUCCI              = 6;
    public static final int         TEX_NOISE               = 7;
    public static final int         TEX_IMAGE               = 8;
    public static final int         TEX_PLUGIN              = 9;
    public static final int         TEX_ENVMAP              = 10;
    public static final int         TEX_MUSGRAVE            = 11;
    public static final int         TEX_VORONOI             = 12;
    public static final int         TEX_DISTNOISE           = 13;
    public static final int         TEX_POINTDENSITY        = 14;                                                                                                                                          // v. 25+
    public static final int         TEX_VOXELDATA           = 15;                                                                                                                                          // v. 25+
    public static final int         TEX_OCEAN               = 16;                                                                                                                                          // v. 26+

    public static final Type[]      TEXCOORD_TYPES          = new Type[] { Type.TexCoord, Type.TexCoord2, Type.TexCoord3, Type.TexCoord4, Type.TexCoord5, Type.TexCoord6, Type.TexCoord7, Type.TexCoord8 };

    private TextureGeneratorFactory textureGeneratorFactory = new TextureGeneratorFactory();

    /**
     * This constructor parses the given blender version and stores the result.
     * It creates noise generator and texture generators.
     * 
     * @param blenderVersion
     *            the version read from the blend file
     * @param blenderContext
     *            the blender context
     */
    public TextureHelper(String blenderVersion, BlenderContext blenderContext) {
        super(blenderVersion, blenderContext);
    }

    /**
     * This class returns a texture read from the file or from packed blender
     * data. The returned texture has the name set to the value of its blender
     * type.
     * 
     * @param textureStructure
     *            texture structure filled with data
     * @param blenderContext
     *            the blender context
     * @return the texture that can be used by JME engine
     * @throws BlenderFileException
     *             this exception is thrown when the blend file structure is
     *             somehow invalid or corrupted
     */
    public Texture getTexture(Structure textureStructure, Structure mTex, BlenderContext blenderContext) throws BlenderFileException {
        Texture result = (Texture) blenderContext.getLoadedFeature(textureStructure.getOldMemoryAddress(), LoadedDataType.FEATURE);
        if (result != null) {
            return result;
        }

        if ("ID".equals(textureStructure.getType())) {
            LOGGER.fine("Loading texture from external blend file.");
            return (Texture) this.loadLibrary(textureStructure);
        }

        int type = ((Number) textureStructure.getFieldValue("type")).intValue();
        int imaflag = ((Number) textureStructure.getFieldValue("imaflag")).intValue();

        switch (type) {
            case TEX_IMAGE:// (it is first because probably this will be most commonly used)
                Pointer pImage = (Pointer) textureStructure.getFieldValue("ima");
                if (pImage.isNotNull()) {
                    Structure image = pImage.fetchData().get(0);
                    Texture loadedTexture = this.loadImageAsTexture(image, imaflag, blenderContext);
                    if (loadedTexture != null) {
                        result = loadedTexture;
                        this.applyColorbandAndColorFactors(textureStructure, result.getImage(), blenderContext);
                    }
                }
                break;
            case TEX_CLOUDS:
            case TEX_WOOD:
            case TEX_MARBLE:
            case TEX_MAGIC:
            case TEX_BLEND:
            case TEX_STUCCI:
            case TEX_NOISE:
            case TEX_MUSGRAVE:
            case TEX_VORONOI:
            case TEX_DISTNOISE:
                result = new GeneratedTexture(textureStructure, mTex, textureGeneratorFactory.createTextureGenerator(type), blenderContext);
                break;
            case TEX_NONE:// No texture, do nothing
                break;
            case TEX_POINTDENSITY:
            case TEX_VOXELDATA:
            case TEX_PLUGIN:
            case TEX_ENVMAP:
            case TEX_OCEAN:
                LOGGER.log(Level.WARNING, "Unsupported texture type: {0} for texture: {1}", new Object[] { type, textureStructure.getName() });
                break;
            default:
                throw new BlenderFileException("Unknown texture type: " + type + " for texture: " + textureStructure.getName());
        }
        if (result != null) {
            result.setName(textureStructure.getName());
            result.setWrap(WrapMode.Repeat);

            // decide if the mipmaps will be generated
            switch (blenderContext.getBlenderKey().getMipmapGenerationMethod()) {
                case ALWAYS_GENERATE:
                    result.setMinFilter(MinFilter.Trilinear);
                    break;
                case NEVER_GENERATE:
                    break;
                case GENERATE_WHEN_NEEDED:
                    if ((imaflag & 0x04) != 0) {
                        result.setMinFilter(MinFilter.Trilinear);
                    }
                    break;
                default:
                    throw new IllegalStateException("Unknown mipmap generation method: " + blenderContext.getBlenderKey().getMipmapGenerationMethod());
            }

            if (type != TEX_IMAGE) {// only generated textures should have this key
                result.setKey(new GeneratedTextureKey(textureStructure.getName()));
            }

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Adding texture {0} to the loaded features with OMA = {1}", new Object[] { result.getName(), textureStructure.getOldMemoryAddress() });
            }
            blenderContext.addLoadedFeatures(textureStructure.getOldMemoryAddress(), LoadedDataType.STRUCTURE, textureStructure);
            blenderContext.addLoadedFeatures(textureStructure.getOldMemoryAddress(), LoadedDataType.FEATURE, result);
        }
        return result;
    }

    /**
     * This class returns a texture read from the file or from packed blender
     * data.
     * 
     * @param imageStructure
     *            image structure filled with data
     * @param imaflag
     *            the image flag
     * @param blenderContext
     *            the blender context
     * @return the texture that can be used by JME engine
     * @throws BlenderFileException
     *             this exception is thrown when the blend file structure is
     *             somehow invalid or corrupted
     */
    public Texture loadImageAsTexture(Structure imageStructure, int imaflag, BlenderContext blenderContext) throws BlenderFileException {
        LOGGER.log(Level.FINE, "Fetching texture with OMA = {0}", imageStructure.getOldMemoryAddress());
        Texture result = null;
        Image im = (Image) blenderContext.getLoadedFeature(imageStructure.getOldMemoryAddress(), LoadedDataType.FEATURE);
       // if (im == null) {  HACK force reaload always, as constructor in else case is destroying the TextureKeys!
            if ("ID".equals(imageStructure.getType())) {
                LOGGER.fine("Loading texture from external blend file.");
                result = (Texture) this.loadLibrary(imageStructure);
            } else {
                String texturePath = imageStructure.getFieldValue("name").toString();
                Pointer pPackedFile = (Pointer) imageStructure.getFieldValue("packedfile");
                if (pPackedFile.isNull()) {
                    LOGGER.log(Level.FINE, "Reading texture from file: {0}", texturePath);
                    result = this.loadImageFromFile(texturePath, imaflag, blenderContext);
                } else {
                    LOGGER.fine("Packed texture. Reading directly from the blend file!");
                    Structure packedFile = pPackedFile.fetchData().get(0);
                    Pointer pData = (Pointer) packedFile.getFieldValue("data");
                    FileBlockHeader dataFileBlock = blenderContext.getFileBlock(pData.getOldMemoryAddress());
                    blenderContext.getInputStream().setPosition(dataFileBlock.getBlockPosition());

                    // Should the texture be flipped? It works for sinbad ..
                    result = new ImageLoader().loadTexture(blenderContext.getAssetManager(), blenderContext.getInputStream(), dataFileBlock.getBlockPosition(), true);
                    if (result == null) {
                        result = new Texture2D(PlaceholderAssets.getPlaceholderImage(blenderContext.getAssetManager()));
                        LOGGER.fine("ImageLoader returned null. It probably failed to load the packed texture, using placeholder asset");
                    }
                }
            }
        //} else {
       //     result = new Texture2D(im);
       // }

        if (result != null) {// render result is not being loaded
            blenderContext.addLoadedFeatures(imageStructure.getOldMemoryAddress(), LoadedDataType.STRUCTURE, imageStructure);
            blenderContext.addLoadedFeatures(imageStructure.getOldMemoryAddress(), LoadedDataType.FEATURE, result.getImage());
            result.setName(imageStructure.getName());
        }
        return result;
    }

    /**
     * This method creates the affine transform that is used to transform a
     * triangle defined by one UV coordinates into a triangle defined by
     * different UV's.
     * 
     * @param source
     *            source UV coordinates
     * @param dest
     *            target UV coordinates
     * @param sourceSize
     *            the width and height of the source image
     * @param targetSize
     *            the width and height of the target image
     * @return affine transform to transform one triangle to another
     */
    public AffineTransform createAffineTransform(Vector2f[] source, Vector2f[] dest, int[] sourceSize, int[] targetSize) {
        float x11 = source[0].getX() * sourceSize[0];
        float x12 = source[0].getY() * sourceSize[1];
        float x21 = source[1].getX() * sourceSize[0];
        float x22 = source[1].getY() * sourceSize[1];
        float x31 = source[2].getX() * sourceSize[0];
        float x32 = source[2].getY() * sourceSize[1];
        float y11 = dest[0].getX() * targetSize[0];
        float y12 = dest[0].getY() * targetSize[1];
        float y21 = dest[1].getX() * targetSize[0];
        float y22 = dest[1].getY() * targetSize[1];
        float y31 = dest[2].getX() * targetSize[0];
        float y32 = dest[2].getY() * targetSize[1];

        float a1 = ((y11 - y21) * (x12 - x32) - (y11 - y31) * (x12 - x22)) / ((x11 - x21) * (x12 - x32) - (x11 - x31) * (x12 - x22));
        float a2 = ((y11 - y21) * (x11 - x31) - (y11 - y31) * (x11 - x21)) / ((x12 - x22) * (x11 - x31) - (x12 - x32) * (x11 - x21));
        float a3 = y11 - a1 * x11 - a2 * x12;
        float a4 = ((y12 - y22) * (x12 - x32) - (y12 - y32) * (x12 - x22)) / ((x11 - x21) * (x12 - x32) - (x11 - x31) * (x12 - x22));
        float a5 = ((y12 - y22) * (x11 - x31) - (y12 - y32) * (x11 - x21)) / ((x12 - x22) * (x11 - x31) - (x12 - x32) * (x11 - x21));
        float a6 = y12 - a4 * x11 - a5 * x12;
        return new AffineTransform(a1, a4, a2, a5, a3, a6);
    }

    /**
     * This method returns the proper pixel position on the image.
     * 
     * @param pos
     *            the relative position (value of range <0, 1> (both inclusive))
     * @param size
     *            the size of the line the pixel lies on (width, heigth or
     *            depth)
     * @return the integer index of the pixel on the line of the specified width
     */
    public int getPixelPosition(float pos, int size) {
        float pixelWidth = 1 / (float) size;
        pos *= size;
        int result = (int) pos;
        // here is where we repair floating point operations errors :)
        if (Math.abs(result - pos) > pixelWidth) {
            ++result;
        }
        return result;
    }

    /**
     * This method returns subimage of the give image. The subimage is
     * constrained by the rectangle coordinates. The source image is unchanged.
     * 
     * @param image
     *            the image to be subimaged
     * @param minX
     *            minimum X position
     * @param minY
     *            minimum Y position
     * @param maxX
     *            maximum X position
     * @param maxY
     *            maximum Y position
     * @return a part of the given image
     */
    public Image getSubimage(Image image, int minX, int minY, int maxX, int maxY) {
        if (minY > maxY) {
            throw new IllegalArgumentException("Minimum Y value is higher than maximum Y value!");
        }
        if (minX > maxX) {
            throw new IllegalArgumentException("Minimum Y value is higher than maximum Y value!");
        }
        if (image.getData().size() > 1) {
            throw new IllegalArgumentException("Only flat images are allowed for subimage operation!");
        }
        if (image.getMipMapSizes() != null) {
            LOGGER.warning("Subimaging image with mipmaps is not yet supported!");
        }

        int width = maxX - minX;
        int height = maxY - minY;
        ByteBuffer data = BufferUtils.createByteBuffer(width * height * (image.getFormat().getBitsPerPixel() >> 3));

        Image result = new Image(image.getFormat(), width, height, data, ColorSpace.sRGB);
        PixelInputOutput pixelIO = PixelIOFactory.getPixelIO(image.getFormat());
        TexturePixel pixel = new TexturePixel();

        for (int x = minX; x < maxX; ++x) {
            for (int y = minY; y < maxY; ++y) {
                pixelIO.read(image, 0, pixel, x, y);
                pixelIO.write(result, 0, pixel, x - minX, y - minY);
            }
        }
        return result;
    }

    /**
     * This method applies the colorband and color factors to image type
     * textures. If there is no colorband defined for the texture or the color
     * factors are all equal to 1.0f then no changes are made.
     * 
     * @param tex
     *            the texture structure
     * @param image
     *            the image that will be altered if necessary
     * @param blenderContext
     *            the blender context
     */
    private void applyColorbandAndColorFactors(Structure tex, Image image, BlenderContext blenderContext) {
        float rfac = ((Number) tex.getFieldValue("rfac")).floatValue();
        float gfac = ((Number) tex.getFieldValue("gfac")).floatValue();
        float bfac = ((Number) tex.getFieldValue("bfac")).floatValue();
        float[][] colorBand = new ColorBand(tex, blenderContext).computeValues();
        int depth = image.getDepth() == 0 ? 1 : image.getDepth();

        if (colorBand != null) {
            TexturePixel pixel = new TexturePixel();
            PixelInputOutput imageIO = PixelIOFactory.getPixelIO(image.getFormat());
            for (int layerIndex = 0; layerIndex < depth; ++layerIndex) {
                for (int x = 0; x < image.getWidth(); ++x) {
                    for (int y = 0; y < image.getHeight(); ++y) {
                        imageIO.read(image, layerIndex, pixel, x, y);

                        int colorbandIndex = (int) (pixel.alpha * 1000.0f);
                        pixel.red = colorBand[colorbandIndex][0] * rfac;
                        pixel.green = colorBand[colorbandIndex][1] * gfac;
                        pixel.blue = colorBand[colorbandIndex][2] * bfac;
                        pixel.alpha = colorBand[colorbandIndex][3];

                        imageIO.write(image, layerIndex, pixel, x, y);
                    }
                }
            }
        } else if (rfac != 1.0f || gfac != 1.0f || bfac != 1.0f) {
            TexturePixel pixel = new TexturePixel();
            PixelInputOutput imageIO = PixelIOFactory.getPixelIO(image.getFormat());
            for (int layerIndex = 0; layerIndex < depth; ++layerIndex) {
                for (int x = 0; x < image.getWidth(); ++x) {
                    for (int y = 0; y < image.getHeight(); ++y) {
                        imageIO.read(image, layerIndex, pixel, x, y);

                        pixel.red *= rfac;
                        pixel.green *= gfac;
                        pixel.blue *= bfac;

                        imageIO.write(image, layerIndex, pixel, x, y);
                    }
                }
            }
        }
    }

    /**
     * This method loads the textre from outside the blend file using the
     * AssetManager that the blend file was loaded with. It returns a texture
     * with a full assetKey that references the original texture so it later
     * doesn't need to ba packed when the model data is serialized. It searches
     * the AssetManager for the full path if the model file is a relative path
     * and will attempt to truncate the path if it is an absolute file path
     * until the path can be found in the AssetManager. If the texture can not
     * be found, it will issue a load attempt for the initial path anyway so the
     * failed load can be reported by the AssetManagers callback methods for
     * failed assets.
     * 
     * @param name
     *            the path to the image
     * @param imaflag
     *            the image flag
     * @param blenderContext
     *            the blender context
     * @return the loaded image or null if the image cannot be found
     */
    protected Texture loadImageFromFile(String name, int imaflag, BlenderContext blenderContext) {
        if (!name.contains(".")) {
            return null; // no extension means not a valid image
        }

        // decide if the mipmaps will be generated
        boolean generateMipmaps = false;
        switch (blenderContext.getBlenderKey().getMipmapGenerationMethod()) {
            case ALWAYS_GENERATE:
                generateMipmaps = true;
                break;
            case NEVER_GENERATE:
                break;
            case GENERATE_WHEN_NEEDED:
                generateMipmaps = (imaflag & 0x04) != 0;
                break;
            default:
                throw new IllegalStateException("Unknown mipmap generation method: " + blenderContext.getBlenderKey().getMipmapGenerationMethod());
        }

        AssetManager assetManager = blenderContext.getAssetManager();
        name = name.replace('\\', '/');
        Texture result = null;

        if (name.startsWith("//")) {
            // This is a relative path, so try to find it relative to the .blend file
            String relativePath = name.substring(2);
            // Augument the path with blender key path
            BlenderKey blenderKey = blenderContext.getBlenderKey();
            int idx = blenderKey.getName().lastIndexOf('/');
            String blenderAssetFolder = blenderKey.getName().substring(0, idx != -1 ? idx : 0);
            String absoluteName = blenderAssetFolder + '/' + relativePath;
            // Directly try to load texture so AssetManager can report missing textures
            try {
                TextureKey key = new TextureKey(absoluteName);
                key.setFlipY(true);
                key.setGenerateMips(generateMipmaps);
                result = assetManager.loadTexture(key);
                result.setKey(key);
            } catch (AssetNotFoundException | AssetLoadException e) {
                LOGGER.fine(e.getLocalizedMessage());
            }
        } else {
            // This is a full path, try to truncate it until the file can be found
            // this works as the assetManager root is most probably a part of the
            // image path. E.g. AssetManager has a locator at c:/Files/ and the
            // texture path is c:/Files/Textures/Models/Image.jpg.
            // For this we create a list with every possible full path name from
            // the asset name to the root. Image.jpg, Models/Image.jpg,
            // Textures/Models/Image.jpg (bingo) etc.
            List<String> assetNames = new ArrayList<String>();
            String[] paths = name.split("\\/");
            StringBuilder sb = new StringBuilder(paths[paths.length - 1]);// the asset name
            assetNames.add(paths[paths.length - 1]);

            for (int i = paths.length - 2; i >= 0; --i) {
                sb.insert(0, '/');
                sb.insert(0, paths[i]);
                assetNames.add(0, sb.toString());
            }
            // Now try to locate the asset
            for (String assetName : assetNames) {
                try {
                    TextureKey key = new TextureKey(assetName);
                    key.setFlipY(true);
                    key.setGenerateMips(generateMipmaps);
                    AssetInfo info = assetManager.locateAsset(key);
                    if (info != null) {
                        Texture texture = assetManager.loadTexture(key);
                        result = texture;
                        // Set key explicitly here if other ways fail
                        texture.setKey(key);
                        // If texture is found return it;
                        return result;
                    }
                } catch (AssetNotFoundException | AssetLoadException e) {
                    LOGGER.fine(e.getLocalizedMessage());
                }
            }
            // The asset was not found in the loop above, call loadTexture with
            // the original path once anyway so that the AssetManager can report
            // the missing asset to subsystems.
            try {
                TextureKey key = new TextureKey(name);
                assetManager.loadTexture(key);
            } catch (AssetNotFoundException | AssetLoadException e) {
                LOGGER.fine(e.getLocalizedMessage());
            }
        }

        return result;
    }

    /**
     * Reads the texture data from the given material or sky structure.
     * @param structure
     *            the structure of material or sky
     * @param diffuseColorArray
     *            array of diffuse colors
     * @param skyTexture
     *            indicates it we're going to read sky texture or not
     * @return a list of combined textures
     * @throws BlenderFileException
     *             an exception is thrown when problems with reading the blend file occur
     */
    @SuppressWarnings("unchecked")
    public List<CombinedTexture> readTextureData(Structure structure, float[] diffuseColorArray, boolean skyTexture) throws BlenderFileException {
        DynamicArray<Pointer> mtexsArray = (DynamicArray<Pointer>) structure.getFieldValue("mtex");
        int separatedTextures = skyTexture ? 0 : ((Number) structure.getFieldValue("septex")).intValue();
        List<TextureData> texturesList = new ArrayList<TextureData>();
        for (int i = 0; i < mtexsArray.getTotalSize(); ++i) {
            Pointer p = mtexsArray.get(i);
            if (p.isNotNull() && (separatedTextures & 1 << i) == 0) {
                TextureData textureData = new TextureData();
                textureData.mtex = p.fetchData().get(0);
                textureData.uvCoordinatesType = skyTexture ? UVCoordinatesType.TEXCO_ORCO.blenderValue : ((Number) textureData.mtex.getFieldValue("texco")).intValue();
                textureData.projectionType = ((Number) textureData.mtex.getFieldValue("mapping")).intValue();
                textureData.uvCoordinatesName = textureData.mtex.getFieldValue("uvName").toString();
                if (textureData.uvCoordinatesName != null && textureData.uvCoordinatesName.trim().length() == 0) {
                    textureData.uvCoordinatesName = null;
                }

                Pointer pTex = (Pointer) textureData.mtex.getFieldValue("tex");
                if (pTex.isNotNull()) {
                    Structure tex = pTex.fetchData().get(0);
                    textureData.textureStructure = tex;
                    texturesList.add(textureData);
                }
            }
        }

        LOGGER.info("Loading model's textures.");
        List<CombinedTexture> loadedTextures = new ArrayList<CombinedTexture>();
        if (blenderContext.getBlenderKey().isOptimiseTextures()) {
            LOGGER.fine("Optimising the useage of model's textures.");
            Map<Number, List<TextureData>> textureDataMap = this.sortTextures(texturesList);
            for (Entry<Number, List<TextureData>> entry : textureDataMap.entrySet()) {
                if (entry.getValue().size() > 0) {
                    CombinedTexture combinedTexture = new CombinedTexture(entry.getKey().intValue(), !skyTexture);
                    for (TextureData textureData : entry.getValue()) {
                        int texflag = ((Number) textureData.mtex.getFieldValue("texflag")).intValue();
                        boolean negateTexture = (texflag & 0x04) != 0;
                        Texture texture = this.getTexture(textureData.textureStructure, textureData.mtex, blenderContext);
                        if (texture != null) {
                            int blendType = ((Number) textureData.mtex.getFieldValue("blendtype")).intValue();
                            float[] color = new float[] { ((Number) textureData.mtex.getFieldValue("r")).floatValue(), ((Number) textureData.mtex.getFieldValue("g")).floatValue(), ((Number) textureData.mtex.getFieldValue("b")).floatValue() };
                            float colfac = ((Number) textureData.mtex.getFieldValue("colfac")).floatValue();
                            TextureBlender textureBlender = TextureBlenderFactory.createTextureBlender(texture.getImage().getFormat(), texflag, negateTexture, blendType, diffuseColorArray, color, colfac);
                            combinedTexture.add(texture, textureBlender, textureData.uvCoordinatesType, textureData.projectionType, textureData.textureStructure, textureData.uvCoordinatesName, blenderContext);
                        }
                    }
                    if (combinedTexture.getTexturesCount() > 0) {
                        loadedTextures.add(combinedTexture);
                    }
                }
            }
        } else {
            LOGGER.fine("No textures optimisation applied.");
            int[] mappings = new int[] { MaterialContext.MTEX_COL, MaterialContext.MTEX_NOR, MaterialContext.MTEX_EMIT, MaterialContext.MTEX_SPEC, MaterialContext.MTEX_ALPHA, MaterialContext.MTEX_AMB };
            for (TextureData textureData : texturesList) {
                Texture texture = this.getTexture(textureData.textureStructure, textureData.mtex, blenderContext);
                if (texture != null) {
                    Number mapto = (Number) textureData.mtex.getFieldValue("mapto");
                    int texflag = ((Number) textureData.mtex.getFieldValue("texflag")).intValue();
                    boolean negateTexture = (texflag & 0x04) != 0;

                    boolean colorSet = false;
                    for (int i = 0; i < mappings.length; ++i) {
                        if ((mappings[i] & mapto.intValue()) != 0) {
                            if(mappings[i] == MaterialContext.MTEX_COL) {
                                colorSet = true;
                            } else if(colorSet && mappings[i] == MaterialContext.MTEX_ALPHA) {
                                continue;
                            }
                            
                            CombinedTexture combinedTexture = new CombinedTexture(mappings[i], !skyTexture);
                            int blendType = ((Number) textureData.mtex.getFieldValue("blendtype")).intValue();
                            float[] color = new float[] { ((Number) textureData.mtex.getFieldValue("r")).floatValue(), ((Number) textureData.mtex.getFieldValue("g")).floatValue(), ((Number) textureData.mtex.getFieldValue("b")).floatValue() };
                            float colfac = ((Number) textureData.mtex.getFieldValue("colfac")).floatValue();
                            TextureBlender textureBlender = TextureBlenderFactory.createTextureBlender(texture.getImage().getFormat(), texflag, negateTexture, blendType, diffuseColorArray, color, colfac);
                            combinedTexture.add(texture, textureBlender, textureData.uvCoordinatesType, textureData.projectionType, textureData.textureStructure, textureData.uvCoordinatesName, blenderContext);
                            if (combinedTexture.getTexturesCount() > 0) {// the added texture might not have been accepted (if for example loading generated textures is disabled)
                                loadedTextures.add(combinedTexture);
                            }
                        }
                    }
                }

            }
        }

        return loadedTextures;
    }

    /**
     * This method sorts the textures by their mapping type. In each group only
     * textures of one type are put (either two- or three-dimensional).
     * 
     * @return a map with sorted textures
     */
    private Map<Number, List<TextureData>> sortTextures(List<TextureData> textures) {
        int[] mappings = new int[] { MaterialContext.MTEX_COL, MaterialContext.MTEX_NOR, MaterialContext.MTEX_EMIT, MaterialContext.MTEX_SPEC, MaterialContext.MTEX_ALPHA, MaterialContext.MTEX_AMB };
        Map<Number, List<TextureData>> result = new HashMap<Number, List<TextureData>>();
        for (TextureData data : textures) {
            Number mapto = (Number) data.mtex.getFieldValue("mapto");
            
            boolean colorSet = false;
            for (int i = 0; i < mappings.length; ++i) {
                if ((mappings[i] & mapto.intValue()) != 0) {
                    if(mappings[i] == MaterialContext.MTEX_COL) {
                        colorSet = true;
                    } else if(colorSet && mappings[i] == MaterialContext.MTEX_ALPHA) {
                        continue;
                    }
                    
                    List<TextureData> datas = result.get(mappings[i]);
                    if (datas == null) {
                        datas = new ArrayList<TextureData>();
                        result.put(mappings[i], datas);
                    }
                    datas.add(data);
                }
            }
        }
        return result;
    }

    private static class TextureData {
        public Structure mtex;
        public Structure textureStructure;
        public int       uvCoordinatesType;
        public int       projectionType;
        /** The name of the user's UV coordinates that are used for this texture. */
        public String    uvCoordinatesName;
    }
}
