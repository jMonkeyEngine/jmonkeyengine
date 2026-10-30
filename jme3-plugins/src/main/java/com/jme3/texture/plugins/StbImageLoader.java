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
import com.jme3.util.ByteBufferUtils;
import com.jme3.util.BufferUtils;

public class StbImageLoader implements AssetLoader {
    private final StbImage stbImage = new StbImage(BufferUtils::createByteBuffer);

    /**
     * Loads an image from an {@link InputStream} with optional vertical flip.
     *
     * @param in     the input stream containing the encoded image data (e.g. PNG, JPEG)
     * @param flipY  {@code true} to flip the image vertically on load
     * @return the decoded {@link Image}
     * @throws IOException if the stream cannot be read or the image format is unsupported
     */
    public Image load(InputStream in, boolean flipY) throws IOException {
        byte[] data = ByteUtils.getByteContent(in);
        return load(data, flipY);
    }

    /**
     * Loads an image from a byte array with optional vertical flip.
     *
     * @param data   the raw encoded image data (e.g. PNG, JPEG)
     * @param flipY  {@code true} to flip the image vertically on load
     * @return the decoded {@link Image}
     * @throws IOException if the image data cannot be decoded or the format is unsupported
     */
    public Image load(byte[] data, boolean flipY) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        stbImage.setConvertIphonePngToRgb(true);
        stbImage.setUnpremultiplyOnLoad(true);

        StbDecoder decoder = stbImage.getDecoder(buffer, flipY);
        StbImageInfo info = decoder.info();
        int channels = info.getChannels();

        int width = info.getWidth();
        int height = info.getHeight();
        int desiredChannels = channels;

        boolean is16bit = info.is16Bit();
        boolean isFloat = info.isFloat();

        Image.Format jmeFormat = selectFormat(channels, is16bit, isFloat);

        StbImageResult imgData;
        if (isFloat){
            imgData = decoder.loadf(desiredChannels);
        } else if (is16bit){
            imgData = decoder.load16(desiredChannels);
        } else {
            imgData = decoder.load(desiredChannels);
        }

        boolean sRGB = (jmeFormat == Image.Format.RGB8 || jmeFormat == Image.Format.RGBA8);
        ByteBuffer jmeImageBuffer = convertImageData(imgData, jmeFormat);

        return new Image(jmeFormat, width, height, jmeImageBuffer, sRGB ? ColorSpace.sRGB : ColorSpace.Linear);
    }

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        AssetKey<?> key = assetInfo.getKey();
        TextureKey textureKey = null;
        if(key instanceof TextureKey){
            textureKey = (TextureKey) key;
        }

        boolean flip = textureKey != null && textureKey.isFlipY();

        try(InputStream is = assetInfo.openStream()) {
            return load(is, flip);
        }
    }

    /**
     * Maps a channel count and bit-depth to the corresponding {@link Image.Format}.
     *
     * @param channels  number of channels (1–4)
     * @param is16bit   {@code true} for 16-bit-per-channel source data
     * @param isFloat   {@code true} for floating-point source data
     * @return the matching {@link Image.Format}
     * @throws IOException if {@code channels} is outside the supported range
     */
    private static Image.Format selectFormat(int channels, boolean is16bit, boolean isFloat)
            throws IOException {
        if (is16bit || isFloat) {
            switch (channels) {
                case 1: return Image.Format.Luminance16F;
                case 2: return Image.Format.Luminance16FAlpha16F;
                case 3: return Image.Format.RGB16F;
                case 4: return Image.Format.RGBA16F;
                default: throw new IOException("Unsupported number of channels: " + channels);
            }
        } else {
            switch (channels) {
                case 1: return Image.Format.Luminance8;
                case 2: return Image.Format.Luminance8Alpha8;
                case 3: return Image.Format.RGB8;
                case 4: return Image.Format.RGBA8;
                default: throw new IOException("Unsupported number of channels: " + channels);
            }
        }
    }

    private ByteBuffer convertImageData(StbImageResult imgData, Image.Format jmeFormat) {
        int outputSize = jmeFormat.getBitsPerPixel() / 8 * imgData.getWidth() * imgData.getHeight();
        ByteBuffer jmeImageBuffer = BufferUtils.createByteBuffer(outputSize);
        ByteBuffer source = ByteBufferUtils.duplicate(imgData.getData());
        source.position(0).limit(imgData.getDataSize());

        if (!imgData.is16Bit() && !imgData.isFloat()) {
            jmeImageBuffer.put(source);
            jmeImageBuffer.flip();
            return jmeImageBuffer;
        }

        int sampleCount = imgData.getWidth() * imgData.getHeight() * imgData.getChannels();
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
