package com.jme3.renderer.android;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import com.jme3.math.FastMath;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import java.nio.ByteBuffer;
import javax.microedition.khronos.opengles.GL10;

public class TextureUtil {

    public static int convertTextureFormat(Format fmt){
        switch (fmt){
            case Alpha16:
            case Alpha8:
                return GL10.GL_ALPHA;
            case Luminance8Alpha8:
            case Luminance16Alpha16:
                return GL10.GL_LUMINANCE_ALPHA;
            case Luminance8:
            case Luminance16:
                return GL10.GL_LUMINANCE;
            case RGB10:
            case RGB16:
            case BGR8:
            case RGB8:
            case RGB565:
                return GL10.GL_RGB;
            case RGB5A1:
            case RGBA16:
            case RGBA8:
                return GL10.GL_RGBA;
                
            case Depth:
                return GLES20.GL_DEPTH_COMPONENT;
            case Depth16:
                return GLES20.GL_DEPTH_COMPONENT16;
            case Depth24:
            case Depth32:
            case Depth32F:
                throw new UnsupportedOperationException("Unsupported depth format: " + fmt);   
                
            case DXT1A:
                throw new UnsupportedOperationException("Unsupported format: " + fmt);
            default:
                throw new UnsupportedOperationException("Unrecognized format: " + fmt);
        }
    }

    private static void buildMipmap(Bitmap bitmap) {
        int level = 0;
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();

        while (height >= 1 || width >= 1) {
            //First of all, generate the texture from our bitmap and set it to the according level
            GLUtils.texImage2D(GL10.GL_TEXTURE_2D, level, bitmap, 0);

            if (height == 1 || width == 1) {
                break;
            }

            //Increase the mipmap level
            level++;

            height /= 2;
            width /= 2;
            Bitmap bitmap2 = Bitmap.createScaledBitmap(bitmap, width, height, true);

            bitmap.recycle();
            bitmap = bitmap2;
        }
    }

    /**
     * <code>uploadTextureBitmap</code> uploads a native android bitmap
     * @param target
     * @param bitmap
     * @param generateMips
     * @param powerOf2
     */
    public static void uploadTextureBitmap(final int target, Bitmap bitmap, boolean generateMips, boolean powerOf2)
    {
        if (!powerOf2)
        {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (!FastMath.isPowerOfTwo(width) || !FastMath.isPowerOfTwo(height))
            {
                // scale to power of two
                width = FastMath.nearestPowerOfTwo(width);
                height = FastMath.nearestPowerOfTwo(height);
                Bitmap bitmap2 = Bitmap.createScaledBitmap(bitmap, width, height, true);
                bitmap.recycle();
                bitmap = bitmap2;
            }
        }

        if (generateMips)
        {
            buildMipmap(bitmap);
        }
        else
        {
            GLUtils.texImage2D(target, 0, bitmap, 0);
            //bitmap.recycle();
        }
    }

    public static void uploadTexture(
                                     Image img,
                                     int target,
                                     int index,
                                     int border,
                                     boolean tdc,
                                     boolean generateMips,
                                     boolean powerOf2){

        if (img.getEfficentData() instanceof Bitmap){
            Bitmap bitmap = (Bitmap) img.getEfficentData();
            uploadTextureBitmap(target, bitmap, generateMips, powerOf2);
            return;
        }

        Image.Format fmt = img.getFormat();
        ByteBuffer data;
        if (index >= 0 || img.getData() != null && img.getData().size() > 0){
            data = img.getData(index);
        }else{
            data = null;
        }

        int width = img.getWidth();
        int height = img.getHeight();
        int depth = img.getDepth();

        boolean compress = false;
        int internalFormat = -1;
        int format = -1;
        int dataType = -1;

        switch (fmt){
            case Alpha16:
            case Alpha8:
                format = GLES20.GL_ALPHA;
                dataType = GLES20.GL_UNSIGNED_BYTE;                
                break;
            case Luminance8:
                format = GLES20.GL_LUMINANCE;
                dataType = GLES20.GL_UNSIGNED_BYTE;
                break;
            case Luminance8Alpha8:
                format = GLES20.GL_LUMINANCE_ALPHA;
                dataType = GLES20.GL_UNSIGNED_BYTE;
                break;
            case Luminance16Alpha16:
                format = GLES20.GL_LUMINANCE_ALPHA;
                dataType = GLES20.GL_UNSIGNED_BYTE;
                break;
            case Luminance16:
                format = GLES20.GL_LUMINANCE;
                dataType = GLES20.GL_UNSIGNED_BYTE;
                break;
            case RGB565:
                format = GLES20.GL_RGB;
                internalFormat = GLES20.GL_RGB565;
                dataType = GLES20.GL_UNSIGNED_SHORT_5_6_5;
                break;
            case ARGB4444:
                format = GLES20.GL_RGBA;
                dataType = GLES20.GL_UNSIGNED_SHORT_4_4_4_4;
                break;
            case RGB10:
                format = GLES20.GL_RGB;
                dataType = GLES20.GL_UNSIGNED_BYTE;
                break;
            case RGB16:
                format = GLES20.GL_RGB;
                dataType = GLES20.GL_UNSIGNED_BYTE;
                break;
            case RGB5A1:
                format = GLES20.GL_RGBA;
                internalFormat = GLES20.GL_RGB5_A1;
                dataType = GLES20.GL_UNSIGNED_SHORT_5_5_5_1;
                break;
            case RGB8:
                format = GLES20.GL_RGB;
                dataType = GLES20.GL_UNSIGNED_BYTE;
                break;
            case BGR8:
                format = GLES20.GL_RGB;
                dataType = GLES20.GL_UNSIGNED_BYTE;
                break;
            case RGBA16:
                format = GLES20.GL_RGBA;
                internalFormat = GLES20.GL_RGBA4;
                dataType = GLES20.GL_UNSIGNED_BYTE;
                break;
            case RGBA8:
                format = GLES20.GL_RGBA;                
                dataType = GLES20.GL_UNSIGNED_BYTE;
                break;
            case DXT1A:
                format = GLES20.GL_COMPRESSED_TEXTURE_FORMATS;
                dataType = GLES20.GL_UNSIGNED_BYTE;
            case Depth:
                format = GLES20.GL_DEPTH_COMPONENT;
                dataType = GLES20.GL_UNSIGNED_BYTE;
                break;
            case Depth16:
                format = GLES20.GL_DEPTH_COMPONENT;
                internalFormat = GLES20.GL_DEPTH_COMPONENT16;
                dataType = GLES20.GL_UNSIGNED_BYTE;
                break;
            case Depth24:
            case Depth32:
            case Depth32F:
                throw new UnsupportedOperationException("Unsupported depth format: " + fmt);                
            default:
                throw new UnsupportedOperationException("Unrecognized format: " + fmt);
        }
        
        if (internalFormat == -1)
        {
            internalFormat = format;
        }

        if (data != null)
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);

        int[] mipSizes = img.getMipMapSizes();
        int pos = 0;
        if (mipSizes == null){
            if (data != null)
                mipSizes = new int[]{ data.capacity() };
            else
                mipSizes = new int[]{ width * height * fmt.getBitsPerPixel() / 8 };
        }

        // XXX: might want to change that when support
        // of more than paletted compressions is added..
        if (compress){
            data.clear();
            GLES20.glCompressedTexImage2D(GLES20.GL_TEXTURE_2D,
                                      1 - mipSizes.length,
                                      format,
                                      width,
                                      height,
                                      0,
                                      data.capacity(),
                                      data);
            return;
        }

        for (int i = 0; i < mipSizes.length; i++){
            int mipWidth =  Math.max(1, width  >> i);
            int mipHeight = Math.max(1, height >> i);
            int mipDepth =  Math.max(1, depth  >> i);

            if (data != null){
                data.position(pos);
                data.limit(pos + mipSizes[i]);
            }

            if (compress && data != null){
                GLES20.glCompressedTexImage2D(GLES20.GL_TEXTURE_2D,
                                          i,
                                          format,
                                          mipWidth,
                                          mipHeight,
                                          0,
                                          data.remaining(),
                                          data);
            }else{
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,
                                i,
                                internalFormat,
                                mipWidth,
                                mipHeight,
                                0,
                                format,
                                dataType,
                                data);
            }

            pos += mipSizes[i];
        }
    }

}
