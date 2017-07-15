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
package com.jme3.scene.plugins.blender.textures.blending;

import com.jme3.math.FastMath;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.textures.TexturePixel;
import com.jme3.scene.plugins.blender.textures.io.PixelIOFactory;
import com.jme3.scene.plugins.blender.textures.io.PixelInputOutput;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * <p>
 * The class that is responsible for blending the following texture types: 
 * </p>
 * <ul>
 * <li>RGBA8</li>
 * <li>ABGR8</li>
 * <li>BGR8</li>
 * <li>RGB8</li>
 * </ul>
 * 
 * <p>
 * Not yet supported (but will be):
 * </p>
 * <ul>
 * <li>ARGB4444</li>
 * <li>RGB10</li>
 * <li>RGB111110F</li>
 * <li>RGB16</li>
 * <li>RGB16F</li>
 * <li>RGB16F_to_RGB111110F</li>
 * <li>RGB16F_to_RGB9E5</li>
 * <li>RGB32F</li>
 * <li>RGB565</li>
 * <li>RGB5A1</li>
 * <li>RGB9E5</li>
 * <li>RGBA16</li>
 * <li>RGBA16F</li>
 * </ul>
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureBlenderAWT extends AbstractTextureBlender {
    public TextureBlenderAWT(int flag, boolean negateTexture, int blendType, float[] materialColor, float[] color, float blendFactor) {
        super(flag, negateTexture, blendType, materialColor, color, blendFactor);
    }

    @Override
    public Image blend(Image image, Image baseImage, BlenderContext blenderContext) {
        this.prepareImagesForBlending(image, baseImage);

        float[] pixelColor = new float[] { color[0], color[1], color[2], 1.0f };
        Format format = image.getFormat();

        PixelInputOutput basePixelIO = null, pixelReader = PixelIOFactory.getPixelIO(format);
        TexturePixel basePixel = null, pixel = new TexturePixel();
        float[] materialColor = this.materialColor;
        if (baseImage != null) {
            basePixelIO = PixelIOFactory.getPixelIO(baseImage.getFormat());
            materialColor = new float[this.materialColor.length];
            basePixel = new TexturePixel();
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int depth = image.getDepth();
        if (depth == 0) {
            depth = 1;
        }
        int bytesPerPixel = image.getFormat().getBitsPerPixel() >> 3;
        ArrayList<ByteBuffer> dataArray = new ArrayList<ByteBuffer>(depth);

        float[] resultPixel = new float[4];
        for (int dataLayerIndex = 0; dataLayerIndex < depth; ++dataLayerIndex) {
            ByteBuffer data = image.getData(dataLayerIndex);
            data.rewind();
            int imagePixelCount = data.limit() / bytesPerPixel;
            ByteBuffer newData = BufferUtils.createByteBuffer(imagePixelCount * 4);

            int dataIndex = 0, x = 0, y = 0, index = 0;
            while (index < data.limit()) {
                // getting the proper material color if the base texture is applied
                if (basePixelIO != null) {
                    basePixelIO.read(baseImage, dataLayerIndex, basePixel, x, y);
                    basePixel.toRGBA(materialColor);
                    ++x;
                    if (x >= width) {
                        x = 0;
                        ++y;
                    }
                }

                // reading the current texture's pixel
                pixelReader.read(image, dataLayerIndex, pixel, index);
                index += bytesPerPixel;
                pixel.toRGBA(pixelColor);
                if (negateTexture) {
                    pixel.negate();
                }

                this.blendPixel(resultPixel, materialColor, pixelColor, blenderContext);
                newData.put(dataIndex++, (byte) (resultPixel[0] * 255.0f));
                newData.put(dataIndex++, (byte) (resultPixel[1] * 255.0f));
                newData.put(dataIndex++, (byte) (resultPixel[2] * 255.0f));
                newData.put(dataIndex++, (byte) (pixelColor[3] * 255.0f));
            }
            dataArray.add(newData);
        }

        Image result = depth > 1 ? new Image(Format.RGBA8, width, height, depth, dataArray, ColorSpace.Linear) : new Image(Format.RGBA8, width, height, dataArray.get(0), ColorSpace.Linear);
        if (image.getMipMapSizes() != null) {
            result.setMipMapSizes(image.getMipMapSizes().clone());
        }
        return result;
    }

    /**
     * This method blends the single pixel depending on the blending type.
     * 
     * @param result
     *            the result pixel
     * @param materialColor
     *            the material color
     * @param pixelColor
     *            the pixel color
     * @param blenderContext
     *            the blender context
     */
    protected void blendPixel(float[] result, float[] materialColor, float[] pixelColor, BlenderContext blenderContext) {
        // We calculate first the importance of the texture (colFactor * texAlphaValue)
        float blendFactor = this.blendFactor * pixelColor[3];
        // Then, we get the object material factor ((1 - texture importance) * matAlphaValue) 
        float oneMinusFactor = (1f - blendFactor) * materialColor[3];
        // Finally, we can get the final blendFactor, which is 1 - the material factor.
        blendFactor = 1f - oneMinusFactor;
        
        // --- Compact method ---
        // float blendFactor = this.blendFactor * (1f - ((1f - pixelColor[3]) * materialColor[3]));
        // float oneMinusFactor = 1f - blendFactor;

        float col;
        
        switch (blendType) {
            case MTEX_BLEND:
                result[0] = blendFactor * pixelColor[0] + oneMinusFactor * materialColor[0];
                result[1] = blendFactor * pixelColor[1] + oneMinusFactor * materialColor[1];
                result[2] = blendFactor * pixelColor[2] + oneMinusFactor * materialColor[2];
                break;
            case MTEX_MUL:
                result[0] = (oneMinusFactor + blendFactor * materialColor[0]) * pixelColor[0];
                result[1] = (oneMinusFactor + blendFactor * materialColor[1]) * pixelColor[1];
                result[2] = (oneMinusFactor + blendFactor * materialColor[2]) * pixelColor[2];
                break;
            case MTEX_DIV:
                if (pixelColor[0] != 0.0) {
                    result[0] = (oneMinusFactor * materialColor[0] + blendFactor * materialColor[0] / pixelColor[0]) * 0.5f;
                }
                if (pixelColor[1] != 0.0) {
                    result[1] = (oneMinusFactor * materialColor[1] + blendFactor * materialColor[1] / pixelColor[1]) * 0.5f;
                }
                if (pixelColor[2] != 0.0) {
                    result[2] = (oneMinusFactor * materialColor[2] + blendFactor * materialColor[2] / pixelColor[2]) * 0.5f;
                }
                break;
            case MTEX_SCREEN:
                result[0] = 1.0f - (oneMinusFactor + blendFactor * (1.0f - materialColor[0])) * (1.0f - pixelColor[0]);
                result[1] = 1.0f - (oneMinusFactor + blendFactor * (1.0f - materialColor[1])) * (1.0f - pixelColor[1]);
                result[2] = 1.0f - (oneMinusFactor + blendFactor * (1.0f - materialColor[2])) * (1.0f - pixelColor[2]);
                break;
            case MTEX_OVERLAY:
                if (materialColor[0] < 0.5f) {
                    result[0] = pixelColor[0] * (oneMinusFactor + 2.0f * blendFactor * materialColor[0]);
                } else {
                    result[0] = 1.0f - (oneMinusFactor + 2.0f * blendFactor * (1.0f - materialColor[0])) * (1.0f - pixelColor[0]);
                }
                if (materialColor[1] < 0.5f) {
                    result[1] = pixelColor[1] * (oneMinusFactor + 2.0f * blendFactor * materialColor[1]);
                } else {
                    result[1] = 1.0f - (oneMinusFactor + 2.0f * blendFactor * (1.0f - materialColor[1])) * (1.0f - pixelColor[1]);
                }
                if (materialColor[2] < 0.5f) {
                    result[2] = pixelColor[2] * (oneMinusFactor + 2.0f * blendFactor * materialColor[2]);
                } else {
                    result[2] = 1.0f - (oneMinusFactor + 2.0f * blendFactor * (1.0f - materialColor[2])) * (1.0f - pixelColor[2]);
                }
                break;
            case MTEX_SUB:
                result[0] = materialColor[0] - blendFactor * pixelColor[0];
                result[1] = materialColor[1] - blendFactor * pixelColor[1];
                result[2] = materialColor[2] - blendFactor * pixelColor[2];
                result[0] = FastMath.clamp(result[0], 0.0f, 1.0f);
                result[1] = FastMath.clamp(result[1], 0.0f, 1.0f);
                result[2] = FastMath.clamp(result[2], 0.0f, 1.0f);
                break;
            case MTEX_ADD:
                result[0] = (blendFactor * pixelColor[0] + materialColor[0]) * 0.5f;
                result[1] = (blendFactor * pixelColor[1] + materialColor[1]) * 0.5f;
                result[2] = (blendFactor * pixelColor[2] + materialColor[2]) * 0.5f;
                break;
            case MTEX_DIFF:
                result[0] = oneMinusFactor * materialColor[0] + blendFactor * Math.abs(materialColor[0] - pixelColor[0]);
                result[1] = oneMinusFactor * materialColor[1] + blendFactor * Math.abs(materialColor[1] - pixelColor[1]);
                result[2] = oneMinusFactor * materialColor[2] + blendFactor * Math.abs(materialColor[2] - pixelColor[2]);
                break;
            case MTEX_DARK:
                col = blendFactor * pixelColor[0];
                result[0] = col < materialColor[0] ? col : materialColor[0];
                col = blendFactor * pixelColor[1];
                result[1] = col < materialColor[1] ? col : materialColor[1];
                col = blendFactor * pixelColor[2];
                result[2] = col < materialColor[2] ? col : materialColor[2];
                break;
            case MTEX_LIGHT:
                col = blendFactor * pixelColor[0];
                result[0] = col > materialColor[0] ? col : materialColor[0];
                col = blendFactor * pixelColor[1];
                result[1] = col > materialColor[1] ? col : materialColor[1];
                col = blendFactor * pixelColor[2];
                result[2] = col > materialColor[2] ? col : materialColor[2];
                break;
            case MTEX_BLEND_HUE:
            case MTEX_BLEND_SAT:
            case MTEX_BLEND_VAL:
            case MTEX_BLEND_COLOR:
                System.arraycopy(materialColor, 0, result, 0, 3);
                this.blendHSV(blendType, result, blendFactor, pixelColor, blenderContext);
                break;
            default:
                throw new IllegalStateException("Unknown blend type: " + blendType);
        }
    }
}
