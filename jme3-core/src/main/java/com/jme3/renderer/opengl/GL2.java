/*
 * Copyright (c) 2009-2014 jMonkeyEngine
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

import java.nio.ByteBuffer;

/**
 * GL functions only available on vanilla desktop OpenGL 2.
 * 
 * @author Kirill Vainer
 */
public interface GL2 extends GL {
    
    public static final int GL_ALPHA8 = 0x803C;
    public static final int GL_ALPHA_TEST = 0xBC0;
    public static final int GL_BGR = 0x80E0;
    public static final int GL_BGRA = 0x80E1;
    public static final int GL_COMPARE_REF_TO_TEXTURE = 0x884E;
    public static final int GL_DEPTH_COMPONENT24 = 0x81A6;
    public static final int GL_DEPTH_COMPONENT32 = 0x81A7;
    public static final int GL_DEPTH_TEXTURE_MODE = 0x884B;
    public static final int GL_DOUBLEBUFFER = 0xC32;
    public static final int GL_DRAW_BUFFER = 0xC01;
    public static final int GL_FILL = 0x1B02;
    public static final int GL_GENERATE_MIPMAP = 0x8191;
    public static final int GL_INTENSITY = 0x8049;
    public static final int GL_LINE = 0x1B01;
    public static final int GL_LUMINANCE8 = 0x8040;
    public static final int GL_LUMINANCE8_ALPHA8 = 0x8045;
    public static final int GL_MAX_ELEMENTS_INDICES = 0x80E9;
    public static final int GL_MAX_ELEMENTS_VERTICES = 0x80E8;
    public static final int GL_MAX_FRAGMENT_UNIFORM_COMPONENTS = 0x8B49;
    public static final int GL_MAX_VERTEX_UNIFORM_COMPONENTS = 0x8B4A;
    public static final int GL_READ_BUFFER = 0xC02;
    public static final int GL_RGB8 = 0x8051;
    public static final int GL_STACK_OVERFLOW = 0x503;
    public static final int GL_STACK_UNDERFLOW = 0x504;
    public static final int GL_TEXTURE_3D = 0x806F;
    public static final int GL_POINT_SPRITE = 0x8861;
    public static final int GL_TEXTURE_COMPARE_FUNC = 0x884D;
    public static final int GL_TEXTURE_COMPARE_MODE = 0x884C;
    public static final int GL_TEXTURE_WRAP_R = 0x8072;
    public static final int GL_VERTEX_PROGRAM_POINT_SIZE = 0x8642;
    public static final int GL_UNSIGNED_INT_8_8_8_8 = 0x8035;
    
    public void glAlphaFunc(int func, float ref);
    public void glPointSize(float size);
    public void glPolygonMode(int face, int mode);
    public void glDrawBuffer(int mode);
    public void glReadBuffer(int mode);
    public void glCompressedTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, ByteBuffer data);
    public void glCompressedTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, ByteBuffer data);
    public void glTexImage3D(int target, int level, int internalFormat, int width, int height, int depth, int border, int format, int type, ByteBuffer data);
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, ByteBuffer data);
}
