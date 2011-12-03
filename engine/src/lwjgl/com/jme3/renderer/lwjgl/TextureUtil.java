/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
import java.nio.ByteBuffer;
import static org.lwjgl.opengl.ATITextureCompression3DC.GL_COMPRESSED_LUMINANCE_ALPHA_3DC_ATI;
import static org.lwjgl.opengl.EXTTextureCompressionLATC.GL_COMPRESSED_LUMINANCE_ALPHA_LATC2_EXT;
import static org.lwjgl.opengl.EXTTextureCompressionLATC.GL_COMPRESSED_LUMINANCE_LATC1_EXT;
import static org.lwjgl.opengl.EXTTextureCompressionS3TC.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.glCompressedTexImage2D;
import static org.lwjgl.opengl.GL13.glCompressedTexImage3D;
import static org.lwjgl.opengl.GL14.*;
import org.lwjgl.opengl.*;

public class TextureUtil {

    private static boolean isFormatSupported(Format fmt, ContextCapabilities caps){
        switch (fmt){
            case ARGB4444:
                return false;
            case BGR8:
                return caps.OpenGL12 || caps.GL_EXT_bgra;
            case DXT1:
            case DXT1A:
            case DXT3:
            case DXT5:
                return caps.GL_EXT_texture_compression_s3tc;
            case Depth:
            case Depth16:
            case Depth24:
            case Depth32:
                return caps.OpenGL14 || caps.GL_ARB_depth_texture;
            case Depth32F:
            case Luminance16F:
            case Luminance16FAlpha16F:
            case Luminance32F:
            case RGBA16F:
            case RGBA32F:
                return caps.OpenGL30 || caps.GL_ARB_texture_float;
            case LATC:
            case LTC:
                return caps.GL_EXT_texture_compression_latc;
            case RGB9E5:
            case RGB16F_to_RGB9E5:
                return caps.OpenGL30 || caps.GL_EXT_texture_shared_exponent;
            case RGB111110F:
            case RGB16F_to_RGB111110F:
                return caps.OpenGL30 || caps.GL_EXT_packed_float;
            default:
                return true;
        }
    }

    public static void checkFormatSupported(Format fmt) {
        if (!isFormatSupported(fmt, GLContext.getCapabilities())) {
            throw new RendererException("Image format '" + fmt + "' is unsupported by the video hardware.");
        }
    }

    public static int convertTextureFormat(Format fmt){
        switch (fmt){
            case Alpha16:
                return GL_ALPHA16;
            case Alpha8:
                return GL_ALPHA8;
            case DXT1:
                return GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
            case DXT1A:
                return GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
            case DXT3:
                return GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
            case DXT5:
                return GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
            case LATC:
                return GL_COMPRESSED_LUMINANCE_ALPHA_LATC2_EXT;
            case Depth:
                return GL_DEPTH_COMPONENT;
            case Depth16:
                return GL_DEPTH_COMPONENT16;
            case Depth24:
                return GL_DEPTH_COMPONENT24;
            case Depth32:
                return GL_DEPTH_COMPONENT32;
            case Depth32F:
                return ARBDepthBufferFloat.GL_DEPTH_COMPONENT32F;
            case Luminance8Alpha8:
                return GL_LUMINANCE8_ALPHA8;
            case Luminance16Alpha16:
                return GL_LUMINANCE16_ALPHA16;
            case Luminance16FAlpha16F:
                return ARBTextureFloat.GL_LUMINANCE_ALPHA16F_ARB;
            case Intensity8:
                return GL_INTENSITY8;
            case Intensity16:
                return GL_INTENSITY16;
            case Luminance8:
                return GL_LUMINANCE8;
            case Luminance16:
                return GL_LUMINANCE16;
            case Luminance16F:
                return ARBTextureFloat.GL_LUMINANCE16F_ARB;
             case Luminance32F:
                return ARBTextureFloat.GL_LUMINANCE32F_ARB;
            case RGB10:
                return GL_RGB10;
            case RGB16:
                return GL_RGB16;
            case RGB111110F:
                return EXTPackedFloat.GL_R11F_G11F_B10F_EXT;
            case RGB9E5:
                return EXTTextureSharedExponent.GL_RGB9_E5_EXT;
            case RGB16F:
                return ARBTextureFloat.GL_RGB16F_ARB;
            case RGBA16F:
                return ARBTextureFloat.GL_RGBA16F_ARB;
            case RGB32F:
                return ARBTextureFloat.GL_RGB32F_ARB;
            case RGB5A1:
                return GL_RGB5_A1;
            case BGR8:
                return GL_RGB8;
            case RGB8:
                return GL_RGB8;
            case RGBA16:
                return GL_RGBA16;
            case RGBA8:
                return GL_RGBA8;
            default:
                throw new UnsupportedOperationException("Unrecognized format: "+fmt);
        }
    }

    public static void uploadTexture(Image img,
                                     int target,
                                     int index,
                                     int border,
                                     boolean tdc){
        Image.Format fmt = img.getFormat();

        checkFormatSupported(fmt);

        ByteBuffer data;
        if (index >= 0 && img.getData() != null && img.getData().size() > 0){
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
                internalFormat = GL_ALPHA16;
                format = GL_ALPHA;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case Alpha8:
                internalFormat = GL_ALPHA8;
                format = GL_ALPHA;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case DXT1:
                compress = true;
                internalFormat = GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
                format = GL_RGB;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case DXT1A:
                compress = true;
                internalFormat = GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
                format = GL_RGBA;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case DXT3:
                compress = true;
                internalFormat = GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
                format = GL_RGBA;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case DXT5:
                compress = true;
                internalFormat = GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
                format = GL_RGBA;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case LATC:
                compress = true;
                if (tdc){
                    internalFormat = GL_COMPRESSED_LUMINANCE_ALPHA_3DC_ATI;
                }else{
                    internalFormat = GL_COMPRESSED_LUMINANCE_ALPHA_LATC2_EXT;
                }
                format = GL_LUMINANCE_ALPHA;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case LTC:
                compress = true;
                internalFormat = GL_COMPRESSED_LUMINANCE_LATC1_EXT;
                format = GL_LUMINANCE_ALPHA;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case Depth:
                internalFormat = GL_DEPTH_COMPONENT;
                format = GL_DEPTH_COMPONENT;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case Depth16:
                internalFormat = GL_DEPTH_COMPONENT16;
                format = GL_DEPTH_COMPONENT;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case Depth24:
                internalFormat = GL_DEPTH_COMPONENT24;
                format = GL_DEPTH_COMPONENT;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case Depth32:
                internalFormat = GL_DEPTH_COMPONENT32;
                format = GL_DEPTH_COMPONENT;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case Depth32F:
                internalFormat = NVDepthBufferFloat.GL_DEPTH_COMPONENT32F_NV;
                format = GL_DEPTH_COMPONENT;
                dataType = GL_FLOAT;
                break;
            case Luminance16FAlpha16F:
                internalFormat = ARBTextureFloat.GL_LUMINANCE_ALPHA16F_ARB;
                format = GL_LUMINANCE_ALPHA;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case Intensity8:
                internalFormat = GL_INTENSITY8;
                format = GL_INTENSITY;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case Intensity16:
                internalFormat = GL_INTENSITY16;
                format = GL_INTENSITY;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case Luminance8:
                internalFormat = GL_LUMINANCE8;
                format = GL_LUMINANCE;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case Luminance8Alpha8:
                internalFormat = GL_LUMINANCE8_ALPHA8;
                format = GL_LUMINANCE_ALPHA;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case Luminance16Alpha16:
                internalFormat = GL_LUMINANCE16_ALPHA16;
                format = GL_LUMINANCE_ALPHA;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case Luminance16:
                internalFormat = GL_LUMINANCE16;
                format = GL_LUMINANCE;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case Luminance16F:
                internalFormat = ARBTextureFloat.GL_LUMINANCE16F_ARB;
                format = GL_LUMINANCE;
                dataType = ARBHalfFloatPixel.GL_HALF_FLOAT_ARB;
                break;
            case Luminance32F:
                internalFormat = ARBTextureFloat.GL_LUMINANCE32F_ARB;
                format = GL_LUMINANCE;
                dataType = GL_FLOAT;
                break;
            case RGB10:
                internalFormat = GL_RGB10;
                format = GL_RGB;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case RGB16:
                internalFormat = GL_RGB16;
                format = GL_RGB;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case RGB111110F:
                internalFormat = EXTPackedFloat.GL_R11F_G11F_B10F_EXT;
                format = GL_RGB;
                dataType = EXTPackedFloat.GL_UNSIGNED_INT_10F_11F_11F_REV_EXT;
                break;
            case RGB16F_to_RGB111110F:
                internalFormat = EXTPackedFloat.GL_R11F_G11F_B10F_EXT;
                format = GL_RGB;
                dataType = ARBHalfFloatPixel.GL_HALF_FLOAT_ARB;
                break;
            case RGB16F_to_RGB9E5:
                internalFormat = EXTTextureSharedExponent.GL_RGB9_E5_EXT;
                format = GL_RGB;
                dataType = ARBHalfFloatPixel.GL_HALF_FLOAT_ARB;
                break;
            case RGB9E5:
                internalFormat = EXTTextureSharedExponent.GL_RGB9_E5_EXT;
                format = GL_RGB;
                dataType = EXTTextureSharedExponent.GL_UNSIGNED_INT_5_9_9_9_REV_EXT;
                break;
            case RGB16F:
                internalFormat = ARBTextureFloat.GL_RGB16F_ARB;
                format = GL_RGB;
                dataType = ARBHalfFloatPixel.GL_HALF_FLOAT_ARB;
                break;
            case RGBA16F:
                internalFormat = ARBTextureFloat.GL_RGBA16F_ARB;
                format = GL_RGBA;
                dataType = ARBHalfFloatPixel.GL_HALF_FLOAT_ARB;
                break;
            case RGB32F:
                internalFormat = ARBTextureFloat.GL_RGB32F_ARB;
                format = GL_RGB;
                dataType = GL_FLOAT;
                break;
            case RGBA32F:
                internalFormat = ARBTextureFloat.GL_RGBA32F_ARB;
                format = GL_RGBA;
                dataType = GL_FLOAT;
                break;
            case RGB5A1:
                internalFormat = GL_RGB5_A1;
                format = GL_RGBA;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case RGB8:
                internalFormat = GL_RGB8;
                format = GL_RGB;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case BGR8:
                internalFormat = GL_RGB8;
                format = GL_BGR;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case RGBA16:
                internalFormat = GL_RGBA16;
                format = GL_RGBA;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case RGBA8:
                internalFormat = GL_RGBA8;
                format = GL_RGBA;
                dataType = GL_UNSIGNED_BYTE;
                break;
            case ABGR8:
                internalFormat = GL_RGBA8;
                format = EXTAbgr.GL_ABGR_EXT;
                dataType = GL_UNSIGNED_BYTE;
                break;
            default:
                throw new UnsupportedOperationException("Unrecognized format: "+fmt);
        }

        if (data != null)
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        int[] mipSizes = img.getMipMapSizes();
        int pos = 0;
        // TODO: Remove unneccessary allocation
        if (mipSizes == null){
            if (data != null)
                mipSizes = new int[]{ data.capacity() };
            else
                mipSizes = new int[]{ width * height * fmt.getBitsPerPixel() / 8 };
        }

        boolean subtex = false;
        int samples = img.getMultiSamples();

        for (int i = 0; i < mipSizes.length; i++){
            int mipWidth =  Math.max(1, width  >> i);
            int mipHeight = Math.max(1, height >> i);
            int mipDepth =  Math.max(1, depth  >> i);

            if (data != null){
                data.position(pos);
                data.limit(pos + mipSizes[i]);
            }
            
            if (compress && data != null){
                if (target == GL_TEXTURE_3D){
                    glCompressedTexImage3D(target,
                                           i,
                                           internalFormat,
                                           mipWidth,
                                           mipHeight,
                                           mipDepth,
                                           border,
                                           data);
                }else{
                    //all other targets use 2D: array, cubemap, 2d
                    glCompressedTexImage2D(target,
                                           i,
                                           internalFormat,
                                           mipWidth,
                                           mipHeight,
                                           border,
                                           data);
                }
            }else{
                if (target == GL_TEXTURE_3D){
                    glTexImage3D(target,
                                 i,
                                 internalFormat,
                                 mipWidth,
                                 mipHeight,
                                 mipDepth,
                                 border,
                                 format,
                                 dataType,
                                 data);
                }else if (target == EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT){
                    // prepare data for 2D array
                    // or upload slice
                    if (index == -1){
                        glTexImage3D(target,
                                     0,
                                     internalFormat,
                                     mipWidth,
                                     mipHeight,
                                     img.getData().size(), //# of slices
                                     border,
                                     format,
                                     dataType,
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
                                        format,
                                        dataType,
                                        data);
                    }
                }else{
                    if (subtex){
                        if (samples > 1)
                            throw new IllegalStateException("Cannot update multisample textures");

                        glTexSubImage2D(target,
                                        i,
                                        0, 0,
                                        mipWidth, mipHeight,
                                        format,
                                        dataType,
                                        data);
                    }else{
                        if (samples > 1){
                            ARBTextureMultisample.glTexImage2DMultisample(target,
                                                                          samples,
                                                                          internalFormat,
                                                                          mipWidth,
                                                                          mipHeight,
                                                                          true);
                        }else{
                            glTexImage2D(target,
                                         i,
                                         internalFormat,
                                         mipWidth,
                                         mipHeight,
                                         border,
                                         format,
                                         dataType,
                                         data);
                        }
                    }
                }
            }
            
            pos += mipSizes[i];
        }
    }

}
