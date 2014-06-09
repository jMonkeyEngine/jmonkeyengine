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

package com.jme3.renderer.lwjgl;

import com.jme3.renderer.RendererException;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.image.ColorSpace;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.lwjgl.opengl.ARBDepthBufferFloat.*;
import static org.lwjgl.opengl.ARBHalfFloatPixel.*;
import static org.lwjgl.opengl.ARBTextureFloat.*;
import static org.lwjgl.opengl.ARBTextureMultisample.*;
import org.lwjgl.opengl.ContextCapabilities;
import static org.lwjgl.opengl.EXTPackedDepthStencil.*;
import static org.lwjgl.opengl.EXTPackedFloat.*;
import static org.lwjgl.opengl.EXTTextureArray.*;
import static org.lwjgl.opengl.EXTTextureCompressionLATC.*;
import static org.lwjgl.opengl.EXTTextureCompressionS3TC.*;
import static org.lwjgl.opengl.EXTTextureSRGB.*;
import static org.lwjgl.opengl.EXTTextureSharedExponent.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL20.*;

class TextureUtil {

    static class GLImageFormat {
        
        int internalFormat;
        int format;
        int dataType;
        boolean compressed;

        public GLImageFormat(int internalFormat, int format, int dataType, boolean compressed) {
            this.internalFormat = internalFormat;
            this.format = format;
            this.dataType = dataType;
            this.compressed = compressed;
        }
    }
    
    private static final GLImageFormat[] formatToGL = new GLImageFormat[Format.values().length];
    
    private static void setFormat(Format format, int glInternalFormat, int glFormat, int glDataType, boolean glCompressed){
        formatToGL[format.ordinal()] = new GLImageFormat(glInternalFormat, glFormat, glDataType, glCompressed);
    }
    
    static {
        // Alpha formats
        setFormat(Format.Alpha8,  GL_ALPHA8,  GL_ALPHA, GL_UNSIGNED_BYTE, false);
        setFormat(Format.Alpha16, GL_ALPHA16, GL_ALPHA, GL_UNSIGNED_SHORT, false);
        
        // Luminance formats
        setFormat(Format.Luminance8,   GL_LUMINANCE8,  GL_LUMINANCE, GL_UNSIGNED_BYTE, false);
        setFormat(Format.Luminance16,  GL_LUMINANCE16, GL_LUMINANCE, GL_UNSIGNED_SHORT, false);
        setFormat(Format.Luminance16F, GL_LUMINANCE16F_ARB, GL_LUMINANCE, GL_HALF_FLOAT_ARB, false);
        setFormat(Format.Luminance32F, GL_LUMINANCE32F_ARB, GL_LUMINANCE, GL_FLOAT, false);
        
        // Luminance alpha formats
        setFormat(Format.Luminance8Alpha8, GL_LUMINANCE8_ALPHA8,  GL_LUMINANCE_ALPHA, GL_UNSIGNED_BYTE, false);
        setFormat(Format.Luminance16Alpha16, GL_LUMINANCE16_ALPHA16, GL_LUMINANCE_ALPHA, GL_UNSIGNED_SHORT, false);
        setFormat(Format.Luminance16FAlpha16F, GL_LUMINANCE_ALPHA16F_ARB, GL_LUMINANCE_ALPHA, GL_HALF_FLOAT_ARB, false);
        
        // Depth formats
        setFormat(Format.Depth,    GL_DEPTH_COMPONENT,    GL_DEPTH_COMPONENT, GL_UNSIGNED_BYTE, false);
        setFormat(Format.Depth16,  GL_DEPTH_COMPONENT16,  GL_DEPTH_COMPONENT, GL_UNSIGNED_SHORT, false);
        setFormat(Format.Depth24,  GL_DEPTH_COMPONENT24,  GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, false);
        setFormat(Format.Depth32,  GL_DEPTH_COMPONENT32,  GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, false);
        setFormat(Format.Depth32F, GL_DEPTH_COMPONENT32F, GL_DEPTH_COMPONENT, GL_FLOAT,         false);
        
        // Depth stencil formats
        setFormat(Format.Depth24Stencil8, GL_DEPTH24_STENCIL8_EXT, GL_DEPTH_STENCIL_EXT, GL_UNSIGNED_INT_24_8_EXT, false);

        // RGB formats
        setFormat(Format.BGR8,        GL_RGB8,   GL_BGR,  GL_UNSIGNED_BYTE, false);
        setFormat(Format.ARGB8,       GL_RGBA8,  GL_BGRA, GL_UNSIGNED_INT_8_8_8_8, false);
        setFormat(Format.BGRA8,       GL_RGBA8,  GL_BGRA, GL_UNSIGNED_BYTE, false);
        setFormat(Format.RGB8,        GL_RGB8,   GL_RGB,  GL_UNSIGNED_BYTE, false);
//        setFormat(Format.RGB10,      GL_RGB10, GL_RGB,        GL_UNSIGNED_INT_10_10_10_2, false); 
        setFormat(Format.RGB16,      GL_RGB16, GL_RGB,  GL_UNSIGNED_SHORT, false); 
        setFormat(Format.RGB16F,     GL_RGB16F_ARB, GL_RGB, GL_HALF_FLOAT_ARB, false);
        setFormat(Format.RGB32F,     GL_RGB32F_ARB, GL_RGB, GL_FLOAT, false);
        
        // Special RGB formats
        setFormat(Format.RGB111110F,           GL_R11F_G11F_B10F_EXT, GL_RGB, GL_UNSIGNED_INT_10F_11F_11F_REV_EXT, false);
        setFormat(Format.RGB9E5,               GL_RGB9_E5_EXT,         GL_RGB, GL_UNSIGNED_INT_5_9_9_9_REV_EXT, false);
        setFormat(Format.RGB16F_to_RGB111110F, GL_R11F_G11F_B10F_EXT, GL_RGB, GL_HALF_FLOAT_ARB, false);
        setFormat(Format.RGB16F_to_RGB9E5,     GL_RGB9_E5_EXT,         GL_RGB, GL_HALF_FLOAT_ARB, false);
        setFormat(Format.RGB10_A2,             GL_RGB10_A2,        GL_RGBA, GL_UNSIGNED_INT_10_10_10_2, false);
        
        // RGBA formats
        setFormat(Format.ABGR8,   GL_RGBA8,  GL_RGBA, GL_UNSIGNED_INT_8_8_8_8, false);
        setFormat(Format.RGB5A1,  GL_RGB5_A1, GL_RGBA, GL_UNSIGNED_SHORT_5_5_5_1, false);
        setFormat(Format.ARGB4444,GL_RGBA4,   GL_RGBA, GL_UNSIGNED_SHORT_4_4_4_4_REV, false);
        setFormat(Format.RGBA8,   GL_RGBA8,   GL_RGBA, GL_UNSIGNED_BYTE, false);
        setFormat(Format.RGBA16,  GL_RGBA16,  GL_RGBA, GL_UNSIGNED_SHORT, false); // might be incorrect
        setFormat(Format.RGBA16F, GL_RGBA16F_ARB, GL_RGBA, GL_HALF_FLOAT_ARB, false);
        setFormat(Format.RGBA32F, GL_RGBA32F_ARB, GL_RGBA, GL_FLOAT, false);
        
        // DXT formats
        setFormat(Format.DXT1,  GL_COMPRESSED_RGB_S3TC_DXT1_EXT, GL_RGB,   GL_UNSIGNED_BYTE, true);
        setFormat(Format.DXT1A, GL_COMPRESSED_RGBA_S3TC_DXT1_EXT, GL_RGBA, GL_UNSIGNED_BYTE, true);
        setFormat(Format.DXT3,  GL_COMPRESSED_RGBA_S3TC_DXT3_EXT, GL_RGBA, GL_UNSIGNED_BYTE, true);
        setFormat(Format.DXT5,  GL_COMPRESSED_RGBA_S3TC_DXT5_EXT, GL_RGBA, GL_UNSIGNED_BYTE, true);
    
        // LTC/LATC/3Dc formats
        setFormat(Format.LTC,  GL_COMPRESSED_LUMINANCE_LATC1_EXT,       GL_LUMINANCE,       GL_UNSIGNED_BYTE, true);
        setFormat(Format.LATC, GL_COMPRESSED_LUMINANCE_ALPHA_LATC2_EXT, GL_LUMINANCE_ALPHA, GL_UNSIGNED_BYTE, true);   
    }
    
    //sRGB formats        
    private static final GLImageFormat sRGB_RGB8 = new GLImageFormat(GL_SRGB8_EXT, GL_RGB, GL_UNSIGNED_BYTE, false);
    private static final GLImageFormat sRGB_RGBA8 = new GLImageFormat(GL_SRGB8_ALPHA8_EXT, GL_RGBA, GL_UNSIGNED_BYTE, false);
    private static final GLImageFormat sRGB_Luminance8 = new GLImageFormat(GL_SLUMINANCE8_EXT, GL_LUMINANCE, GL_UNSIGNED_BYTE, false);
    private static final GLImageFormat sRGB_LuminanceAlpha8 = new GLImageFormat(GL_SLUMINANCE8_ALPHA8_EXT, GL_LUMINANCE_ALPHA, GL_UNSIGNED_BYTE, false);
    private static final GLImageFormat sRGB_BGR8 = new GLImageFormat(GL_SRGB8_EXT, GL_BGR, GL_UNSIGNED_BYTE, false);
    private static final GLImageFormat sRGB_ABGR8 = new GLImageFormat(GL_SRGB8_ALPHA8_EXT, GL_RGBA, GL_UNSIGNED_INT_8_8_8_8, false);
    private static final GLImageFormat sRGB_ARGB8 = new GLImageFormat(GL_SRGB8_ALPHA8_EXT, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8, false);
    private static final GLImageFormat sRGB_BGRA8 = new GLImageFormat(GL_SRGB8_ALPHA8_EXT, GL_BGRA, GL_UNSIGNED_BYTE, false);
    private static final GLImageFormat sRGB_DXT1 = new GLImageFormat(GL_COMPRESSED_SRGB_S3TC_DXT1_EXT,GL_RGB,   GL_UNSIGNED_BYTE, true);
    private static final GLImageFormat sRGB_DXT1A = new GLImageFormat(GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT1_EXT, GL_RGBA, GL_UNSIGNED_BYTE, true);
    private static final GLImageFormat sRGB_DXT3 = new GLImageFormat(GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT3_EXT, GL_RGBA, GL_UNSIGNED_BYTE, true);
    private static final GLImageFormat sRGB_DXT5 = new GLImageFormat(GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT5_EXT, GL_RGBA, GL_UNSIGNED_BYTE, true);

    public static GLImageFormat getImageFormat(ContextCapabilities caps, Format fmt, boolean isSrgb){
        switch (fmt){
            case DXT1:
            case DXT1A:
            case DXT3:
            case DXT5:
                if (!caps.GL_EXT_texture_compression_s3tc) {
                    return null;
                }
                break;
            case Depth24Stencil8:
                if (!caps.OpenGL30 && !caps.GL_EXT_packed_depth_stencil){
                    return null;
                }
                break;
            case Luminance16F:
            case Luminance16FAlpha16F:
            case Luminance32F:
            case RGB16F:
            case RGB32F:
            case RGBA16F:
            case RGBA32F:
                if (!caps.OpenGL30 && !caps.GL_ARB_texture_float){
                    return null;
                }
                break;
            case Depth32F:
                if (!caps.OpenGL30 && !caps.GL_NV_depth_buffer_float){
                    return null;
                }
                break;
            case LATC:
            case LTC:
                if (!caps.GL_EXT_texture_compression_latc){
                    return null;
                }
                break;
            case RGB9E5:
            case RGB16F_to_RGB9E5:
                if (!caps.OpenGL30 && !caps.GL_EXT_texture_shared_exponent){
                    return null;
                }
                break;
            case RGB111110F:
            case RGB16F_to_RGB111110F:
                if (!caps.OpenGL30 && !caps.GL_EXT_packed_float){
                    return null;
                }
                break;
        }
        if (isSrgb) {
            return getSrgbFormat(fmt);
        } else {
            return formatToGL[fmt.ordinal()];
        }
    }
    
    public static GLImageFormat getImageFormatWithError(ContextCapabilities caps, Format fmt, boolean isSrgb) {
        GLImageFormat glFmt = getImageFormat(caps, fmt, isSrgb);
        if (glFmt == null) {
            throw new RendererException("Image format '" + fmt + "' is unsupported by the video hardware.");
        }
        return glFmt;
    }
    
    private static GLImageFormat getSrgbFormat(Format fmt){
        switch (fmt) {
            case RGB8:
                return sRGB_RGB8;
            case RGBA8:
                return sRGB_RGBA8;
            case BGR8:
                return sRGB_BGR8;
            case ABGR8:
                return sRGB_ABGR8;
            case ARGB8:
                return sRGB_ARGB8;
            case BGRA8:
                return sRGB_BGRA8;
            case Luminance8:
                return sRGB_Luminance8;
            case Luminance8Alpha8:
                return sRGB_LuminanceAlpha8;
            case DXT1:
                return sRGB_DXT1;
            case DXT1A:
                return sRGB_DXT1A;
            case DXT3:
                return sRGB_DXT3;
            case DXT5:
                return sRGB_DXT5;
            default:
                Logger.getLogger(TextureUtil.class.getName()).log(Level.WARNING, "Format {0} has no sRGB equivalent, using linear format.", fmt.toString());
                return formatToGL[fmt.ordinal()];
        }
    }
    
    public static void uploadTexture(ContextCapabilities caps,
                                     Image image,
                                     int target,
                                     int index,
                                     int border,
                                     boolean linearizeSrgb){
        
        Image.Format fmt = image.getFormat();
        GLImageFormat glFmt = getImageFormatWithError(caps, fmt, image.getColorSpace() == ColorSpace.sRGB && linearizeSrgb);

        ByteBuffer data;
        if (index >= 0 && image.getData() != null && image.getData().size() > 0){
            data = image.getData(index);
        }else{
            data = null;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int depth = image.getDepth();

        if (data != null) {
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        }

        int[] mipSizes = image.getMipMapSizes();
        int pos = 0;
        // TODO: Remove unneccessary allocation
        if (mipSizes == null){
            if (data != null)
                mipSizes = new int[]{ data.capacity() };
            else
                mipSizes = new int[]{ width * height * fmt.getBitsPerPixel() / 8 };
        }

        boolean subtex = false;
        int samples = image.getMultiSamples();

        for (int i = 0; i < mipSizes.length; i++){
            int mipWidth =  Math.max(1, width  >> i);
            int mipHeight = Math.max(1, height >> i);
            int mipDepth =  Math.max(1, depth  >> i);

            if (data != null){
                data.position(pos);
                data.limit(pos + mipSizes[i]);
            }
            
            if (glFmt.compressed && data != null){
                if (target == GL_TEXTURE_3D){
                    glCompressedTexImage3D(target,
                                            i,
                                            glFmt.internalFormat,
                                            mipWidth,
                                            mipHeight,
                                            mipDepth,
                                            border,
                                            data);
                }else{
                    //all other targets use 2D: array, cubemap, 2d
                    glCompressedTexImage2D(target,
                                            i,
                                            glFmt.internalFormat,
                                            mipWidth,
                                            mipHeight,
                                            border,
                                            data);
                }
            }else{
                if (target == GL_TEXTURE_3D){
                    glTexImage3D(target,
                                i,
                                glFmt.internalFormat,
                                mipWidth,
                                mipHeight,
                                mipDepth,
                                border,
                                glFmt.format,
                                glFmt.dataType,
                                data);
                }else if (target == GL_TEXTURE_2D_ARRAY_EXT){
                    // prepare data for 2D array
                    // or upload slice
                    if (index == -1){
                        glTexImage3D(target,
                                    i,
                                    glFmt.internalFormat,
                                    mipWidth,
                                    mipHeight,
                                    image.getData().size(), //# of slices
                                    border,
                                    glFmt.format,
                                    glFmt.dataType,
                                    data);
                    }else{
                        glTexSubImage3D(target,
                                        i, // level
                                        0, // xoffset
                                        0, // yoffset
                                        index, // zoffset
                                        width, // width
                                        height, // height
                                        1, // depth
                                        glFmt.format,
                                        glFmt.dataType,
                                        data);
                    }
                }else{
                    if (subtex){
                        if (samples > 1){
                            throw new IllegalStateException("Cannot update multisample textures");
                        }

                        glTexSubImage2D(target,
                                        i,
                                        0, 0,
                                        mipWidth, mipHeight,
                                        glFmt.format,
                                        glFmt.dataType,
                                        data);
                    }else{
                        if (samples > 1){
                            glTexImage2DMultisample(target,
                                                    samples,
                                                    glFmt.internalFormat,
                                                    mipWidth,
                                                    mipHeight,
                                                    true);
                        }else{
                            glTexImage2D(target,
                                        i,
                                        glFmt.internalFormat,
                                        mipWidth,
                                        mipHeight,
                                        border,
                                        glFmt.format,
                                        glFmt.dataType,
                                        data);
                        }
                    }
                }
            }
            
            pos += mipSizes[i];
        }
    }

    /**
     * Update the texture currently bound to target at with data from the given Image at position x and y. The parameter
     * index is used as the zoffset in case a 3d texture or texture 2d array is being updated.
     *
     * @param image Image with the source data (this data will be put into the texture)
     * @param target the target texture
     * @param index the mipmap level to update
     * @param x the x position where to put the image in the texture
     * @param y the y position where to put the image in the texture
     */
    public static void uploadSubTexture(
        ContextCapabilities caps,
        Image image,
        int target,
        int index,
        int x,
        int y,
        boolean linearizeSrgb) {
      Image.Format fmt = image.getFormat();
      GLImageFormat glFmt = getImageFormatWithError(caps, fmt, image.getColorSpace() == ColorSpace.sRGB  && linearizeSrgb);

      ByteBuffer data = null;
      if (index >= 0 && image.getData() != null && image.getData().size() > 0) {
        data = image.getData(index);
      }

      int width = image.getWidth();
      int height = image.getHeight();
      int depth = image.getDepth();

      if (data != null) {
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
      }

      int[] mipSizes = image.getMipMapSizes();
      int pos = 0;

      // TODO: Remove unneccessary allocation
      if (mipSizes == null){
        if (data != null) {
          mipSizes = new int[]{ data.capacity() };
        } else {
          mipSizes = new int[]{ width * height * fmt.getBitsPerPixel() / 8 };
        }
      }

      int samples = image.getMultiSamples();

      for (int i = 0; i < mipSizes.length; i++){
        int mipWidth =  Math.max(1, width  >> i);
        int mipHeight = Math.max(1, height >> i);
        int mipDepth =  Math.max(1, depth  >> i);

        if (data != null){
          data.position(pos);
          data.limit(pos + mipSizes[i]);
        }

        // to remove the cumbersome if/then/else stuff below we'll update the pos right here and use continue after each
        // gl*Image call in an attempt to unclutter things a bit
        pos += mipSizes[i];

        int glFmtInternal = glFmt.internalFormat;
        int glFmtFormat = glFmt.format;
        int glFmtDataType = glFmt.dataType;

        if (glFmt.compressed && data != null){
          if (target == GL_TEXTURE_3D){
            glCompressedTexSubImage3D(target, i, x, y, index, mipWidth, mipHeight, mipDepth, glFmtInternal, data);
            continue;
          }

          // all other targets use 2D: array, cubemap, 2d
          glCompressedTexSubImage2D(target, i, x, y, mipWidth, mipHeight, glFmtInternal, data);
          continue;
        }

        if (target == GL_TEXTURE_3D){
          glTexSubImage3D(target, i, x, y, index, mipWidth, mipHeight, mipDepth, glFmtFormat, glFmtDataType, data);
          continue;
        }

        if (target == GL_TEXTURE_2D_ARRAY_EXT){
          // prepare data for 2D array or upload slice
          if (index == -1){
            glTexSubImage3D(target, i, x, y, index, mipWidth, mipHeight, mipDepth, glFmtFormat, glFmtDataType, data);
            continue;
          }

          glTexSubImage3D(target, i, x, y, index, width, height, 1, glFmtFormat, glFmtDataType, data);
          continue;
        }

        if (samples > 1){
          throw new IllegalStateException("Cannot update multisample textures");
        }

        glTexSubImage2D(target, i, x, y, mipWidth, mipHeight, glFmtFormat, glFmtDataType, data);
      }
    }
}
