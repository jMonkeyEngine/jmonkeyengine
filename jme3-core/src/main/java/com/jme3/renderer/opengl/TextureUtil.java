/*
 * Copyright (c) 2009-2024 jMonkeyEngine
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
package com.jme3.renderer.opengl;

import com.jme3.renderer.Caps;
import com.jme3.renderer.RendererException;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Internal utility class used by {@link GLRenderer} to manage textures.
 * 
 * @author Kirill Vainer
 */
public final class TextureUtil {

    private static final Logger logger = Logger.getLogger(TextureUtil.class.getName());

    private final GL gl;
    private final GL2 gl2;
    private final GLExt glext;
    private GLImageFormat[][] formats;
    private boolean supportUnpackRowLength;
    private ByteBuffer packedPixelBuffer;
    
    public TextureUtil(GL gl, GL2 gl2, GLExt glext) {
        this.gl = gl;
        this.gl2 = gl2;
        this.glext = glext;
    }
    
    public void initialize(EnumSet<Caps> caps) {
        supportUnpackRowLength = caps.contains(Caps.UnpackRowLength);
        this.formats = GLImageFormats.getFormatsForCaps(caps);
        if (logger.isLoggable(Level.FINE)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Supported texture formats: \n");
            for (int i = 0; i < Format.values().length; i++) {
                Format format = Format.values()[i];
                if (formats[0][i] != null) {
                    boolean srgb = formats[1][i] != null;
                    sb.append("\t").append(format.toString());
                    sb.append(" (Linear");
                    if (srgb) sb.append("/sRGB");
                    sb.append(")\n");
                }
            }
            logger.log(Level.FINE, sb.toString());
        }
    }

    public GLImageFormat getImageFormat(Format fmt, boolean isSrgb) {
        if (isSrgb) {
            return formats[1][fmt.ordinal()];
        } else {
            return formats[0][fmt.ordinal()];
        }
    }

    public GLImageFormat getImageFormatWithError(Format fmt, boolean isSrgb) {
        //if the passed format is one kind of depth there is no point in getting the srgb format;
        isSrgb = isSrgb && !fmt.isDepthFormat();
        GLImageFormat glFmt = getImageFormat(fmt, isSrgb);
        if (glFmt == null && isSrgb) {
            glFmt = getImageFormat(fmt, false);               
            logger.log(Level.WARNING, "No sRGB format available for ''{0}''. Falling back to linear.", fmt);
        }
        if (glFmt == null) { 
            throw new RendererException("Image format '" + fmt + "' is unsupported by the video hardware.");
        }
        return glFmt;
    }
    
    private void setupTextureSwizzle(int target, Format format) {
        // Needed for OpenGL 3.3 to support luminance / alpha formats
        switch (format) {
            case Alpha8:
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_R, GL.GL_ZERO);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_G, GL.GL_ZERO);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_B, GL.GL_ZERO);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_A, GL.GL_RED);
                break;
            case Luminance8:
            case Luminance16F:
            case Luminance32F:
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_R, GL.GL_RED);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_G, GL.GL_RED);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_B, GL.GL_RED);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_A, GL.GL_ONE);
                break;
            case Luminance8Alpha8:
            case Luminance16FAlpha16F:
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_R, GL.GL_RED);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_G, GL.GL_RED);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_B, GL.GL_RED);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_A, GL.GL_GREEN);
                break;
            case BGR8:
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_R, GL.GL_BLUE);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_G, GL.GL_GREEN);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_B, GL.GL_RED);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_A, GL.GL_ONE);
                break;
            case ARGB8:
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_R, GL.GL_GREEN);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_G, GL.GL_BLUE);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_B, GL.GL_ALPHA);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_A, GL.GL_RED);
                break;
            case BGRA8:
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_R, GL.GL_BLUE);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_G, GL.GL_GREEN);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_B, GL.GL_RED);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_A, GL.GL_ALPHA);
                break;
            case ABGR8:
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_R, GL.GL_ALPHA);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_G, GL.GL_BLUE);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_B, GL.GL_GREEN);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_A, GL.GL_RED);
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }
    
    private static boolean requiresPackedExpansion(Format format, GLImageFormat glFormat) {
        return glFormat.dataType == GL.GL_UNSIGNED_BYTE
                && (format == Format.RGB565 || format == Format.RGB5A1);
    }

    private ByteBuffer expandPackedPixels(ByteBuffer data, Format format, int pixelCount) {
        // GL reads packed shorts in native byte order, regardless of the ByteBuffer's byte-order metadata.
        ByteBuffer source = data.duplicate().order(ByteOrder.nativeOrder());
        boolean hasAlpha = format == Format.RGB5A1;
        int components = hasAlpha ? 4 : 3;
        if (pixelCount > source.remaining() / 2) {
            throw new IllegalArgumentException("Packed image data is too small for the requested dimensions");
        }
        int requiredBytes = Math.multiplyExact(pixelCount, components);
        if (packedPixelBuffer == null || packedPixelBuffer.capacity() < requiredBytes) {
            cleanup();
            packedPixelBuffer = BufferUtils.createByteBuffer(requiredBytes);
        }
        // Client-memory pixels have been consumed when the preceding GL upload returns.
        ByteBuffer expanded = packedPixelBuffer;
        expanded.clear();
        for (int i = 0; i < pixelCount; i++) {
            int pixel = source.getShort() & 0xffff;
            int red = pixel >> 11;
            int green = hasAlpha ? (pixel >> 6) & 0x1f : (pixel >> 5) & 0x3f;
            int blue = hasAlpha ? (pixel >> 1) & 0x1f : pixel & 0x1f;
            // Preserve the encoded sRGB values; the sRGB texture performs linearization when sampled.
            expanded.put((byte) ((red * 255 + 15) / 31));
            expanded.put((byte) ((green * 255 + (hasAlpha ? 15 : 31)) / (hasAlpha ? 31 : 63)));
            expanded.put((byte) ((blue * 255 + 15) / 31));
            if (hasAlpha) {
                expanded.put((byte) ((pixel & 1) * 255));
            }
        }
        expanded.flip();
        return expanded;
    }

    /** Releases the owned CPU conversion scratch buffer; repeated calls are safe. */
    void cleanup() {
        ByteBuffer buffer = packedPixelBuffer;
        packedPixelBuffer = null;
        if (buffer != null) {
            BufferUtils.destroyDirectBuffer(buffer);
        }
    }

    private void uploadTextureLevel(GLImageFormat format, int target, int level, int slice, int sliceCount, int width, int height, int depth, int samples, ByteBuffer data) {
        if (format.compressed && data != null) {
            if (target == GL2.GL_TEXTURE_3D) {
                // For 3D textures, we upload the entire mipmap level.
                gl2.glCompressedTexImage3D(target,
                                            level,
                                            format.internalFormat,
                                            width,
                                            height,
                                            depth,
                                            0,
                                            data);
            } else if (target == GLExt.GL_TEXTURE_2D_ARRAY_EXT) {
                // For texture arrays, only upload 1 slice at a time.
                // zoffset specifies slice index, and depth is 1 to indicate
                // a single texture in the array.
                gl2.glCompressedTexSubImage3D(target,
                                              level,
                                              0,
                                              0,
                                              slice,
                                              width,
                                              height,
                                              1,
                                              format.internalFormat,
                                              data);
            } else {
                // Cubemaps also use 2D upload.
                gl2.glCompressedTexImage2D(target,
                                           level,
                                           format.internalFormat,
                                           width,
                                           height,
                                           0,
                                           data);
            }
        } else {
            // (Non-compressed OR allocating texture storage for FBO)
            if (target == GL2.GL_TEXTURE_3D) {
                gl2.glTexImage3D(target,
                                 level,
                                 format.internalFormat,
                                 width,
                                 height,
                                 depth,
                                 0,
                                 format.format,
                                 format.dataType,
                                 data);
            } else if (target == GLExt.GL_TEXTURE_2D_ARRAY_EXT) {
                if (slice == -1) {
                    // Allocate texture storage (data is NULL)
                    gl2.glTexImage3D(target,
                                     level,
                                     format.internalFormat,
                                     width,
                                     height,
                                     sliceCount, //# of slices
                                     0,
                                     format.format,
                                     format.dataType,
                                     data);
                } else {
                    // For texture arrays, only upload 1 slice at a time.
                    // zoffset specifies slice index, and depth is 1 to indicate
                    // a single texture in the array.
                    gl2.glTexSubImage3D(target,
                                        level,          // level
                                        0,              // xoffset
                                        0,              // yoffset
                                        slice,          // zoffset
                                        width,          // width
                                        height,         // height
                                        1,              // depth
                                        format.format,
                                        format.dataType,
                                        data);
                }
            } else {
                // 2D multisampled image.
                if (samples > 1) {
                    glext.glTexImage2DMultisample(target,
                                                  samples,
                                                  format.internalFormat,
                                                  width,
                                                  height,
                                                  true);
                } else {
                    // Regular 2D image
                    gl.glTexImage2D(target,
                                    level,
                                    format.internalFormat,
                                    width,
                                    height,
                                    0,
                                    format.format,
                                    format.dataType,
                                    data);
                }
            }
        }
    }

    public void uploadTexture(Image image,
                              int target,
                              int index,
                              boolean linearizeSrgb) {

        boolean getSrgbFormat = image.getColorSpace() == ColorSpace.sRGB && linearizeSrgb;
        Image.Format jmeFormat = image.getFormat();
        final GLImageFormat oglFormat = getImageFormatWithError(jmeFormat, getSrgbFormat);

        ByteBuffer data = null;
        int sliceCount = 1;
        
        if (index >= 0) {
            ByteBuffer source = image.getData(index);
            if (source != null) {
                data = source.duplicate();
                data.clear();
            }
        }
        
        if (image.getData() != null && image.getData().size() > 0) {
            sliceCount = image.getData().size();
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int depth = image.getDepth();
        
        int[] mipSizes = image.getMipMapSizes();
        int pos = 0;
        // TODO: Remove unnecessary allocation
        if (mipSizes == null) {
            if (data != null) {
                mipSizes = new int[]{data.capacity()};
            } else {
                mipSizes = new int[]{width * height * jmeFormat.getBitsPerPixel() / 8};
            }
        }

        int samples = image.getMultiSamples();
        
        // For OGL3 core: setup texture swizzle.
        if (oglFormat.swizzleRequired) {
            setupTextureSwizzle(target, jmeFormat);
        }

        for (int i = 0; i < mipSizes.length; i++) {
            int mipWidth = Math.max(1, width >> i);
            int mipHeight = Math.max(1, height >> i);
            int mipDepth = Math.max(1, depth >> i);

            if (data != null) {
                data.position(pos);
                data.limit(pos + mipSizes[i]);
            }

            ByteBuffer uploadData = data;
            if (data != null && requiresPackedExpansion(jmeFormat, oglFormat)) {
                int pixelCount = Math.multiplyExact(mipWidth, mipHeight);
                if (target == GL2.GL_TEXTURE_3D) {
                    pixelCount = Math.multiplyExact(pixelCount, mipDepth);
                }
                uploadData = expandPackedPixels(data, jmeFormat, pixelCount);
            }
            uploadTextureLevel(oglFormat, target, i, index, sliceCount,
                    mipWidth, mipHeight, mipDepth, samples, uploadData);

            pos += mipSizes[i];
        }
    }

    /**
     * @deprecated Use uploadSubTexture(int target,  Image src, int index,int targetX, int targetY,int srcX,int srcY,  int areaWidth,int areaHeight, boolean linearizeSrgb) 
     */
    @Deprecated
    public void uploadSubTexture(Image image, int target, int index, int x, int y, boolean linearizeSrgb) {
        if (target != GL.GL_TEXTURE_2D || image.getDepth() > 1) {
            throw new UnsupportedOperationException("Updating non-2D texture is not supported");
        }
        
        if (image.getMipMapSizes() != null) {
            throw new UnsupportedOperationException("Updating mip-mapped images is not supported");
        }
        
        if (image.getMultiSamples() > 1) {
            throw new UnsupportedOperationException("Updating multisampled images is not supported");
        }
        
        Image.Format jmeFormat = image.getFormat();
        
        if (jmeFormat.isCompressed()) {
            throw new UnsupportedOperationException("Updating compressed images is not supported");
        } else if (jmeFormat.isDepthFormat()) {
            throw new UnsupportedOperationException("Updating depth images is not supported");
        }
        
        boolean getSrgbFormat = image.getColorSpace() == ColorSpace.sRGB && linearizeSrgb;
        final GLImageFormat oglFormat = getImageFormatWithError(jmeFormat, getSrgbFormat);
        
        ByteBuffer data = null;
        
        if (index >= 0) {
            ByteBuffer source = image.getData(index);
            if (source != null) {
                data = source.duplicate();
                data.clear();
            }
        }
        
        if (data == null) {
            throw new IndexOutOfBoundsException("The image index " + index + " is not valid for the given image");
        }

        data.position(0);
        data.limit(data.capacity());
        if (requiresPackedExpansion(jmeFormat, oglFormat)) {
            data = expandPackedPixels(data, jmeFormat,
                    Math.multiplyExact(image.getWidth(), image.getHeight()));
        }
        
        gl.glTexSubImage2D(target, 0, x, y, image.getWidth(), image.getHeight(), 
                           oglFormat.format, oglFormat.dataType, data);
    }

    public void uploadSubTexture(int target, Image src, int index, int targetX, int targetY, int areaX, int areaY, int areaWidth, int areaHeight, boolean linearizeSrgb) {
        uploadSubTexture(target, src, index, targetX, targetY, areaX, areaY, areaWidth, areaHeight,
                linearizeSrgb, src.getColorSpace());
    }

    void uploadSubTexture(int target, Image src, int index, int targetX, int targetY,
            int areaX, int areaY, int areaWidth, int areaHeight,
            boolean linearizeSrgb, ColorSpace destinationColorSpace) {
        if (target != GL.GL_TEXTURE_2D || src.getDepth() > 1) {
            throw new UnsupportedOperationException("Updating non-2D texture is not supported");
        }

        if (src.getMipMapSizes() != null) {
            throw new UnsupportedOperationException("Updating mip-mapped images is not supported");
        }

        if (src.getMultiSamples() > 1) {
            throw new UnsupportedOperationException("Updating multisampled images is not supported");
        }

        Image.Format jmeFormat = src.getFormat();

        if (jmeFormat.isCompressed()) {
            throw new UnsupportedOperationException("Updating compressed images is not supported");
        } else if (jmeFormat.isDepthFormat()) {
            throw new UnsupportedOperationException("Updating depth images is not supported");
        }

        // The destination storage determines which pixel types GLES3 accepts, even when the
        // source has a different color space. This is a raw copy, without CPU gamma conversion.
        boolean getSrgbFormat = destinationColorSpace == ColorSpace.sRGB && linearizeSrgb;
        final GLImageFormat oglFormat = getImageFormatWithError(jmeFormat, getSrgbFormat);

        ByteBuffer data = src.getData(index);

        if (data == null) {
            throw new IndexOutOfBoundsException("The image index " + index + " is not valid for the given image");
        }

        data = data.duplicate();
        int bytesPerPixel = src.getFormat().getBitsPerPixel() / 8;
        if (requiresPackedExpansion(jmeFormat, oglFormat)) {
            data.clear();
            data = expandPackedPixels(data, jmeFormat, Math.multiplyExact(src.getWidth(), src.getHeight()));
            bytesPerPixel = jmeFormat == Format.RGB565 ? 3 : 4;
        }

        int srcWidth = src.getWidth();
        int skip = areaX;
        skip += areaY * srcWidth;
        skip *= bytesPerPixel;

        data.position(skip);

        boolean needsStride = srcWidth != areaWidth;

        if (needsStride && (!supportUnpackRowLength)) { // doesn't support stride, copy row by row (slower).
            for (int i = 0; i < areaHeight; i++) {
                data.position(skip + (srcWidth * bytesPerPixel * i));
                gl.glTexSubImage2D(target, 0, targetX, targetY + i, areaWidth, 1,
                        oglFormat.format, oglFormat.dataType, data);
            }
        } else {
            if (needsStride)
                gl.glPixelStorei(GL.GL_UNPACK_ROW_LENGTH, srcWidth);
            gl.glTexSubImage2D(target, 0, targetX, targetY, areaWidth, areaHeight, oglFormat.format, oglFormat.dataType, data);
            if (needsStride)
                gl.glPixelStorei(GL.GL_UNPACK_ROW_LENGTH, 0);
        }

    }

}
