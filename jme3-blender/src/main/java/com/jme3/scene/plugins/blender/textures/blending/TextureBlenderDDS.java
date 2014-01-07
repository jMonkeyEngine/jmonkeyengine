package com.jme3.scene.plugins.blender.textures.blending;

import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.textures.TexturePixel;
import com.jme3.scene.plugins.blender.textures.io.PixelIOFactory;
import com.jme3.scene.plugins.blender.textures.io.PixelInputOutput;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import jme3tools.converters.RGB565;

/**
 * The class that is responsible for blending the following texture types: <li>DXT1 <li>DXT1A <li>DXT3 <li>DXT5
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureBlenderDDS extends TextureBlenderAWT {
    public TextureBlenderDDS(int flag, boolean negateTexture, int blendType, float[] materialColor, float[] color, float blendFactor) {
        super(flag, negateTexture, blendType, materialColor, color, blendFactor);
    }

    @Override
    public Image blend(Image image, Image baseImage, BlenderContext blenderContext) {
        this.prepareImagesForBlending(image, baseImage);

        Format format = image.getFormat();
        int width = image.getWidth();
        int height = image.getHeight();
        int depth = image.getDepth();
        if (depth == 0) {
            depth = 1;
        }
        ArrayList<ByteBuffer> dataArray = new ArrayList<ByteBuffer>(depth);

        PixelInputOutput basePixelIO = null;
        float[][] compressedMaterialColor = null;
        TexturePixel[] baseTextureColors = null;
        if (baseImage != null) {
            basePixelIO = PixelIOFactory.getPixelIO(baseImage.getFormat());
            compressedMaterialColor = new float[2][4];
            baseTextureColors = new TexturePixel[] { new TexturePixel(), new TexturePixel() };
        }

        float[] resultPixel = new float[4];
        float[] pixelColor = new float[4];
        TexturePixel[] colors = new TexturePixel[] { new TexturePixel(), new TexturePixel() };
        int baseXTexelIndex = 0, baseYTexelIndex = 0;
        float[] alphas = new float[] { 1, 1 };
        for (int dataLayerIndex = 0; dataLayerIndex < depth; ++dataLayerIndex) {
            ByteBuffer data = image.getData(dataLayerIndex);
            data.rewind();
            ByteBuffer newData = BufferUtils.createByteBuffer(data.remaining());
            while (data.hasRemaining()) {
                if (format == Format.DXT3) {
                    long alpha = data.getLong();
                    // get alpha for first and last pixel that is compressed in the texel
                    byte alpha0 = (byte) (alpha << 4 & 0xFF);
                    byte alpha1 = (byte) (alpha >> 60 & 0xFF);
                    alphas[0] = alpha0 >= 0 ? alpha0 / 255.0f : 1.0f - ~alpha0 / 255.0f;
                    alphas[1] = alpha1 >= 0 ? alpha1 / 255.0f : 1.0f - ~alpha1 / 255.0f;
                    newData.putLong(alpha);
                } else if (format == Format.DXT5) {
                    byte alpha0 = data.get();
                    byte alpha1 = data.get();
                    alphas[0] = alpha0 >= 0 ? alpha0 / 255.0f : 1.0f - ~alpha0 / 255.0f;
                    alphas[1] = alpha1 >= 0 ? alpha0 / 255.0f : 1.0f - ~alpha0 / 255.0f;
                    newData.put(alpha0);
                    newData.put(alpha1);
                    // only read the next 6 bytes (these are alpha indexes)
                    newData.putInt(data.getInt());
                    newData.putShort(data.getShort());
                }
                int col0 = RGB565.RGB565_to_ARGB8(data.getShort());
                int col1 = RGB565.RGB565_to_ARGB8(data.getShort());
                colors[0].fromARGB8(col0);
                colors[1].fromARGB8(col1);

                // compressing 16 pixels from the base texture as if they belonged to a texel
                if (baseImage != null) {
                    // reading pixels (first and last of the 16 colors array)
                    basePixelIO.read(baseImage, dataLayerIndex, baseTextureColors[0], baseXTexelIndex << 2, baseYTexelIndex << 2);// first pixel
                    basePixelIO.read(baseImage, dataLayerIndex, baseTextureColors[1], baseXTexelIndex << 2 + 4, baseYTexelIndex << 2 + 4);// last pixel
                    baseTextureColors[0].toRGBA(compressedMaterialColor[0]);
                    baseTextureColors[1].toRGBA(compressedMaterialColor[1]);
                }

                // blending colors
                for (int i = 0; i < colors.length; ++i) {
                    if (negateTexture) {
                        colors[i].negate();
                    }
                    colors[i].toRGBA(pixelColor);
                    pixelColor[3] = alphas[i];
                    this.blendPixel(resultPixel, compressedMaterialColor != null ? compressedMaterialColor[i] : materialColor, pixelColor, blenderContext);
                    colors[i].fromARGB(1, resultPixel[0], resultPixel[1], resultPixel[2]);
                    int argb8 = colors[i].toARGB8();
                    short rgb565 = RGB565.ARGB8_to_RGB565(argb8);
                    newData.putShort(rgb565);
                }

                // just copy the remaining 4 bytes of the current texel
                newData.putInt(data.getInt());

                ++baseXTexelIndex;
                if (baseXTexelIndex > image.getWidth() >> 2) {
                    baseXTexelIndex = 0;
                    ++baseYTexelIndex;
                }
            }
            dataArray.add(newData);
        }

        Image result = dataArray.size() > 1 ? new Image(format, width, height, depth, dataArray) : new Image(format, width, height, dataArray.get(0));
        if (image.getMipMapSizes() != null) {
            result.setMipMapSizes(image.getMipMapSizes().clone());
        }
        return result;
    }
}
