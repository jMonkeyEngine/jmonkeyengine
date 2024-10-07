/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
    public static final int GL_POINT = 0x1B00;
    public static final int GL_LINE = 0x1B01;
    public static final int GL_FILL = 0x1B02;
    public static final int GL_GENERATE_MIPMAP = 0x8191;
    public static final int GL_INTENSITY = 0x8049;
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
    public static final int GL_TEXTURE_BASE_LEVEL = 0x813C;
    public static final int GL_TEXTURE_MAX_LEVEL = 0x813D;
    public static final int GL_POINT_SPRITE = 0x8861;
    public static final int GL_TEXTURE_COMPARE_FUNC = 0x884D;
    public static final int GL_TEXTURE_COMPARE_MODE = 0x884C;
    public static final int GL_TEXTURE_WRAP_R = 0x8072;
    public static final int GL_VERTEX_PROGRAM_POINT_SIZE = 0x8642;
    public static final int GL_UNSIGNED_INT_8_8_8_8 = 0x8035;

    /**
     * <p><a target="_blank" href="http://docs.gl/gl3/glAlphaFunc">Reference Page</a> - <em>This function is deprecated and unavailable in the Core profile</em></p>
     *
     * The alpha test discards a fragment conditionally based on the outcome of a comparison between the incoming fragment’s alpha value and a constant value.
     * The comparison is enabled or disabled with the generic {@link #glEnable Enable} and {@link #glDisable Disable} commands using the symbolic constant {@link #GL_ALPHA_TEST ALPHA_TEST}.
     * When disabled, it is as if the comparison always passes. The test is controlled with this method.
     *
     * @param func a symbolic constant indicating the alpha test function. One of:
     *  {@link #GL_NEVER NEVER}
     *  {@link #GL_ALWAYS ALWAYS}
     *  {@link #GL_LESS LESS}
     *  {@link #GL_LEQUAL LEQUAL}
     *  {@link #GL_EQUAL EQUAL}
     *  {@link #GL_GEQUAL GEQUAL}
     *  {@link #GL_GREATER GREATER}
     *  {@link #GL_NOTEQUAL NOTEQUAL}
     * @param ref  a reference value clamped to the range [0, 1]. When performing the alpha test, the GL will convert the reference value to the same representation as the fragment's alpha value (floating-point or fixed-point).
     */
    public void glAlphaFunc(int func, float ref);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glPointSize">Reference Page</a></p>
     * <p>
     * Controls the rasterization of points if no vertex, tessellation control, tessellation evaluation, or geometry shader is active. The default point size is 1.0.
     *
     * @param size the request size of a point.
     */
    public void glPointSize(float size);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glPolygonMode">Reference Page</a></p>
     *
     * Controls the interpretation of polygons for rasterization.
     *
     * <p>{@link #GL_FILL FILL} is the default mode of polygon rasterization. Note that these modes affect only the final rasterization of polygons: in particular, a
     * polygon's vertices are lit, and the polygon is clipped and possibly culled before these modes are applied. Polygon antialiasing applies only to the
     * {@link #GL_FILL FILL} state of PolygonMode. For {@link #GL_POINT POINT} or {@link #GL_LINE LINE}, point antialiasing or line segment antialiasing, respectively, apply.</p>
     *
     * @param face the face for which to set the rasterizing method. One of:
     *  {@link #GL_FRONT FRONT}
     *  {@link #GL_BACK BACK}
     *  {@link #GL_FRONT_AND_BACK FRONT_AND_BACK}
     * @param mode the rasterization mode. One of:
     *  {@link #GL_POINT POINT}
     *  {@link #GL_LINE LINE}
     *  {@link #GL_FILL FILL}
     */
    public void glPolygonMode(int face, int mode);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glDrawBuffer">Reference Page</a></p>
     * <p>
     * Defines the color buffer to which fragment color zero is written.
     *
     * @param mode the color buffer to draw to.
     */
    public void glDrawBuffer(int mode);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glReadBuffer">Reference Page</a></p>
     * <p>
     * Defines the color buffer from which values are obtained.
     *
     * @param mode the color buffer to read from.
     */
    public void glReadBuffer(int mode);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glCompressedTexImage3D">Reference Page</a></p>
     * <p>
     * Specifies a three-dimensional texture image in a compressed format.
     *
     * @param target         the target texture.
     * @param level          the level-of-detail number. Level 0 is the base image level. Level n is the nth mipmap reduction image.
     * @param internalFormat the format of the compressed image data.
     * @param width          the width of the texture image
     * @param height         the height of the texture image
     * @param depth          the depth of the texture image
     * @param border         must be 0
     * @param data           a pointer to the compressed image data
     */
    public void glCompressedTexImage3D(int target, int level, int internalFormat, int width, int height, int depth,
                                       int border, ByteBuffer data);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glCompressedTexSubImage3D">Reference Page</a></p>
     * <p>
     * Respecifies only a cubic subregion of an existing 3D texel array, with incoming data stored in a specific compressed image format.
     *
     * @param target  the target texture.
     * @param level   the level-of-detail number. Level 0 is the base image level. Level n is the nth mipmap reduction image.
     * @param xoffset a texel offset in the x direction within the texture array.
     * @param yoffset a texel offset in the y direction within the texture array.
     * @param zoffset a texel offset in the z direction within the texture array.
     * @param width   the width of the texture subimage.
     * @param height  the height of the texture subimage.
     * @param depth   the depth of the texture subimage.
     * @param format  the format of the compressed image data stored at address {@code data}.
     * @param data    a pointer to the compressed image data.
     */
    public void glCompressedTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width,
                                          int height, int depth, int format, ByteBuffer data);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glTexImage3D">Reference Page</a></p>
     * <p>
     * Specifies a three-dimensional texture image.
     *
     * @param target         the texture target.
     * @param level          the level-of-detail number.
     * @param internalFormat the texture internal format.
     * @param width          the texture width.
     * @param height         the texture height.
     * @param depth          the texture depth.
     * @param border         the texture border width.
     * @param format         the texel data format.
     * @param type           the texel data type.
     * @param data           the texel data.
     */
    public void glTexImage3D(int target, int level, int internalFormat, int width, int height, int depth, int border,
                             int format, int type, ByteBuffer data);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glTexSubImage3D">Reference Page</a></p>
     * <p>
     * Respecifies a cubic subregion of an existing 3D texel array. No change is made to the internalformat, width, height, depth, or border parameters of
     * the specified texel array, nor is any change made to texel values outside the specified subregion.
     *
     * @param target  the texture target.
     * @param level   the level-of-detail-number.
     * @param xoffset the x coordinate of the texel subregion.
     * @param yoffset the y coordinate of the texel subregion.
     * @param zoffset the z coordinate of the texel subregion.
     * @param width   the subregion width.
     * @param height  the subregion height.
     * @param depth   the subregion depth.
     * @param format  the pixel data format.
     * @param type    the pixel data type.
     * @param data    the pixel data.
     */
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height,
                                int depth, int format, int type, ByteBuffer data);
}
