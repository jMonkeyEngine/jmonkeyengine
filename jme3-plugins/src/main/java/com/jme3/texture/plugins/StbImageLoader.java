package com.jme3.texture.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.ngengine.stbimage.StbDecoder;
import org.ngengine.stbimage.StbImage;
import org.ngengine.stbimage.StbImageInfo;
import org.ngengine.stbimage.StbImageResult;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.TextureKey;
import com.jme3.export.binary.ByteUtils;
import com.jme3.math.FastMath;
import com.jme3.texture.Image;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;

public class StbImageLoader implements AssetLoader {
    private final StbImage stbImage = new StbImage(BufferUtils::createByteBuffer);

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        AssetKey<?> key = assetInfo.getKey();
        TextureKey textureKey = null;
        if(key instanceof TextureKey){
            textureKey = (TextureKey) key;
        }

        boolean flip = textureKey != null && textureKey.isFlipY();

        try(InputStream is = assetInfo.openStream()) {
            byte[] data = ByteUtils.getByteContent(is);
            ByteBuffer buffer = ByteBuffer.wrap(data);
            stbImage.setConvertIphonePngToRgb(true);
            stbImage.setUnpremultiplyOnLoad(true);

            StbDecoder decoder = stbImage.getDecoder(buffer, flip);
            StbImageInfo info = decoder.info();
            int channels = info.getChannels();

            int width = info.getWidth();
            int height = info.getHeight();
            int desiredChannels = channels;

            boolean is16bit = info.is16Bit();
            boolean isFloat = info.isFloat();
            boolean sRGB = false;


            Image.Format jmeFormat;
            if (is16bit || isFloat) {
                switch (channels) {
                    case 1:
                        jmeFormat = Image.Format.R16F;
                        desiredChannels = 1;
                        break;
                    case 2:
                        jmeFormat = Image.Format.RG16F;
                        desiredChannels = 2;
                        break;
                    case 3:
                        jmeFormat = Image.Format.RGB16F;
                        desiredChannels = 3;
                        break;
                    case 4:
                        jmeFormat = Image.Format.RGBA16F;
                        desiredChannels = 4;
                        break;
                    default:
                        throw new IOException("Unsupported number of channels: " + channels);

                }
            } else {
                switch (channels) {
                    case 1:
                        jmeFormat = Image.Format.Luminance8;
                        desiredChannels = 1;
                        break;
                    case 2:
                        jmeFormat = Image.Format.Luminance8Alpha8;
                        desiredChannels = 2;
                        break;
                    case 3:
                        jmeFormat = Image.Format.RGB8;
                        desiredChannels = 3;
                        sRGB = true;
                        break;
                    case 4:
                        jmeFormat = Image.Format.RGBA8;
                        desiredChannels = 4;
                        sRGB = true;
                        break;
                    default:
                        throw new IOException("Unsupported number of channels: " + channels);
                }
            }

            StbImageResult imgData;
            if(isFloat){
                imgData = decoder.loadf(desiredChannels);
            } else if(is16bit){
                imgData = decoder.load16(desiredChannels);
            } else {
                imgData = decoder.load(desiredChannels);
            }

            ByteBuffer jmeImageBuffer = convertImageData(imgData, jmeFormat);

            Image jmeImage = new Image(jmeFormat, width, height, jmeImageBuffer, sRGB ? ColorSpace.sRGB : ColorSpace.Linear);
            return jmeImage;
        }
    }

    private ByteBuffer convertImageData(StbImageResult imgData, Image.Format jmeFormat) {
        int outputSize = jmeFormat.getBitsPerPixel() / 8 * imgData.getWidth() * imgData.getHeight();
        ByteBuffer jmeImageBuffer = BufferUtils.createByteBuffer(outputSize);
        ByteBuffer source = imgData.getData().duplicate();
        source.order(imgData.getData().order());
        source.position(0).limit(imgData.getDataSize());

        if (!imgData.is16Bit() && !imgData.isFloat()) {
            jmeImageBuffer.put(source);
            jmeImageBuffer.flip();
            return jmeImageBuffer;
        }

        int sampleCount = imgData.getWidth() * imgData.getHeight() * imgData.getRequestedChannels();
        if (imgData.is16Bit()) {
            for (int i = 0; i < sampleCount; i++) {
                float value = (source.getShort() & 0xFFFF) / 65535f;
                jmeImageBuffer.putShort(FastMath.convertFloatToHalf(value));
            }
        } else {
            for (int i = 0; i < sampleCount; i++) {
                jmeImageBuffer.putShort(FastMath.convertFloatToHalf(source.getFloat()));
            }
        }

        jmeImageBuffer.flip();
        return jmeImageBuffer;
    }
}
