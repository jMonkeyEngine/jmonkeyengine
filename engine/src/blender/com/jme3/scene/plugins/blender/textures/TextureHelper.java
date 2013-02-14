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

import com.jme3.asset.AssetInfo;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jme3tools.converters.ImageToAwt;
import jme3tools.converters.RGB565;

import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.asset.BlenderKey;
import com.jme3.asset.BlenderKey.FeaturesToLoad;
import com.jme3.asset.GeneratedTextureKey;
import com.jme3.asset.TextureKey;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.FileBlockHeader;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.textures.generating.TextureGeneratorFactory;
import com.jme3.scene.plugins.blender.textures.io.PixelIOFactory;
import com.jme3.scene.plugins.blender.textures.io.PixelInputOutput;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;

/**
 * A class that is used in texture calculations.
 * 
 * @author Marcin Roguski
 */
public class TextureHelper extends AbstractBlenderHelper {
    private static final Logger     LOGGER           = Logger.getLogger(TextureHelper.class.getName());

    // texture types
    public static final int         TEX_NONE         = 0;
    public static final int         TEX_CLOUDS       = 1;
    public static final int         TEX_WOOD         = 2;
    public static final int         TEX_MARBLE       = 3;
    public static final int         TEX_MAGIC        = 4;
    public static final int         TEX_BLEND        = 5;
    public static final int         TEX_STUCCI       = 6;
    public static final int         TEX_NOISE        = 7;
    public static final int         TEX_IMAGE        = 8;
    public static final int         TEX_PLUGIN       = 9;
    public static final int         TEX_ENVMAP       = 10;
    public static final int         TEX_MUSGRAVE     = 11;
    public static final int         TEX_VORONOI      = 12;
    public static final int         TEX_DISTNOISE    = 13;
    public static final int         TEX_POINTDENSITY = 14;                                             // v.
                                                                                                        // 25+
    public static final int         TEX_VOXELDATA    = 15;                                             // v.
                                                                                                        // 25+

    private TextureGeneratorFactory textureGeneratorFactory;

    /**
     * This constructor parses the given blender version and stores the result.
     * It creates noise generator and texture generators.
     * 
     * @param blenderVersion
     *            the version read from the blend file
     * @param fixUpAxis
     *            a variable that indicates if the Y asxis is the UP axis or not
     */
    public TextureHelper(String blenderVersion, boolean fixUpAxis) {
        super(blenderVersion, false);
        textureGeneratorFactory = new TextureGeneratorFactory(blenderVersion);
    }

    /**
     * This class returns a texture read from the file or from packed blender
     * data. The returned texture has the name set to the value of its blender
     * type.
     * 
     * @param tex
     *            texture structure filled with data
     * @param blenderContext
     *            the blender context
     * @return the texture that can be used by JME engine
     * @throws BlenderFileException
     *             this exception is thrown when the blend file structure is
     *             somehow invalid or corrupted
     */
    public Texture getTexture(Structure tex, Structure mTex, BlenderContext blenderContext) throws BlenderFileException {
        Texture result = (Texture) blenderContext.getLoadedFeature(tex.getOldMemoryAddress(), LoadedFeatureDataType.LOADED_FEATURE);
        if (result != null) {
            return result;
        }
        int type = ((Number) tex.getFieldValue("type")).intValue();
        int imaflag = ((Number) tex.getFieldValue("imaflag")).intValue();

        switch (type) {
            case TEX_IMAGE:// (it is first because probably this will be most commonly used)
                Pointer pImage = (Pointer) tex.getFieldValue("ima");
                if (pImage.isNotNull()) {
                    Structure image = pImage.fetchData(blenderContext.getInputStream()).get(0);
                    Texture loadedTexture = this.loadTexture(image, imaflag, blenderContext);
                    if (loadedTexture != null) {
                        result = loadedTexture;
                        this.applyColorbandAndColorFactors(tex, result.getImage(), blenderContext);
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
                result = new GeneratedTexture(tex, mTex, textureGeneratorFactory.createTextureGenerator(type), blenderContext);
                break;
            case TEX_NONE:// No texture, do nothing
                break;
            case TEX_POINTDENSITY:
            case TEX_VOXELDATA:
            case TEX_PLUGIN:
            case TEX_ENVMAP:
                LOGGER.log(Level.WARNING, "Unsupported texture type: {0} for texture: {1}", new Object[] { type, tex.getName() });
                break;
            default:
                throw new BlenderFileException("Unknown texture type: " + type + " for texture: " + tex.getName());
        }
        if (result != null) {
            result.setName(tex.getName());
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
                result.setKey(new GeneratedTextureKey(tex.getName()));
            }

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Adding texture {0} to the loaded features with OMA = {1}", new Object[] { result.getName(), tex.getOldMemoryAddress() });
            }
            blenderContext.addLoadedFeatures(tex.getOldMemoryAddress(), tex.getName(), tex, result);
        }
        return result;
    }

    /**
     * This method converts the given texture into normal-map texture.
     * 
     * @param source
     *            the source texture
     * @param strengthFactor
     *            the normal strength factor
     * @return normal-map texture
     */
    public Image convertToNormalMapTexture(Image source, float strengthFactor) {
        BufferedImage sourceImage = ImageToAwt.convert(source, false, false, 0);

        BufferedImage heightMap = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        BufferedImage bumpMap = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        ColorConvertOp gscale = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        gscale.filter(sourceImage, heightMap);

        Vector3f S = new Vector3f();
        Vector3f T = new Vector3f();
        Vector3f N = new Vector3f();

        for (int x = 0; x < bumpMap.getWidth(); ++x) {
            for (int y = 0; y < bumpMap.getHeight(); ++y) {
                // generating bump pixel
                S.x = 1;
                S.y = 0;
                S.z = strengthFactor * this.getHeight(heightMap, x + 1, y) - strengthFactor * this.getHeight(heightMap, x - 1, y);
                T.x = 0;
                T.y = 1;
                T.z = strengthFactor * this.getHeight(heightMap, x, y + 1) - strengthFactor * this.getHeight(heightMap, x, y - 1);

                float den = (float) Math.sqrt(S.z * S.z + T.z * T.z + 1);
                N.x = -S.z;
                N.y = -T.z;
                N.z = 1;
                N.divideLocal(den);

                // setting thge pixel in the result image
                bumpMap.setRGB(x, y, this.vectorToColor(N.x, N.y, N.z));
            }
        }
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(source.getWidth() * source.getHeight() * 3);
        ImageToAwt.convert(bumpMap, Format.RGB8, byteBuffer);
        return new Image(Format.RGB8, source.getWidth(), source.getHeight(), byteBuffer);
    }

    /**
     * This method decompresses the given image. If the given image is already
     * decompressed nothing happens and it is simply returned.
     * 
     * @param image
     *            the image to decompress
     * @return the decompressed image
     */
    public Image decompress(Image image) {
        Format format = image.getFormat();
        int depth = image.getDepth();
        if (depth == 0) {
            depth = 1;
        }
        ArrayList<ByteBuffer> dataArray = new ArrayList<ByteBuffer>(depth);
        int[] sizes = image.getMipMapSizes() != null ? image.getMipMapSizes() : new int[1];
        int[] newMipmapSizes = image.getMipMapSizes() != null ? new int[image.getMipMapSizes().length] : null;

        for (int dataLayerIndex = 0; dataLayerIndex < depth; ++dataLayerIndex) {
            ByteBuffer data = image.getData(dataLayerIndex);
            data.rewind();
            if (sizes.length == 1) {
                sizes[0] = data.remaining();
            }
            float widthToHeightRatio = image.getWidth() / image.getHeight();// this should always be constant for each mipmap
            List<DDSTexelData> texelDataList = new ArrayList<DDSTexelData>(sizes.length);
            int maxPosition = 0, resultSize = 0;

            for (int sizeIndex = 0; sizeIndex < sizes.length; ++sizeIndex) {
                maxPosition += sizes[sizeIndex];
                DDSTexelData texelData = new DDSTexelData(sizes[sizeIndex], widthToHeightRatio, format);
                texelDataList.add(texelData);
                switch (format) {
                    case DXT1:// BC1
                    case DXT1A:
                        while (data.position() < maxPosition) {
                            TexturePixel[] colors = new TexturePixel[] { new TexturePixel(), new TexturePixel(), new TexturePixel(), new TexturePixel() };
                            short c0 = data.getShort();
                            short c1 = data.getShort();
                            int col0 = RGB565.RGB565_to_ARGB8(c0);
                            int col1 = RGB565.RGB565_to_ARGB8(c1);
                            colors[0].fromARGB8(col0);
                            colors[1].fromARGB8(col1);

                            if (col0 > col1) {
                                // creating color2 = 2/3color0 + 1/3color1
                                colors[2].fromPixel(colors[0]);
                                colors[2].mult(2);
                                colors[2].add(colors[1]);
                                colors[2].divide(3);

                                // creating color3 = 1/3color0 + 2/3color1;
                                colors[3].fromPixel(colors[1]);
                                colors[3].mult(2);
                                colors[3].add(colors[0]);
                                colors[3].divide(3);
                            } else {
                                // creating color2 = 1/2color0 + 1/2color1
                                colors[2].fromPixel(colors[0]);
                                colors[2].add(colors[1]);
                                colors[2].mult(0.5f);

                                colors[3].fromARGB8(0);
                            }
                            int indexes = data.getInt();// 4-byte table with color indexes in decompressed table
                            texelData.add(colors, indexes);
                        }
                        break;
                    case DXT3:// BC2
                        while (data.position() < maxPosition) {
                            TexturePixel[] colors = new TexturePixel[] { new TexturePixel(), new TexturePixel(), new TexturePixel(), new TexturePixel() };
                            long alpha = data.getLong();
                            float[] alphas = new float[16];
                            long alphasIndex = 0;
                            for (int i = 0; i < 16; ++i) {
                                alphasIndex |= i << i * 4;
                                byte a = (byte) ((alpha >> i * 4 & 0x0F) << 4);
                                alphas[i] = a >= 0 ? a / 255.0f : 1.0f - ~a / 255.0f;
                            }

                            short c0 = data.getShort();
                            short c1 = data.getShort();
                            int col0 = RGB565.RGB565_to_ARGB8(c0);
                            int col1 = RGB565.RGB565_to_ARGB8(c1);
                            colors[0].fromARGB8(col0);
                            colors[1].fromARGB8(col1);

                            // creating color2 = 2/3color0 + 1/3color1
                            colors[2].fromPixel(colors[0]);
                            colors[2].mult(2);
                            colors[2].add(colors[1]);
                            colors[2].divide(3);

                            // creating color3 = 1/3color0 + 2/3color1;
                            colors[3].fromPixel(colors[1]);
                            colors[3].mult(2);
                            colors[3].add(colors[0]);
                            colors[3].divide(3);

                            int indexes = data.getInt();// 4-byte table with color indexes in decompressed table
                            texelData.add(colors, indexes, alphas, alphasIndex);
                        }
                        break;
                    case DXT5:// BC3
                        float[] alphas = new float[8];
                        while (data.position() < maxPosition) {
                            TexturePixel[] colors = new TexturePixel[] { new TexturePixel(), new TexturePixel(), new TexturePixel(), new TexturePixel() };
                            alphas[0] = data.get() * 255.0f;
                            alphas[1] = data.get() * 255.0f;
                            long alphaIndices = data.get() | data.get() << 8 | data.get() << 16 | data.get() << 24 | data.get() << 32 | data.get() << 40;
                            if (alphas[0] > alphas[1]) {// 6 interpolated alpha values.
                                alphas[2] = (6 * alphas[0] + alphas[1]) / 7;
                                alphas[3] = (5 * alphas[0] + 2 * alphas[1]) / 7;
                                alphas[4] = (4 * alphas[0] + 3 * alphas[1]) / 7;
                                alphas[5] = (3 * alphas[0] + 4 * alphas[1]) / 7;
                                alphas[6] = (2 * alphas[0] + 5 * alphas[1]) / 7;
                                alphas[7] = (alphas[0] + 6 * alphas[1]) / 7;
                            } else {
                                alphas[2] = (4 * alphas[0] + alphas[1]) * 0.2f;
                                alphas[3] = (3 * alphas[0] + 2 * alphas[1]) * 0.2f;
                                alphas[4] = (2 * alphas[0] + 3 * alphas[1]) * 0.2f;
                                alphas[5] = (alphas[0] + 4 * alphas[1]) * 0.2f;
                                alphas[6] = 0;
                                alphas[7] = 1;
                            }

                            short c0 = data.getShort();
                            short c1 = data.getShort();
                            int col0 = RGB565.RGB565_to_ARGB8(c0);
                            int col1 = RGB565.RGB565_to_ARGB8(c1);
                            colors[0].fromARGB8(col0);
                            colors[1].fromARGB8(col1);

                            // creating color2 = 2/3color0 + 1/3color1
                            colors[2].fromPixel(colors[0]);
                            colors[2].mult(2);
                            colors[2].add(colors[1]);
                            colors[2].divide(3);

                            // creating color3 = 1/3color0 + 2/3color1;
                            colors[3].fromPixel(colors[1]);
                            colors[3].mult(2);
                            colors[3].add(colors[0]);
                            colors[3].divide(3);

                            int indexes = data.getInt();// 4-byte table with color indexes in decompressed table
                            texelData.add(colors, indexes, alphas, alphaIndices);
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unknown compressed format: " + format);
                }
                newMipmapSizes[sizeIndex] = texelData.getSizeInBytes();
                resultSize += texelData.getSizeInBytes();
            }
            byte[] bytes = new byte[resultSize];
            int offset = 0;
            byte[] pixelBytes = new byte[4];
            for (DDSTexelData texelData : texelDataList) {
                for (int i = 0; i < texelData.getPixelWidth(); ++i) {
                    for (int j = 0; j < texelData.getPixelHeight(); ++j) {
                        if (texelData.getRGBA8(i, j, pixelBytes)) {
                            bytes[offset + (j * texelData.getPixelWidth() + i) * 4] = pixelBytes[0];
                            bytes[offset + (j * texelData.getPixelWidth() + i) * 4 + 1] = pixelBytes[1];
                            bytes[offset + (j * texelData.getPixelWidth() + i) * 4 + 2] = pixelBytes[2];
                            bytes[offset + (j * texelData.getPixelWidth() + i) * 4 + 3] = pixelBytes[3];
                        } else {
                            break;
                        }
                    }
                }
                offset += texelData.getSizeInBytes();
            }
            dataArray.add(BufferUtils.createByteBuffer(bytes));
        }

        Image result = depth > 1 ? new Image(Format.RGBA8, image.getWidth(), image.getHeight(), depth, dataArray) : new Image(Format.RGBA8, image.getWidth(), image.getHeight(), dataArray.get(0));
        if (newMipmapSizes != null) {
            result.setMipMapSizes(newMipmapSizes);
        }
        return result;
    }

    /**
     * This method returns the height represented by the specified pixel in the
     * given texture. The given texture should be a height-map.
     * 
     * @param image
     *            the height-map texture
     * @param x
     *            pixel's X coordinate
     * @param y
     *            pixel's Y coordinate
     * @return height reprezented by the given texture in the specified location
     */
    protected int getHeight(BufferedImage image, int x, int y) {
        if (x < 0) {
            x = 0;
        } else if (x >= image.getWidth()) {
            x = image.getWidth() - 1;
        }
        if (y < 0) {
            y = 0;
        } else if (y >= image.getHeight()) {
            y = image.getHeight() - 1;
        }
        return image.getRGB(x, y) & 0xff;
    }

    /**
     * This method transforms given vector's coordinates into ARGB color (A is
     * always = 255).
     * 
     * @param x
     *            X factor of the vector
     * @param y
     *            Y factor of the vector
     * @param z
     *            Z factor of the vector
     * @return color representation of the given vector
     */
    protected int vectorToColor(float x, float y, float z) {
        int r = Math.round(255 * (x + 1f) / 2f);
        int g = Math.round(255 * (y + 1f) / 2f);
        int b = Math.round(255 * (z + 1f) / 2f);
        return (255 << 24) + (r << 16) + (g << 8) + b;
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
    protected Texture loadTexture(Structure imageStructure, int imaflag, BlenderContext blenderContext) throws BlenderFileException {
        LOGGER.log(Level.FINE, "Fetching texture with OMA = {0}", imageStructure.getOldMemoryAddress());
        Texture result = null;
        Image im = (Image) blenderContext.getLoadedFeature(imageStructure.getOldMemoryAddress(), LoadedFeatureDataType.LOADED_FEATURE);
        if (im == null) {
            String texturePath = imageStructure.getFieldValue("name").toString();
            Pointer pPackedFile = (Pointer) imageStructure.getFieldValue("packedfile");
            if (pPackedFile.isNull()) {
                LOGGER.log(Level.FINE, "Reading texture from file: {0}", texturePath);
                result = this.loadImageFromFile(texturePath, imaflag, blenderContext);
            } else {
                LOGGER.fine("Packed texture. Reading directly from the blend file!");
                Structure packedFile = pPackedFile.fetchData(blenderContext.getInputStream()).get(0);
                Pointer pData = (Pointer) packedFile.getFieldValue("data");
                FileBlockHeader dataFileBlock = blenderContext.getFileBlock(pData.getOldMemoryAddress());
                blenderContext.getInputStream().setPosition(dataFileBlock.getBlockPosition());
                ImageLoader imageLoader = new ImageLoader();

                // Should the texture be flipped? It works for sinbad ..
                result = new Texture2D(imageLoader.loadImage(blenderContext.getInputStream(), dataFileBlock.getBlockPosition(), true));
            }
        } else {
            result = new Texture2D(im);
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

        Image result = new Image(image.getFormat(), width, height, data);
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
        // @Marcin: please, please disable the use of "TAB"
        // in your IDE in favor of four spaces.
        // All your code looks like this for us: http://i.imgur.com/sGcBv6Q.png
        // spaces always work ;)
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
                key.setAsCube(false);
                key.setFlipY(true);
                key.setGenerateMips(generateMipmaps);
                result = assetManager.loadTexture(key);
                result.setKey(key);
            } catch (AssetNotFoundException e) {
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
                    key.setAsCube(false);
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
                } catch (AssetNotFoundException e) {
                    LOGGER.fine(e.getLocalizedMessage());
                }
            }
            // The asset was not found in the loop above, call loadTexture with
            // the original path once anyway so that the AssetManager can report
            // the missing asset to subsystems.
            try {
                TextureKey key = new TextureKey(name);
                assetManager.loadTexture(key);
            } catch (AssetNotFoundException e) {
                LOGGER.fine(e.getLocalizedMessage());
            }
        }

        return result;
    }

    @Override
    public boolean shouldBeLoaded(Structure structure, BlenderContext blenderContext) {
        return (blenderContext.getBlenderKey().getFeaturesToLoad() & FeaturesToLoad.TEXTURES) != 0;
    }
}