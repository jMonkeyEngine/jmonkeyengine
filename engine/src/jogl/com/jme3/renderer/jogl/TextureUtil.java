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

package com.jme3.renderer.jogl;

import com.jme3.renderer.RendererException;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import java.nio.ByteBuffer;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLContext;

public class TextureUtil {
    
    private static boolean abgrToRgbaConversionEnabled = false;

    public static int convertTextureFormat(Format fmt) {
        switch (fmt) {
            case Alpha16:
            case Alpha8:
                return GL.GL_ALPHA;
            case Luminance8Alpha8:
            case Luminance16Alpha16:
                return GL.GL_LUMINANCE_ALPHA;
            case Luminance8:
            case Luminance16:
                return GL.GL_LUMINANCE;
            case RGB10:
            case RGB16:
            case BGR8:
            case RGB8:
            case RGB565:
                return GL.GL_RGB;
            case RGB5A1:
            case RGBA16:
            case RGBA8:
                return GL.GL_RGBA;
            case Depth:
                return GL2ES2.GL_DEPTH_COMPONENT;
            default:
                throw new UnsupportedOperationException("Unrecognized format: " + fmt);
        }
    }
    
    public static class GLImageFormat {
        
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
        setFormat(Format.Alpha8,  GL2.GL_ALPHA8,  GL.GL_ALPHA, GL.GL_UNSIGNED_BYTE, false);
        setFormat(Format.Alpha16, GL2.GL_ALPHA16, GL.GL_ALPHA, GL.GL_UNSIGNED_SHORT, false);
        
        // Luminance formats
        setFormat(Format.Luminance8,   GL2.GL_LUMINANCE8,  GL.GL_LUMINANCE, GL.GL_UNSIGNED_BYTE, false);
        setFormat(Format.Luminance16,  GL2.GL_LUMINANCE16, GL.GL_LUMINANCE, GL.GL_UNSIGNED_SHORT, false);
        setFormat(Format.Luminance16F, GL.GL_LUMINANCE16F_ARB, GL.GL_LUMINANCE, GL.GL_HALF_FLOAT, false);
        setFormat(Format.Luminance32F, GL.GL_LUMINANCE32F_ARB, GL.GL_LUMINANCE, GL.GL_FLOAT, false);
        
        // Luminance alpha formats
        setFormat(Format.Luminance8Alpha8, GL2.GL_LUMINANCE8_ALPHA8,  GL.GL_LUMINANCE_ALPHA, GL.GL_UNSIGNED_BYTE, false);
        setFormat(Format.Luminance16Alpha16, GL2.GL_LUMINANCE16_ALPHA16, GL.GL_LUMINANCE_ALPHA, GL.GL_UNSIGNED_SHORT, false);
        setFormat(Format.Luminance16FAlpha16F, GL.GL_LUMINANCE_ALPHA16F_ARB, GL.GL_LUMINANCE_ALPHA, GL.GL_HALF_FLOAT, false);
        
        // Depth formats
        setFormat(Format.Depth,    GL2ES2.GL_DEPTH_COMPONENT,    GL2ES2.GL_DEPTH_COMPONENT, GL.GL_UNSIGNED_BYTE, false);
        setFormat(Format.Depth16,  GL.GL_DEPTH_COMPONENT16,  GL2ES2.GL_DEPTH_COMPONENT, GL.GL_UNSIGNED_SHORT, false);
        setFormat(Format.Depth24,  GL.GL_DEPTH_COMPONENT24,  GL2ES2.GL_DEPTH_COMPONENT, GL.GL_UNSIGNED_INT, false);
        setFormat(Format.Depth32,  GL.GL_DEPTH_COMPONENT32,  GL2ES2.GL_DEPTH_COMPONENT, GL.GL_UNSIGNED_INT, false);
        setFormat(Format.Depth32F, GL2GL3.GL_DEPTH_COMPONENT32F, GL2ES2.GL_DEPTH_COMPONENT, GL.GL_FLOAT,         false);
        
        // Depth stencil formats
        setFormat(Format.Depth24Stencil8, GL.GL_DEPTH24_STENCIL8, GL.GL_DEPTH_STENCIL, GL.GL_UNSIGNED_INT_24_8, false);
        
        // RGB formats
        setFormat(Format.BGR8,       GL.GL_RGB8,  GL2GL3.GL_BGR, GL.GL_UNSIGNED_BYTE, false);
        setFormat(Format.RGB8,       GL.GL_RGB8,  GL.GL_RGB,        GL.GL_UNSIGNED_BYTE, false);
//        setFormat(Format.RGB10,      GL11.GL_RGB10, GL11.GL_RGB,        GL12.GL_UNSIGNED_INT_10_10_10_2, false); 
        setFormat(Format.RGB16,      GL2GL3.GL_RGB16, GL.GL_RGB,        GL.GL_UNSIGNED_SHORT, false); 
        setFormat(Format.RGB16F,     GL2ES2.GL_RGB16F, GL.GL_RGB, GL.GL_HALF_FLOAT, false);
        setFormat(Format.RGB32F,     GL.GL_RGB32F, GL.GL_RGB, GL.GL_FLOAT, false);
        
        // Special RGB formats
        setFormat(Format.RGB111110F, GL.GL_R11F_G11F_B10F,    GL.GL_RGB, GL.GL_UNSIGNED_INT_10F_11F_11F_REV, false);
        setFormat(Format.RGB9E5,     GL2GL3.GL_RGB9_E5, GL.GL_RGB, GL2GL3.GL_UNSIGNED_INT_5_9_9_9_REV, false);
        setFormat(Format.RGB16F_to_RGB111110F, GL.GL_R11F_G11F_B10F,    GL.GL_RGB, GL.GL_HALF_FLOAT, false);
        setFormat(Format.RGB16F_to_RGB9E5, GL2.GL_RGB9_E5, GL.GL_RGB, GL.GL_HALF_FLOAT, false);
        
        // RGBA formats
        setFormat(Format.ABGR8,   GL.GL_RGBA8,       GL2.GL_ABGR_EXT, GL.GL_UNSIGNED_BYTE, false);
        setFormat(Format.RGB5A1,    GL.GL_RGB5_A1,   GL.GL_RGBA,        GL.GL_UNSIGNED_SHORT_5_5_5_1, false);
        setFormat(Format.ARGB4444,  GL.GL_RGBA4,     GL2.GL_ABGR_EXT, GL.GL_UNSIGNED_SHORT_4_4_4_4, false);
        setFormat(Format.RGBA8,   GL.GL_RGBA8,       GL.GL_RGBA,        GL.GL_UNSIGNED_BYTE, false);
        setFormat(Format.RGBA16,  GL2GL3.GL_RGBA16,      GL.GL_RGBA,        GL.GL_UNSIGNED_SHORT, false); // might be incorrect
        setFormat(Format.RGBA16F, GL2ES2.GL_RGBA16F, GL.GL_RGBA, GL.GL_HALF_FLOAT, false);
        setFormat(Format.RGBA32F, GL.GL_RGBA32F, GL.GL_RGBA, GL.GL_FLOAT, false);
        
        // DXT formats
        setFormat(Format.DXT1,  GL.GL_COMPRESSED_RGB_S3TC_DXT1_EXT, GL.GL_RGB,   GL.GL_UNSIGNED_BYTE, true);
        setFormat(Format.DXT1A, GL.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, true);
        setFormat(Format.DXT3,  GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, true);
        setFormat(Format.DXT5,  GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, true);
    
        // LTC/LATC/3Dc formats
        setFormat(Format.LTC,  GL2.GL_COMPRESSED_LUMINANCE_LATC1_EXT,       GL.GL_LUMINANCE,       GL.GL_UNSIGNED_BYTE, true);
        setFormat(Format.LATC, GL2.GL_COMPRESSED_LUMINANCE_ALPHA_LATC2_EXT, GL.GL_LUMINANCE_ALPHA, GL.GL_UNSIGNED_BYTE, true);
    }
    
    public static GLImageFormat getImageFormat(Format fmt){
        GL gl = GLContext.getCurrentGL();
        switch (fmt){
            case ABGR8:
                if (!gl.isExtensionAvailable("GL_EXT_abgr") && !abgrToRgbaConversionEnabled) {
                    setFormat(Format.ABGR8,   GL.GL_RGBA,        GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, false);
                    abgrToRgbaConversionEnabled = true;
                }
                break;
            case BGR8:
                if (!gl.isExtensionAvailable("GL_VERSION_1_2") && !gl.isExtensionAvailable("EXT_bgra")){
                    return null;
                }
                break;
            case DXT1:
            case DXT1A:
            case DXT3:
            case DXT5:
                if (!gl.isExtensionAvailable("GL_EXT_texture_compression_s3tc")) {
                    return null;
                }
                break;
            case Depth:
            case Depth16:
            case Depth24:
            case Depth32:
                if (!gl.isExtensionAvailable("GL_VERSION_1_4") && !gl.isExtensionAvailable("ARB_depth_texture")){
                    return null;
                }
                break;
            case Depth24Stencil8:
                if (!gl.isExtensionAvailable("GL_VERSION_3_0")){
                    return null;
                }
                break;
            case Luminance16F:
            case Luminance16FAlpha16F:
            case Luminance32F:
                if (!gl.isExtensionAvailable("GL_ARB_texture_float")){
                    return null;
                }
                break;
            case RGB16F:
            case RGB32F:
            case RGBA16F:
            case RGBA32F:
                if (!gl.isExtensionAvailable("GL_VERSION_3_0") && !gl.isExtensionAvailable("GL_ARB_texture_float")){
                    return null;
                }
                break;
            case Depth32F:
                if (!gl.isExtensionAvailable("GL_VERSION_3_0") && !gl.isExtensionAvailable("GL_NV_depth_buffer_float")){
                    return null;
                }
                break;
            case LATC:
            case LTC:
                if (!gl.isExtensionAvailable("GL_EXT_texture_compression_latc")){
                    return null;
                }
                break;
            case RGB9E5:
            case RGB16F_to_RGB9E5:
                if (!gl.isExtensionAvailable("GL_VERSION_3_0") && !gl.isExtensionAvailable("GL_EXT_texture_shared_exponent")){
                    return null;
                }
                break;
            case RGB111110F:
            case RGB16F_to_RGB111110F:
                if (!gl.isExtensionAvailable("GL_VERSION_3_0") && !gl.isExtensionAvailable("GL_EXT_packed_float")){
                    return null;
                }
                break;
        }
        return formatToGL[fmt.ordinal()];
    }
    
    public static GLImageFormat getImageFormatWithError(Format fmt) {
        GLImageFormat glFmt = getImageFormat(fmt);
        if (glFmt == null) {
            throw new RendererException("Image format '" + fmt + "' is unsupported by the video hardware.");
        }
        return glFmt;
    }

    public static void uploadTexture(Image image,
                                     int target,
                                     int index,
                                     int border){
        GL gl = GLContext.getCurrentGL();
        Image.Format fmt = image.getFormat();
        GLImageFormat glFmt = getImageFormatWithError(fmt);

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
            if (abgrToRgbaConversionEnabled) {
                convertABGRtoRGBA(data);
            }
            gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
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
                if (target == GL2ES2.GL_TEXTURE_3D){
                    gl.getGL2ES2().glCompressedTexImage3D(target,
                                                i,
                                                glFmt.internalFormat,
                                                mipWidth,
                                                mipHeight,
                                                mipDepth,
                                                0,
                                                border,
                                                data);
                }else{
                    //all other targets use 2D: array, cubemap, 2d
                    gl.glCompressedTexImage2D(target,
                                                i,
                                                glFmt.internalFormat,
                                                mipWidth,
                                                mipHeight,
                                                0,
                                                border,
                                                data);
                }
            }else{
                if (target == GL2ES2.GL_TEXTURE_3D){
                    gl.getGL2ES2().glTexImage3D(target,
                                      i,
                                      glFmt.internalFormat,
                                      mipWidth,
                                      mipHeight,
                                      mipDepth,
                                      border,
                                      glFmt.format,
                                      glFmt.dataType,
                                      data);
                }else if (target == GL.GL_TEXTURE_2D_ARRAY){
                    // prepare data for 2D array
                    // or upload slice
                    if (index == -1){
                        gl.getGL2ES2().glTexImage3D(target,
                                          0,
                                          glFmt.internalFormat,
                                          mipWidth,
                                          mipHeight,
                                          image.getData().size(), //# of slices
                                          border,
                                          glFmt.format,
                                          glFmt.dataType,
                                          data);
                    }else{
                        gl.getGL2ES2().glTexSubImage3D(target,
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

                        gl.glTexSubImage2D(target,
                                             i,
                                             0, 0,
                                             mipWidth, mipHeight,
                                             glFmt.format,
                                             glFmt.dataType,
                                             data);
                    }else{
                        if (samples > 1){
                            if (gl.isGL2GL3()) {
                                gl.getGL2GL3().glTexImage2DMultisample(target,
                                        samples,
                                        glFmt.internalFormat,
                                        mipWidth,
                                        mipHeight,
                                        true);
                            }
                        } else {
                            gl.glTexImage2D(target,
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
    
    private static void convertABGRtoRGBA(ByteBuffer buffer) {

        for (int i = 0; i < buffer.capacity(); i++) {

            int a = buffer.get(i++);
            int b = buffer.get(i++);
            int g = buffer.get(i++);
            int r = buffer.get(i);

            buffer.put(i - 3, (byte) r);
            buffer.put(i - 2, (byte) g);
            buffer.put(i - 1, (byte) b);
            buffer.put(i, (byte) a);

        }

    }
}
