package com.jme3.renderer.android;

import android.graphics.Bitmap;
import android.opengl.ETC1;
import android.opengl.ETC1Util.ETC1Texture;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import com.jme3.asset.AndroidImageInfo;
import com.jme3.math.FastMath;
import com.jme3.renderer.RendererException;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TextureUtil {

    private static final Logger logger = Logger.getLogger(TextureUtil.class.getName());
    
    //TODO Make this configurable through appSettings
    public static boolean ENABLE_COMPRESSION = true;
    private static boolean NPOT = false;
    private static boolean ETC1support = false;
    private static boolean DXT1 = false;
    private static boolean DEPTH24 = false;
    
    public static void loadTextureFeatures(String extensionString) {
        ETC1support = extensionString.contains("GL_OES_compressed_ETC1_RGB8_texture");
        DEPTH24 = extensionString.contains("GL_OES_depth24");
        NPOT = extensionString.contains("GL_OES_texture_npot") || extensionString.contains("GL_NV_texture_npot_2D_mipmap");
        DXT1 = extensionString.contains("GL_EXT_texture_compression_dxt1");
        logger.log(Level.FINE, "Supports ETC1? {0}", ETC1support);
        logger.log(Level.FINE, "Supports DEPTH24? {0}", DEPTH24);
        logger.log(Level.FINE, "Supports NPOT? {0}", NPOT);
        logger.log(Level.FINE, "Supports DXT1? {0}", DXT1);
    }
    
    private static void buildMipmap(Bitmap bitmap, boolean compress) {
        int level = 0;
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        
        logger.log(Level.FINEST, " - Generating mipmaps for bitmap using SOFTWARE");

        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
        
        while (height >= 1 || width >= 1) {
            //First of all, generate the texture from our bitmap and set it to the according level
            if (compress) {
                logger.log(Level.FINEST, " - Uploading LOD level {0} ({1}x{2}) with compression.", new Object[]{level, width, height});
                uploadBitmapAsCompressed(GLES20.GL_TEXTURE_2D, level, bitmap);
            } else {
                logger.log(Level.FINEST, " - Uploading LOD level {0} ({1}x{2}) directly.", new Object[]{level, width, height});
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, level, bitmap, 0);
            }

            if (height == 1 || width == 1) {
                break;
            }

            //Increase the mipmap level
            height /= 2;
            width /= 2;
            Bitmap bitmap2 = Bitmap.createScaledBitmap(bitmap, width, height, true);

            // Recycle any bitmaps created as a result of scaling the bitmap.
            // Do not recycle the original image (mipmap level 0)
            if (level != 0){
                bitmap.recycle();
            }
            
            bitmap = bitmap2;
            
            level++;
        }
    }

    private static void uploadBitmapAsCompressed(int target, int level, Bitmap bitmap) {
        if (bitmap.hasAlpha()) {
            logger.log(Level.FINEST, " - Uploading bitmap directly. Cannot compress as alpha present.");
            GLUtils.texImage2D(target, level, bitmap, 0);
        } else {
            // Convert to RGB565
            int bytesPerPixel = 2;
            Bitmap rgb565 = bitmap.copy(Bitmap.Config.RGB_565, true);

            // Put texture data into ByteBuffer
            ByteBuffer inputImage = BufferUtils.createByteBuffer(bitmap.getRowBytes() * bitmap.getHeight());
            rgb565.copyPixelsToBuffer(inputImage);
            inputImage.position(0);

            // Delete the copied RGB565 image
            rgb565.recycle();

            // Encode the image into the output bytebuffer
            int encodedImageSize = ETC1.getEncodedDataSize(bitmap.getWidth(), bitmap.getHeight());
            ByteBuffer compressedImage = BufferUtils.createByteBuffer(encodedImageSize);
            ETC1.encodeImage(inputImage, bitmap.getWidth(), 
                                 bitmap.getHeight(), 
                             bytesPerPixel, 
                             bytesPerPixel * bitmap.getWidth(), 
                             compressedImage);
            
            // Delete the input image buffer
            BufferUtils.destroyDirectBuffer(inputImage);
            
            // Create an ETC1Texture from the compressed image data
            ETC1Texture etc1tex = new ETC1Texture(bitmap.getWidth(), bitmap.getHeight(), compressedImage);

            // Upload the ETC1Texture
            if (bytesPerPixel == 2) {
                int oldSize = (bitmap.getRowBytes() * bitmap.getHeight());
                int newSize = compressedImage.capacity();
                logger.log(Level.FINEST, " - Uploading compressed image to GL, oldSize = {0}, newSize = {1}, ratio = {2}", new Object[]{oldSize, newSize, (float)oldSize/newSize});
                GLES20.glCompressedTexImage2D(target, 
                                              level, 
                                              ETC1.ETC1_RGB8_OES, 
                                              bitmap.getWidth(), 
                                              bitmap.getHeight(), 
                                              0, 
                                              etc1tex.getData().capacity(), 
                                              etc1tex.getData());
                
//                ETC1Util.loadTexture(target, level, 0, GLES20.GL_RGB,
//                        GLES20.GL_UNSIGNED_SHORT_5_6_5, etc1Texture);
//            } else if (bytesPerPixel == 3) {
//                ETC1Util.loadTexture(target, level, 0, GLES20.GL_RGB,
//                        GLES20.GL_UNSIGNED_BYTE, etc1Texture);
            }
            
            BufferUtils.destroyDirectBuffer(compressedImage);
        }
    }
    
    /**
     * <code>uploadTextureBitmap</code> uploads a native android bitmap
     */
    public static void uploadTextureBitmap(final int target, Bitmap bitmap, boolean needMips) {
        boolean recycleBitmap = false;
        if (!NPOT || needMips) {
            // Power of 2 images are not supported by this GPU.
            // OR
            // Mipmaps were requested to be used. 
            // Currently OGLES does not support NPOT textures with mipmaps.
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            
            // If the image is not power of 2, rescale it
            if (!FastMath.isPowerOfTwo(width) || !FastMath.isPowerOfTwo(height)) {
                // Scale to power of two.
                width  = FastMath.nearestPowerOfTwo(width);
                height = FastMath.nearestPowerOfTwo(height);
                
                logger.log(Level.WARNING, " - Image is not POT, so scaling it to new resolution: {0}x{1}", new Object[]{width, height});
                Bitmap bitmap2 = Bitmap.createScaledBitmap(bitmap, width, height, true);
                bitmap = bitmap2;
                
                // Flag to indicate that bitmap
                // should be recycled at the end.
                recycleBitmap = true; 
            }
        }

        boolean willCompress = ENABLE_COMPRESSION && ETC1support && !bitmap.hasAlpha();
        if (needMips && willCompress) {
            // Image is compressed and mipmaps are desired, generate them
            // using software.
            buildMipmap(bitmap, willCompress);
        } else {
            if (willCompress) {
                // Image is compressed but mipmaps are not desired, upload directly.
                logger.log(Level.FINEST, " - Uploading compressed bitmap. Mipmaps are not generated.");
                uploadBitmapAsCompressed(target, 0, bitmap);
            } else {
                // Image is not compressed, mipmaps may or may not be desired.
                logger.log(Level.FINEST, " - Uploading bitmap directly.{0}", 
                        (needMips ? 
                            " Mipmaps will be generated in HARDWARE" : 
                            " Mipmaps are not generated."));
                GLUtils.texImage2D(target, 0, bitmap, 0);
                if (needMips) {
                    // No pregenerated mips available,
                    // generate from base level if required
                    GLES20.glGenerateMipmap(target);
                }
            }
        }
        
        if (recycleBitmap) {
            bitmap.recycle();
        }
    }
    
    public static void uploadTextureAny(Image img, int target, int index, boolean needMips) {
        if (img.getEfficentData() instanceof AndroidImageInfo){
            logger.log(Level.FINEST, " === Uploading image {0}. Using BITMAP PATH === ", img);
            // If image was loaded from asset manager, use fast path
            AndroidImageInfo imageInfo = (AndroidImageInfo) img.getEfficentData();
            uploadTextureBitmap(target, imageInfo.getBitmap(), needMips);
        } else {
            logger.log(Level.FINEST, " === Uploading image {0}. Using BUFFER PATH === ", img);
            boolean wantGeneratedMips = needMips && !img.hasMipmaps();
            if (wantGeneratedMips && img.getFormat().isCompressed()) {
                logger.log(Level.WARNING, "Generating mipmaps is only"
                        + " supported for Bitmap based or non-compressed images!");
            }
            
            // Upload using slower path
            logger.log(Level.FINEST, " - Uploading bitmap directly.{0}", 
                        (wantGeneratedMips ? 
                            " Mipmaps will be generated in HARDWARE" : 
                            " Mipmaps are not generated."));
            uploadTexture(img, target, index);
            
            // Image was uploaded using slower path, since it is not compressed,
            // then compress it
            if (wantGeneratedMips) {
                // No pregenerated mips available,
                // generate from base level if required
                GLES20.glGenerateMipmap(target);
            }
        }
    }
    
    private static void unsupportedFormat(Format fmt) {
        throw new UnsupportedOperationException("The image format '" + fmt + "' is unsupported by the video hardware.");
    }

    private static void uploadTexture(Image img,
                                     int target,
                                     int index){

        if (img.getEfficentData() instanceof AndroidImageInfo){
            throw new RendererException("This image uses efficient data. "
                    + "Use uploadTextureBitmap instead.");
        }

        // Otherwise upload image directly. 
        // Prefer to only use power of 2 textures here to avoid errors.
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
        
        if (!NPOT) {
            // Check if texture is POT
            if (!FastMath.isPowerOfTwo(width) || !FastMath.isPowerOfTwo(height)) {
                throw new RendererException("Non-power-of-2 textures "
                        + "are not supported by the video hardware "
                        + "and no scaling path available for image: " + img);
            }
        }
        
        boolean compress = false;
        int format = -1;
        int dataType = -1;

        switch (fmt){
            case RGBA16:
            case RGB16:
            case RGB10:
            case Luminance16:
            case Luminance16Alpha16:
            case Alpha16:
            case Depth32:
            case Depth32F:
                throw new UnsupportedOperationException("The image format '" 
                        + fmt + "' is not supported by OpenGL ES 2.0 specification.");
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
            case RGB565:
                format = GLES20.GL_RGB;
                dataType = GLES20.GL_UNSIGNED_SHORT_5_6_5;
                break;
            case ARGB4444:
                format = GLES20.GL_RGBA4;
                dataType = GLES20.GL_UNSIGNED_SHORT_4_4_4_4;
                break;
            case RGB5A1:
                format = GLES20.GL_RGBA;
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
            case RGBA8:
                format = GLES20.GL_RGBA;                
                dataType = GLES20.GL_UNSIGNED_BYTE;
                break;
            case Depth:
            case Depth16:
            case Depth24:
                format = GLES20.GL_DEPTH_COMPONENT;
                dataType = GLES20.GL_UNSIGNED_BYTE;
                break;
            case DXT1:
                if (!DXT1) {
                    unsupportedFormat(fmt);
                }
                format = 0x83F0;
                dataType = GLES20.GL_UNSIGNED_BYTE;
                compress = true;
                break;
            case DXT1A:
                if (!DXT1) {
                    unsupportedFormat(fmt);
                }
                format = 0x83F1;
                dataType = GLES20.GL_UNSIGNED_BYTE;
                compress = true;
                break;
            default:
                throw new UnsupportedOperationException("Unrecognized format: " + fmt);
        }

        if (data != null) {
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
        }

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
        /// NOTE: Doesn't support mipmaps
//        if (compress){
//            data.clear();
//            GLES20.glCompressedTexImage2D(target,
//                                      1 - mipSizes.length,
//                                      format,
//                                      width,
//                                      height,
//                                      0,
//                                      data.capacity(),
//                                      data);
//            return;
//        }

        for (int i = 0; i < mipSizes.length; i++){
            int mipWidth =  Math.max(1, width  >> i);
            int mipHeight = Math.max(1, height >> i);
//            int mipDepth =  Math.max(1, depth  >> i);

            if (data != null){
                data.position(pos);
                data.limit(pos + mipSizes[i]);
            }

            if (compress && data != null){
                GLES20.glCompressedTexImage2D(target,
                                          i,
                                          format,
                                          mipWidth,
                                          mipHeight,
                                          0,
                                          data.remaining(),
                                          data);
            }else{
                GLES20.glTexImage2D(target,
                                i,
                                format,
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
