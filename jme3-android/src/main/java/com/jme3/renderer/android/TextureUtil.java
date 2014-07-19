package com.jme3.renderer.android;

import android.graphics.Bitmap;
import android.opengl.ETC1;
import android.opengl.ETC1Util.ETC1Texture;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import com.jme3.asset.AndroidImageInfo;
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
    private static boolean DEPTH24_STENCIL8 = false;
    private static boolean DEPTH_TEXTURE = false;
    private static boolean RGBA8 = false;
    
    // Same constant used by both GL_ARM_rgba8 and GL_OES_rgb8_rgba8.
    private static final int GL_RGBA8 = 0x8058;
    
    private static final int GL_DXT1 = 0x83F0;
    private static final int GL_DXT1A = 0x83F1;
    
    private static final int GL_DEPTH_STENCIL_OES = 0x84F9;
    private static final int GL_UNSIGNED_INT_24_8_OES = 0x84FA;
    private static final int GL_DEPTH24_STENCIL8_OES = 0x88F0;

    public static void loadTextureFeatures(String extensionString) {
        ETC1support = extensionString.contains("GL_OES_compressed_ETC1_RGB8_texture");
        DEPTH24_STENCIL8 = extensionString.contains("GL_OES_packed_depth_stencil");
        NPOT = extensionString.contains("GL_IMG_texture_npot") 
                || extensionString.contains("GL_OES_texture_npot") 
                || extensionString.contains("GL_NV_texture_npot_2D_mipmap");
        
        DXT1 = extensionString.contains("GL_EXT_texture_compression_dxt1");
        DEPTH_TEXTURE = extensionString.contains("GL_OES_depth_texture");
        
        RGBA8 = extensionString.contains("GL_ARM_rgba8") ||
                extensionString.contains("GL_OES_rgb8_rgba8");
        
        logger.log(Level.FINE, "Supports ETC1? {0}", ETC1support);
        logger.log(Level.FINE, "Supports DEPTH24_STENCIL8? {0}", DEPTH24_STENCIL8);
        logger.log(Level.FINE, "Supports NPOT? {0}", NPOT);
        logger.log(Level.FINE, "Supports DXT1? {0}", DXT1);
        logger.log(Level.FINE, "Supports DEPTH_TEXTURE? {0}", DEPTH_TEXTURE);
        logger.log(Level.FINE, "Supports RGBA8? {0}", RGBA8);
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
                uploadBitmapAsCompressed(GLES20.GL_TEXTURE_2D, level, bitmap, false, 0, 0);
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
            if (level != 0) {
                bitmap.recycle();
            }

            bitmap = bitmap2;

            level++;
        }
    }

    private static void uploadBitmapAsCompressed(int target, int level, Bitmap bitmap, boolean subTexture, int x, int y) {
        if (bitmap.hasAlpha()) {
            logger.log(Level.FINEST, " - Uploading bitmap directly. Cannot compress as alpha present.");
            if (subTexture) {
                GLUtils.texSubImage2D(target, level, x, y, bitmap);
                RendererUtil.checkGLError();
            } else {
                GLUtils.texImage2D(target, level, bitmap, 0);
                RendererUtil.checkGLError();
            }
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
                logger.log(Level.FINEST, " - Uploading compressed image to GL, oldSize = {0}, newSize = {1}, ratio = {2}", new Object[]{oldSize, newSize, (float) oldSize / newSize});
                if (subTexture) {
                    GLES20.glCompressedTexSubImage2D(target,
                            level,
                            x, y,
                            bitmap.getWidth(),
                            bitmap.getHeight(),
                            ETC1.ETC1_RGB8_OES,
                            etc1tex.getData().capacity(),
                            etc1tex.getData());
                    
                    RendererUtil.checkGLError();
                } else {
                    GLES20.glCompressedTexImage2D(target,
                            level,
                            ETC1.ETC1_RGB8_OES,
                            bitmap.getWidth(),
                            bitmap.getHeight(),
                            0,
                            etc1tex.getData().capacity(),
                            etc1tex.getData());
                    
                    RendererUtil.checkGLError();
                }

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
        uploadTextureBitmap(target, bitmap, needMips, false, 0, 0);
    }

    /**
     * <code>uploadTextureBitmap</code> uploads a native android bitmap
     */
    public static void uploadTextureBitmap(final int target, Bitmap bitmap, boolean needMips, boolean subTexture, int x, int y) {
        boolean recycleBitmap = false;
        //TODO, maybe this should raise an exception when NPOT is not supported

        boolean willCompress = ENABLE_COMPRESSION && ETC1support && !bitmap.hasAlpha();
        if (needMips && willCompress) {
            // Image is compressed and mipmaps are desired, generate them
            // using software.
            buildMipmap(bitmap, willCompress);
        } else {
            if (willCompress) {
                // Image is compressed but mipmaps are not desired, upload directly.
                logger.log(Level.FINEST, " - Uploading compressed bitmap. Mipmaps are not generated.");
                uploadBitmapAsCompressed(target, 0, bitmap, subTexture, x, y);

            } else {
                // Image is not compressed, mipmaps may or may not be desired.
                logger.log(Level.FINEST, " - Uploading bitmap directly.{0}",
                        (needMips
                        ? " Mipmaps will be generated in HARDWARE"
                        : " Mipmaps are not generated."));
                if (subTexture) {
                    System.err.println("x : " + x + " y :" + y + " , " + bitmap.getWidth() + "/" + bitmap.getHeight());
                    GLUtils.texSubImage2D(target, 0, x, y, bitmap);
                    RendererUtil.checkGLError();
                } else {
                    GLUtils.texImage2D(target, 0, bitmap, 0);
                    RendererUtil.checkGLError();
                }

                if (needMips) {
                    // No pregenerated mips available,
                    // generate from base level if required
                    GLES20.glGenerateMipmap(target);
                    RendererUtil.checkGLError();
                }
            }
        }

        if (recycleBitmap) {
            bitmap.recycle();
        }
    }

    public static void uploadTextureAny(Image img, int target, int index, boolean needMips) {
        if (img.getEfficentData() instanceof AndroidImageInfo) {
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
                    (wantGeneratedMips
                    ? " Mipmaps will be generated in HARDWARE"
                    : " Mipmaps are not generated."));
            
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

    public static AndroidGLImageFormat getImageFormat(Format fmt) throws UnsupportedOperationException {
        AndroidGLImageFormat imageFormat = new AndroidGLImageFormat();
        switch (fmt) {
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
                imageFormat.format = GLES20.GL_ALPHA;
                imageFormat.dataType = GLES20.GL_UNSIGNED_BYTE;
                if (RGBA8) {
                    imageFormat.renderBufferStorageFormat = GL_RGBA8;
                } else {
                    // Highest precision alpha supported by vanilla OGLES2
                    imageFormat.renderBufferStorageFormat = GLES20.GL_RGBA4;
                }
                break;
            case Luminance8:
                imageFormat.format = GLES20.GL_LUMINANCE;
                imageFormat.dataType = GLES20.GL_UNSIGNED_BYTE;
                if (RGBA8) {
                    imageFormat.renderBufferStorageFormat = GL_RGBA8;
                } else {
                    // Highest precision luminance supported by vanilla OGLES2
                    imageFormat.renderBufferStorageFormat = GLES20.GL_RGB565;
                }
                break;
            case Luminance8Alpha8:
                imageFormat.format = GLES20.GL_LUMINANCE_ALPHA;
                imageFormat.dataType = GLES20.GL_UNSIGNED_BYTE;
                if (RGBA8) {
                    imageFormat.renderBufferStorageFormat = GL_RGBA8;
                } else {
                    imageFormat.renderBufferStorageFormat = GLES20.GL_RGBA4;
                }
                break;
            case RGB565:
                imageFormat.format = GLES20.GL_RGB;
                imageFormat.dataType = GLES20.GL_UNSIGNED_SHORT_5_6_5;
                imageFormat.renderBufferStorageFormat = GLES20.GL_RGB565;
                break;
            case ARGB4444:
                imageFormat.format = GLES20.GL_RGBA4;
                imageFormat.dataType = GLES20.GL_UNSIGNED_SHORT_4_4_4_4;
                imageFormat.renderBufferStorageFormat = GLES20.GL_RGBA4;
                break;
            case RGB5A1:
                imageFormat.format = GLES20.GL_RGBA;
                imageFormat.dataType = GLES20.GL_UNSIGNED_SHORT_5_5_5_1;
                imageFormat.renderBufferStorageFormat = GLES20.GL_RGB5_A1;
                break;
            case RGB8:
                imageFormat.format = GLES20.GL_RGB;
                imageFormat.dataType = GLES20.GL_UNSIGNED_BYTE;
                if (RGBA8) {
                    imageFormat.renderBufferStorageFormat = GL_RGBA8;
                } else {
                    // Fallback: Use RGB565 if RGBA8 is not available.
                    imageFormat.renderBufferStorageFormat = GLES20.GL_RGB565;
                }
                break;
            case BGR8:
                imageFormat.format = GLES20.GL_RGB;
                imageFormat.dataType = GLES20.GL_UNSIGNED_BYTE;
                if (RGBA8) {
                    imageFormat.renderBufferStorageFormat = GL_RGBA8;
                } else {
                    imageFormat.renderBufferStorageFormat = GLES20.GL_RGB565;
                }
                break;
            case RGBA8:
                imageFormat.format = GLES20.GL_RGBA;
                imageFormat.dataType = GLES20.GL_UNSIGNED_BYTE;
                if (RGBA8) {
                    imageFormat.renderBufferStorageFormat = GL_RGBA8;
                } else {
                    imageFormat.renderBufferStorageFormat = GLES20.GL_RGBA4;
                }
                break;
            case Depth:
            case Depth16:
                if (!DEPTH_TEXTURE) {
                    unsupportedFormat(fmt);
                }
                imageFormat.format = GLES20.GL_DEPTH_COMPONENT;
                imageFormat.dataType = GLES20.GL_UNSIGNED_SHORT;
                imageFormat.renderBufferStorageFormat = GLES20.GL_DEPTH_COMPONENT16;
                break;
            case Depth24:
            case Depth24Stencil8:
                if (!DEPTH_TEXTURE) {
                    unsupportedFormat(fmt);
                }
                if (DEPTH24_STENCIL8) {
                    // NEW: True Depth24 + Stencil8 format.
                    imageFormat.format = GL_DEPTH_STENCIL_OES;
                    imageFormat.dataType = GL_UNSIGNED_INT_24_8_OES;
                    imageFormat.renderBufferStorageFormat = GL_DEPTH24_STENCIL8_OES;
                } else {
                    // Vanilla OGLES2, only Depth16 available.
                    imageFormat.format = GLES20.GL_DEPTH_COMPONENT;
                    imageFormat.dataType = GLES20.GL_UNSIGNED_SHORT;
                    imageFormat.renderBufferStorageFormat = GLES20.GL_DEPTH_COMPONENT16;
                }
                break;
            case DXT1:
                if (!DXT1) {
                    unsupportedFormat(fmt);
                }
                imageFormat.format = GL_DXT1;
                imageFormat.dataType = GLES20.GL_UNSIGNED_BYTE;
                imageFormat.compress = true;
                break;
            case DXT1A:
                if (!DXT1) {
                    unsupportedFormat(fmt);
                }
                imageFormat.format = GL_DXT1A;
                imageFormat.dataType = GLES20.GL_UNSIGNED_BYTE;
                imageFormat.compress = true;
                break;
            default:
                throw new UnsupportedOperationException("Unrecognized format: " + fmt);
        }
        return imageFormat;
    }

    public static class AndroidGLImageFormat {

        boolean compress = false;
        int format = -1;
        int renderBufferStorageFormat = -1;
        int dataType = -1;
    }

    private static void uploadTexture(Image img,
            int target,
            int index) {

        if (img.getEfficentData() instanceof AndroidImageInfo) {
            throw new RendererException("This image uses efficient data. "
                    + "Use uploadTextureBitmap instead.");
        }

        // Otherwise upload image directly.
        // Prefer to only use power of 2 textures here to avoid errors.
        Image.Format fmt = img.getFormat();
        ByteBuffer data;
        if (index >= 0 || img.getData() != null && img.getData().size() > 0) {
            data = img.getData(index);
        } else {
            data = null;
        }

        int width = img.getWidth();
        int height = img.getHeight();

        if (!NPOT && img.isNPOT()) {
            // Check if texture is POT
            throw new RendererException("Non-power-of-2 textures "
                    + "are not supported by the video hardware "
                    + "and no scaling path available for image: " + img);
        }
        AndroidGLImageFormat imageFormat = getImageFormat(fmt);

        if (data != null) {
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
        }

        int[] mipSizes = img.getMipMapSizes();
        int pos = 0;
        if (mipSizes == null) {
            if (data != null) {
                mipSizes = new int[]{data.capacity()};
            } else {
                mipSizes = new int[]{width * height * fmt.getBitsPerPixel() / 8};
            }
        }

        for (int i = 0; i < mipSizes.length; i++) {
            int mipWidth = Math.max(1, width >> i);
            int mipHeight = Math.max(1, height >> i);

            if (data != null) {
                data.position(pos);
                data.limit(pos + mipSizes[i]);
            }

            if (imageFormat.compress && data != null) {
                GLES20.glCompressedTexImage2D(target,
                        i,
                        imageFormat.format,
                        mipWidth,
                        mipHeight,
                        0,
                        data.remaining(),
                        data);
            } else {
                GLES20.glTexImage2D(target,
                        i,
                        imageFormat.format,
                        mipWidth,
                        mipHeight,
                        0,
                        imageFormat.format,
                        imageFormat.dataType,
                        data);
            }

            pos += mipSizes[i];
        }
    }

    /**
     * Update the texture currently bound to target at with data from the given
     * Image at position x and y. The parameter index is used as the zoffset in
     * case a 3d texture or texture 2d array is being updated.
     *
     * @param image Image with the source data (this data will be put into the
     * texture)
     * @param target the target texture
     * @param index the mipmap level to update
     * @param x the x position where to put the image in the texture
     * @param y the y position where to put the image in the texture
     */
    public static void uploadSubTexture(
            Image img,
            int target,
            int index,
            int x,
            int y) {
        if (img.getEfficentData() instanceof AndroidImageInfo) {
            AndroidImageInfo imageInfo = (AndroidImageInfo) img.getEfficentData();
            uploadTextureBitmap(target, imageInfo.getBitmap(), true, true, x, y);
            return;
        }

        // Otherwise upload image directly.
        // Prefer to only use power of 2 textures here to avoid errors.
        Image.Format fmt = img.getFormat();
        ByteBuffer data;
        if (index >= 0 || img.getData() != null && img.getData().size() > 0) {
            data = img.getData(index);
        } else {
            data = null;
        }

        int width = img.getWidth();
        int height = img.getHeight();

        if (!NPOT && img.isNPOT()) {
            // Check if texture is POT
            throw new RendererException("Non-power-of-2 textures "
                    + "are not supported by the video hardware "
                    + "and no scaling path available for image: " + img);
        }
        AndroidGLImageFormat imageFormat = getImageFormat(fmt);

        if (data != null) {
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
        }

        int[] mipSizes = img.getMipMapSizes();
        int pos = 0;
        if (mipSizes == null) {
            if (data != null) {
                mipSizes = new int[]{data.capacity()};
            } else {
                mipSizes = new int[]{width * height * fmt.getBitsPerPixel() / 8};
            }
        }

        for (int i = 0; i < mipSizes.length; i++) {
            int mipWidth = Math.max(1, width >> i);
            int mipHeight = Math.max(1, height >> i);

            if (data != null) {
                data.position(pos);
                data.limit(pos + mipSizes[i]);
            }

            if (imageFormat.compress && data != null) {
                GLES20.glCompressedTexSubImage2D(target, i, x, y, mipWidth, mipHeight, imageFormat.format, data.remaining(), data);
                RendererUtil.checkGLError();
            } else {
                GLES20.glTexSubImage2D(target, i, x, y, mipWidth, mipHeight, imageFormat.format, imageFormat.dataType, data);
                RendererUtil.checkGLError();
            }

            pos += mipSizes[i];
        }
    }
}
