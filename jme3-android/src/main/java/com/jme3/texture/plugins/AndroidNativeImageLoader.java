package com.jme3.texture.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoadException;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.TextureKey;
import com.jme3.texture.Image;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Native image loader to deal with filetypes that support alpha channels.
 * The Android Bitmap class premultiplies the channels by the alpha when
 * loading.  This loader does not.
 *
 * @author iwgeric
 */
public class AndroidNativeImageLoader  implements AssetLoader {
    private static final Logger logger = Logger.getLogger(AndroidNativeImageLoader.class.getName());

    public Image load(InputStream in, boolean flipY) throws IOException{
        int result;
        byte[] bytes = getBytes(in);
        int origSize = bytes.length;
//        logger.log(Level.INFO, "png file length: {0}", size);

        ByteBuffer origDataBuffer = BufferUtils.createByteBuffer(origSize);
        origDataBuffer.clear();
        origDataBuffer.put(bytes, 0, origSize);
        origDataBuffer.flip();

        int headerSize = 12;
        ByteBuffer headerDataBuffer = BufferUtils.createByteBuffer(headerSize);
        headerDataBuffer.asIntBuffer();
        headerDataBuffer.clear();

        result = getImageInfo(origDataBuffer, origSize, headerDataBuffer, headerSize);
        if (result != 0) {
            logger.log(Level.SEVERE, "Image header could not be read: {0}", getFailureReason());
            return null;
        }
        headerDataBuffer.rewind();

//        logger.log(Level.INFO, "image header size: {0}", headerDataBuffer.capacity());
//        int position = 0;
//        while (headerDataBuffer.position() < headerDataBuffer.capacity()) {
//            int value = headerDataBuffer.getInt();
//            logger.log(Level.INFO, "position: {0}, value: {1}",
//                    new Object[]{position, value});
//            position++;
//        }
//        headerDataBuffer.rewind();


        int width = headerDataBuffer.getInt();
        int height = headerDataBuffer.getInt();
        int numComponents = headerDataBuffer.getInt();
        int imageDataSize = width * height * numComponents;
//        logger.log(Level.INFO, "width: {0}, height: {1}, numComponents: {2}, imageDataSize: {3}",
//                new Object[]{width, height, numComponents, imageDataSize});

        ByteBuffer imageDataBuffer = BufferUtils.createByteBuffer(imageDataSize);
        imageDataBuffer.clear();

        result = decodeBuffer(origDataBuffer, origSize, flipY, imageDataBuffer, imageDataSize);
        if (result != 0) {
            logger.log(Level.SEVERE, "Image could not be decoded: {0}", getFailureReason());
            return null;
        }
        imageDataBuffer.rewind();

//        logger.log(Level.INFO, "png outSize: {0}", imageDataBuffer.capacity());
//        int pixelNum = 0;
//        while (imageDataBuffer.position() < imageDataBuffer.capacity()) {
//            short r = (short) (imageDataBuffer.get() & 0xFF);
//            short g = (short) (imageDataBuffer.get() & 0xFF);
//            short b = (short) (imageDataBuffer.get() & 0xFF);
//            short a = (short) (imageDataBuffer.get() & 0xFF);
//            logger.log(Level.INFO, "pixel: {0}, r: {1}, g: {2}, b: {3}, a: {4}",
//                    new Object[]{pixelNum, r, g, b, a});
//            pixelNum++;
//        }
//        imageDataBuffer.rewind();

        BufferUtils.destroyDirectBuffer(origDataBuffer);
        BufferUtils.destroyDirectBuffer(headerDataBuffer);

        Image img = new Image(getImageFormat(numComponents), width, height, imageDataBuffer, ColorSpace.sRGB);

        return img;
    }

    public Image load(AssetInfo info) throws IOException {
//        logger.log(Level.INFO, "Loading texture: {0}", ((TextureKey)info.getKey()).toString());
        boolean flip = ((TextureKey) info.getKey()).isFlipY();
        InputStream in = null;
        try {
            in = info.openStream();
            Image img = load(in, flip);
            if (img == null){
                throw new AssetLoadException("The given image cannot be loaded " + info.getKey());
            }
            return img;
        } finally {
            if (in != null){
                in.close();
            }
        }
    }

    private static Image.Format getImageFormat(int stbiNumComponents) {
//     stb_image always returns 8 bit components
//     N=#comp     components
//       1           grey
//       2           grey, alpha
//       3           red, green, blue
//       4           red, green, blue, alpha
        Image.Format format = null;

        if (stbiNumComponents == 1) {
            format = Image.Format.Luminance8;
        } else if (stbiNumComponents == 2) {
            format = Image.Format.Luminance8Alpha8;
        } else if (stbiNumComponents == 3) {
            format = Image.Format.RGB8;
        } else if (stbiNumComponents == 4) {
            format = Image.Format.RGBA8;
        } else {
            throw new IllegalArgumentException("Format returned by stbi is not valid.  Returned value: " + stbiNumComponents);
        }

        return format;
    }

    public static byte[] getBytes(InputStream input) throws IOException {
        byte[] buffer = new byte[32768];
        int bytesRead;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        while ((bytesRead = input.read(buffer)) != -1)
        {
            os.write(buffer, 0, bytesRead);
        }

        byte[] output = os.toByteArray();
        return output;
    }



    /** Load jni .so on initialization */
    static {
         System.loadLibrary("stbijme");
    }

    private static native int getImageInfo(ByteBuffer inBuffer, int inSize, ByteBuffer outBuffer, int outSize);
    private static native int decodeBuffer(ByteBuffer inBuffer, int inSize, boolean flipY, ByteBuffer outBuffer, int outSize);
    private static native String getFailureReason();
}
