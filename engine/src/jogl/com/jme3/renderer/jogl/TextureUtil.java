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

package com.jme3.renderer.jogl;

import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import java.nio.ByteBuffer;
import javax.media.opengl.GL;

public class TextureUtil {

    public static int convertTextureFormat(Format fmt){
        switch (fmt){
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
            default:
                throw new UnsupportedOperationException("Unrecognized format: "+fmt);
        }
    }

    public static void uploadTexture(GL gl,
                                     Image img,
                                     int index,
                                     boolean generateMips,
                                     boolean powerOf2){

        Image.Format fmt = img.getFormat();
        ByteBuffer data;
        if (index >= 0 || img.getData() != null && img.getData().size() > 0){
            data = img.getData(index);
        }else{
            data = null;
        }

        int width = img.getWidth();
        int height = img.getHeight();
//        int depth = img.getDepth();

        boolean compress = false;
        int format = -1;
        int internalFormat = -1;
        int dataType = -1;

        switch (fmt){
            case Alpha16:
                format = gl.GL_ALPHA;
                dataType = gl.GL_UNSIGNED_BYTE;
                internalFormat = gl.GL_ALPHA16;
                break;
            case Alpha8:
                format = gl.GL_ALPHA;
                dataType = gl.GL_UNSIGNED_BYTE;
                internalFormat = gl.GL_ALPHA8;
                break;
            case Luminance8:
                format = gl.GL_LUMINANCE;
                dataType = gl.GL_UNSIGNED_BYTE;
                internalFormat = gl.GL_LUMINANCE8;
                break;
            case Luminance8Alpha8:
                format = gl.GL_LUMINANCE_ALPHA;
                dataType = gl.GL_UNSIGNED_BYTE;
                internalFormat = gl.GL_LUMINANCE8_ALPHA8;
                break;
            case Luminance16Alpha16:
                format = gl.GL_LUMINANCE_ALPHA;
                dataType = gl.GL_UNSIGNED_BYTE;
                internalFormat = gl.GL_LUMINANCE16_ALPHA16;
                break;
            case Luminance16:
                format = gl.GL_LUMINANCE;
                dataType = gl.GL_UNSIGNED_BYTE;
                internalFormat = gl.GL_LUMINANCE16;
                break;
            case RGB565:
                format = gl.GL_RGB;
                dataType = gl.GL_UNSIGNED_SHORT_5_6_5;
                internalFormat = gl.GL_RGB8;
                break;
            case ARGB4444:
                format = gl.GL_RGBA;
                dataType = gl.GL_UNSIGNED_SHORT_4_4_4_4;
                internalFormat = gl.GL_RGBA4;
                break;
            case RGB10:
                format = gl.GL_RGB;
                dataType = gl.GL_UNSIGNED_BYTE;
                internalFormat = gl.GL_RGB10;
                break;
            case RGB16:
                format = gl.GL_RGB;
                dataType = gl.GL_UNSIGNED_BYTE;
                internalFormat = gl.GL_RGB16;
                break;
            case RGB5A1:
                format = gl.GL_RGBA;
                dataType = gl.GL_UNSIGNED_SHORT_5_5_5_1;
                internalFormat = gl.GL_RGB5_A1;
                break;
            case RGB8:
                format = gl.GL_RGB;
                dataType = gl.GL_UNSIGNED_BYTE;
                internalFormat = gl.GL_RGB8;
                break;
            case BGR8:
                format = gl.GL_BGR;
                dataType = gl.GL_UNSIGNED_BYTE;
                internalFormat = gl.GL_RGB8;
                break;
            case RGBA16:
                format = gl.GL_RGBA;
                dataType = gl.GL_UNSIGNED_BYTE;
                internalFormat = gl.GL_RGBA16;
                break;
            case RGBA8:
                format = gl.GL_RGBA;
                dataType = gl.GL_UNSIGNED_BYTE;
                internalFormat = gl.GL_RGBA8;
                break;
            case DXT1:
                compress = true;
                internalFormat = gl.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
                format = gl.GL_RGB;
                dataType = gl.GL_UNSIGNED_BYTE;
                break;
            case DXT1A:
                compress = true;
                internalFormat = gl.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
                format = gl.GL_RGBA;
                dataType = gl.GL_UNSIGNED_BYTE;
                break;
            case DXT3:
                compress = true;
                internalFormat = gl.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
                format = gl.GL_RGBA;
                dataType = gl.GL_UNSIGNED_BYTE;
                break;
            case DXT5:
                compress = true;
                internalFormat = gl.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
                format = gl.GL_RGBA;
                dataType = gl.GL_UNSIGNED_BYTE;
                break;
            default:
                throw new UnsupportedOperationException("Unrecognized format: "+fmt);
        }

        if (data != null)
            gl.glPixelStorei(gl.GL_UNPACK_ALIGNMENT, 1);

        int[] mipSizes = img.getMipMapSizes();
        int pos = 0;
        if (mipSizes == null){
            if (data != null)
                mipSizes = new int[]{ data.capacity() };
            else
                mipSizes = new int[]{ width * height * fmt.getBitsPerPixel() / 8 };
        }

        for (int i = 0; i < mipSizes.length; i++){
            int mipWidth =  Math.max(1, width  >> i);
            int mipHeight = Math.max(1, height >> i);
//            int mipDepth =  Math.max(1, depth  >> i);

            if (data != null){
                data.position(pos);
                data.limit(pos + mipSizes[i]);
            }

            if (compress && data != null){
                gl.glCompressedTexImage2D(gl.GL_TEXTURE_2D,
                                          i,
                                          internalFormat,
                                          mipWidth,
                                          mipHeight,
                                          0,
                                          data.remaining(),
                                          data);
            }else{
                gl.glTexImage2D(gl.GL_TEXTURE_2D,
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
