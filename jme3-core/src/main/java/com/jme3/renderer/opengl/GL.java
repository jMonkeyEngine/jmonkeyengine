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
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Baseline GL methods that must be available on all platforms.
 * <p>
 * This is the subset of vanilla desktop OpenGL 2 and OpenGL ES 2.
 *
 * @author Kirill Vainer
 */
public interface GL {

    public static final int GL_ALIASED_LINE_WIDTH_RANGE = 0x846E;
    public static final int GL_ALPHA = 0x1906;
    public static final int GL_ALWAYS = 0x207;
    public static final int GL_ARRAY_BUFFER = 0x8892;
    public static final int GL_BACK = 0x405;
    public static final int GL_BLEND = 0xBE2;
    public static final int GL_BLUE = 0x1905;
    public static final int GL_BYTE = 0x1400;
    public static final int GL_CLAMP_TO_EDGE = 0x812F;
    public static final int GL_COLOR_BUFFER_BIT = 0x4000;
    public static final int GL_COMPILE_STATUS = 0x8B81;
    public static final int GL_CULL_FACE = 0xB44;
    public static final int GL_DECR = 0x1E03;
    public static final int GL_DECR_WRAP = 0x8508;
    public static final int GL_DEPTH_BUFFER_BIT = 0x100;
    public static final int GL_DEPTH_COMPONENT = 0x1902;
    public static final int GL_DEPTH_COMPONENT16 = 0x81A5;
    public static final int GL_DEPTH_TEST = 0xB71;
    public static final int GL_DOUBLE = 0x140A;
    public static final int GL_DST_ALPHA = 0x0304;
    public static final int GL_DST_COLOR = 0x306;
    public static final int GL_DYNAMIC_DRAW = 0x88E8;
    public static final int GL_DYNAMIC_COPY = 0x88EA;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    public static final int GL_EQUAL = 0x202;
    public static final int GL_EXTENSIONS = 0x1F03;
    public static final int GL_FALSE = 0x0;
    public static final int GL_FLOAT = 0x1406;
    public static final int GL_FRAGMENT_SHADER = 0x8B30;
    public static final int GL_FRONT = 0x404;
    public static final int GL_FUNC_ADD = 0x8006;
    public static final int GL_FUNC_SUBTRACT = 0x800A;
    public static final int GL_FUNC_REVERSE_SUBTRACT = 0x800B;
    public static final int GL_FRONT_AND_BACK = 0x408;
    public static final int GL_GEQUAL = 0x206;
    public static final int GL_GREATER = 0x204;
    public static final int GL_GREEN = 0x1904;
    public static final int GL_INCR = 0x1E02;
    public static final int GL_INCR_WRAP = 0x8507;
    public static final int GL_INFO_LOG_LENGTH = 0x8B84;
    public static final int GL_INT = 0x1404;
    public static final int GL_INVALID_ENUM = 0x500;
    public static final int GL_INVALID_VALUE = 0x501;
    public static final int GL_INVALID_OPERATION = 0x502;
    public static final int GL_INVERT = 0x150A;
    public static final int GL_KEEP = 0x1E00;
    public static final int GL_LEQUAL = 0x203;
    public static final int GL_LESS = 0x201;
    public static final int GL_LINEAR = 0x2601;
    public static final int GL_LINEAR_MIPMAP_LINEAR = 0x2703;
    public static final int GL_LINEAR_MIPMAP_NEAREST = 0x2701;
    public static final int GL_LINES = 0x1;
    public static final int GL_LINE_LOOP = 0x2;
    public static final int GL_LINE_SMOOTH = 0xB20;
    public static final int GL_LINE_STRIP = 0x3;
    public static final int GL_LINK_STATUS = 0x8B82;
    public static final int GL_LUMINANCE = 0x1909;
    public static final int GL_LUMINANCE_ALPHA = 0x190A;
    public static final int GL_MAX = 0x8008;
    public static final int GL_MAX_CUBE_MAP_TEXTURE_SIZE = 0x851C;
    public static final int GL_MAX_FRAGMENT_UNIFORM_COMPONENTS = 0x8B49;
    public static final int GL_MAX_FRAGMENT_UNIFORM_VECTORS = 0x8DFD;
    public static final int GL_MAX_TEXTURE_IMAGE_UNITS = 0x8872;
    public static final int GL_MAX_TEXTURE_SIZE = 0xD33;
    public static final int GL_MAX_VERTEX_ATTRIBS = 0x8869;
    public static final int GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS = 0x8B4C;
    public static final int GL_MAX_VERTEX_UNIFORM_COMPONENTS = 0x8B4A;
    public static final int GL_MAX_VERTEX_UNIFORM_VECTORS = 0x8DFB;
    public static final int GL_MIRRORED_REPEAT = 0x8370;
    public static final int GL_MIN = 0x8007;
    public static final int GL_NEAREST = 0x2600;
    public static final int GL_NEAREST_MIPMAP_LINEAR = 0x2702;
    public static final int GL_NEAREST_MIPMAP_NEAREST = 0x2700;
    public static final int GL_NEVER = 0x200;
    public static final int GL_NO_ERROR = 0x0;
    public static final int GL_NONE = 0x0;
    public static final int GL_NOTEQUAL = 0x205;
    public static final int GL_ONE = 0x1;
    public static final int GL_ONE_MINUS_DST_ALPHA = 0x0305;
    public static final int GL_ONE_MINUS_DST_COLOR = 0x307;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 0x303;
    public static final int GL_ONE_MINUS_SRC_COLOR = 0x301;
    public static final int GL_OUT_OF_MEMORY = 0x505;
    public static final int GL_POINTS = 0x0;
    public static final int GL_POLYGON_OFFSET_FILL = 0x8037;
    public static final int GL_QUERY_RESULT = 0x8866;
    public static final int GL_QUERY_RESULT_AVAILABLE = 0x8867;
    public static final int GL_RED = 0x1903;
    public static final int GL_RENDERER = 0x1F01;
    public static final int GL_REPEAT = 0x2901;
    public static final int GL_REPLACE = 0x1E01;
    public static final int GL_RGB = 0x1907;
    public static final int GL_RGB565 = 0x8D62;
    public static final int GL_RGB5_A1 = 0x8057;
    public static final int GL_RGBA = 0x1908;
    public static final int GL_RGBA4 = 0x8056;
    public static final int GL_SCISSOR_TEST = 0xC11;
    public static final int GL_SHADING_LANGUAGE_VERSION = 0x8B8C;
    public static final int GL_SHORT = 0x1402;
    public static final int GL_SRC_ALPHA = 0x302;
    public static final int GL_SRC_ALPHA_SATURATE = 0x0308;
    public static final int GL_SRC_COLOR = 0x300;
    public static final int GL_STATIC_DRAW = 0x88E4;
    public static final int GL_STENCIL_BUFFER_BIT = 0x400;
    public static final int GL_STENCIL_TEST = 0xB90;
    public static final int GL_STREAM_DRAW = 0x88E0;
    public static final int GL_STREAM_READ = 0x88E1;
    public static final int GL_TEXTURE = 0x1702;
    public static final int GL_TEXTURE0 = 0x84C0;
    public static final int GL_TEXTURE1 = 0x84C1;
    public static final int GL_TEXTURE2 = 0x84C2;
    public static final int GL_TEXTURE3 = 0x84C3;
    public static final int GL_TEXTURE4 = 0x84C4;
    public static final int GL_TEXTURE5 = 0x84C5;
    public static final int GL_TEXTURE6 = 0x84C6;
    public static final int GL_TEXTURE7 = 0x84C7;
    public static final int GL_TEXTURE8 = 0x84C8;
    public static final int GL_TEXTURE9 = 0x84C9;
    public static final int GL_TEXTURE10 = 0x84CA;
    public static final int GL_TEXTURE11 = 0x84CB;
    public static final int GL_TEXTURE12 = 0x84CC;
    public static final int GL_TEXTURE13 = 0x84CD;
    public static final int GL_TEXTURE14 = 0x84CE;
    public static final int GL_TEXTURE15 = 0x84CF;
    public static final int GL_TEXTURE_2D = 0xDE1;
    public static final int GL_TEXTURE_CUBE_MAP = 0x8513;
    public static final int GL_TEXTURE_CUBE_MAP_POSITIVE_X = 0x8515;
    public static final int GL_TEXTURE_CUBE_MAP_NEGATIVE_X = 0x8516;
    public static final int GL_TEXTURE_CUBE_MAP_POSITIVE_Y = 0x8517;
    public static final int GL_TEXTURE_CUBE_MAP_NEGATIVE_Y = 0x8518;
    public static final int GL_TEXTURE_CUBE_MAP_POSITIVE_Z = 0x8519;
    public static final int GL_TEXTURE_CUBE_MAP_NEGATIVE_Z = 0x851A;
    public static final int GL_TEXTURE_MAG_FILTER = 0x2800;
    public static final int GL_TEXTURE_MIN_FILTER = 0x2801;
    public static final int GL_TEXTURE_WRAP_S = 0x2802;
    public static final int GL_TEXTURE_WRAP_T = 0x2803;
    public static final int GL_TIME_ELAPSED = 0x88BF;
    public static final int GL_TRIANGLES = 0x4;
    public static final int GL_TRIANGLE_FAN = 0x6;
    public static final int GL_TRIANGLE_STRIP = 0x5;
    public static final int GL_TRUE = 0x1;
    public static final int GL_UNPACK_ALIGNMENT = 0xCF5;
    public static final int GL_UNSIGNED_BYTE = 0x1401;
    public static final int GL_UNSIGNED_INT = 0x1405;
    public static final int GL_UNSIGNED_SHORT = 0x1403;
    public static final int GL_UNSIGNED_SHORT_5_6_5 = 0x8363;
    public static final int GL_UNSIGNED_SHORT_5_5_5_1 = 0x8034;
    public static final int GL_VENDOR = 0x1F00;
    public static final int GL_VERSION = 0x1F02;
    public static final int GL_VERTEX_SHADER = 0x8B31;
    public static final int GL_ZERO = 0x0;
    public static final int GL_UNPACK_ROW_LENGTH = 0x0CF2;
    public static final int GL_FRAMEBUFFER_BINDING = 0x8CA6;

    public void resetStats();

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glActiveTexture">Reference Page</a></p>
     * <p>
     * Selects which texture unit subsequent texture state calls will affect. The number of texture units an implementation supports is implementation
     * dependent.
     *
     * @param texture which texture unit to make active. One of:
     *  {@link #GL_TEXTURE0 TEXTURE0}
     *  GL_TEXTURE[1-31]
     */
    public void glActiveTexture(int texture);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glAttachShader">Reference Page</a></p>
     * <p>
     * Attaches a shader object to a program object.
     *
     * <p>In order to create a complete shader program, there must be a way to specify the list of things that will be linked together. Program objects provide
     * this mechanism. Shaders that are to be linked together in a program object must first be attached to that program object. glAttachShader attaches the
     * shader object specified by shader to the program object specified by program. This indicates that shader will be included in link operations that will
     * be performed on program.</p>
     *
     * <p>All operations that can be performed on a shader object are valid whether or not the shader object is attached to a program object. It is permissible to
     * attach a shader object to a program object before source code has been loaded into the shader object or before the shader object has been compiled. It
     * is permissible to attach multiple shader objects of the same type because each may contain a portion of the complete shader. It is also permissible to
     * attach a shader object to more than one program object. If a shader object is deleted while it is attached to a program object, it will be flagged for
     * deletion, and deletion will not occur until glDetachShader is called to detach it from all program objects to which it is attached.</p>
     *
     * @param program the program object to which a shader object will be attached.
     * @param shader  the shader object that is to be attached.
     */
    public void glAttachShader(int program, int shader);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glBeginQuery">Reference Page</a></p>
     * <p>
     * Creates a query object and makes it active.
     *
     * @param target the target type of query object established.
     * @param query  the name of a query object.
     */
    public void glBeginQuery(int target, int query);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glBindBuffer">Reference Page</a></p>
     * <p>
     * Binds a named buffer object.
     *
     * @param target the target to which the buffer object is bound.
     * @param buffer the name of a buffer object.
     */
    public void glBindBuffer(int target, int buffer);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glBindTexture">Reference Page</a></p>
     * <p>
     * Binds the a texture to a texture target.
     *
     * <p>While a texture object is bound, GL operations on the target to which it is bound affect the bound object, and queries of the target to which it is
     * bound return state from the bound object. If texture mapping of the dimensionality of the target to which a texture object is bound is enabled, the
     * state of the bound texture object directs the texturing operation.</p>
     *
     * @param target  the texture target.
     * @param texture the texture object to bind.
     */
    public void glBindTexture(int target, int texture);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glBlendEquationSeparate">Reference Page</a></p>
     * <p>
     * Sets the RGB blend equation and the alpha blend equation separately.
     *
     * @param colorMode the RGB blend equation, how the red, green, and blue components of the source and destination colors are combined.
     * @param alphaMode the alpha blend equation, how the alpha component of the source and destination colors are combined
     */
    public void glBlendEquationSeparate(int colorMode, int alphaMode);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glBlendFunc">Reference Page</a></p>
     * <p>
     * Specifies the weighting factors used by the blend equation, for both RGB and alpha functions and for all draw buffers.
     *
     * @param sFactor the source weighting factor.
     * @param dFactor the destination weighting factor.
     */
    public void glBlendFunc(int sFactor, int dFactor);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glBlendFuncSeparate">Reference Page</a></p>
     * <p>
     * Specifies pixel arithmetic for RGB and alpha components separately.
     *
     * @param sFactorRGB   how the red, green, and blue blending factors are computed. The initial value is GL_ONE.
     * @param dFactorRGB   how the red, green, and blue destination blending factors are computed. The initial value is GL_ZERO.
     * @param sFactorAlpha how the alpha source blending factor is computed. The initial value is GL_ONE.
     * @param dFactorAlpha how the alpha destination blending factor is computed. The initial value is GL_ZERO.
     */
    public void glBlendFuncSeparate(int sFactorRGB, int dFactorRGB, int sFactorAlpha, int dFactorAlpha);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glBufferData">Reference Page</a></p>
     * <p>
     * Creates and initializes a buffer object's data store.
     *
     * <p>{@code usage} is a hint to the GL implementation as to how a buffer object's data store will be accessed. This enables the GL implementation to make
     * more intelligent decisions that may significantly impact buffer object performance. It does not, however, constrain the actual usage of the data store.
     * {@code usage} can be broken down into two parts: first, the frequency of access (modification and usage), and second, the nature of that access. The
     * frequency of access may be one of these:</p>
     *
     * <ul>
     * <li><em>STREAM</em> - The data store contents will be modified once and used at most a few times.</li>
     * <li><em>STATIC</em> - The data store contents will be modified once and used many times.</li>
     * <li><em>DYNAMIC</em> - The data store contents will be modified repeatedly and used many times.</li>
     * </ul>
     *
     * <p>The nature of access may be one of these:</p>
     *
     * <ul>
     * <li><em>DRAW</em> - The data store contents are modified by the application, and used as the source for GL drawing and image specification commands.</li>
     * <li><em>READ</em> - The data store contents are modified by reading data from the GL, and used to return that data when queried by the application.</li>
     * <li><em>COPY</em> - The data store contents are modified by reading data from the GL, and used as the source for GL drawing and image specification commands.</li>
     * </ul>
     *
     * @param target   the target buffer object.
     * @param dataSize the size in bytes of the buffer object's new data store
     * @param usage    the expected usage pattern of the data store.
     */
    public void glBufferData(int target, long dataSize, int usage);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glBufferData">Reference Page</a></p>
     * <p>
     * Creates and initializes a buffer object's data store.
     *
     * <p>{@code usage} is a hint to the GL implementation as to how a buffer object's data store will be accessed. This enables the GL implementation to make
     * more intelligent decisions that may significantly impact buffer object performance. It does not, however, constrain the actual usage of the data store.
     * {@code usage} can be broken down into two parts: first, the frequency of access (modification and usage), and second, the nature of that access. The
     * frequency of access may be one of these:</p>
     *
     * <ul>
     * <li><em>STREAM</em> - The data store contents will be modified once and used at most a few times.</li>
     * <li><em>STATIC</em> - The data store contents will be modified once and used many times.</li>
     * <li><em>DYNAMIC</em> - The data store contents will be modified repeatedly and used many times.</li>
     * </ul>
     *
     * <p>The nature of access may be one of these:</p>
     *
     * <ul>
     * <li><em>DRAW</em> - The data store contents are modified by the application, and used as the source for GL drawing and image specification commands.</li>
     * <li><em>READ</em> - The data store contents are modified by reading data from the GL, and used to return that data when queried by the application.</li>
     * <li><em>COPY</em> - The data store contents are modified by reading data from the GL, and used as the source for GL drawing and image specification commands.</li>
     * </ul>
     *
     * @param target the target buffer object.
     * @param data   a pointer to data that will be copied into the data store for initialization, or {@code NULL} if no data is to be copied.
     * @param usage  the expected usage pattern of the data store.
     */
    public void glBufferData(int target, FloatBuffer data, int usage);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glBufferData">Reference Page</a></p>
     * <p>
     * Creates and initializes a buffer object's data store.
     *
     * <p>{@code usage} is a hint to the GL implementation as to how a buffer object's data store will be accessed. This enables the GL implementation to make
     * more intelligent decisions that may significantly impact buffer object performance. It does not, however, constrain the actual usage of the data store.
     * {@code usage} can be broken down into two parts: first, the frequency of access (modification and usage), and second, the nature of that access. The
     * frequency of access may be one of these:</p>
     *
     * <ul>
     * <li><em>STREAM</em> - The data store contents will be modified once and used at most a few times.</li>
     * <li><em>STATIC</em> - The data store contents will be modified once and used many times.</li>
     * <li><em>DYNAMIC</em> - The data store contents will be modified repeatedly and used many times.</li>
     * </ul>
     *
     * <p>The nature of access may be one of these:</p>
     *
     * <ul>
     * <li><em>DRAW</em> - The data store contents are modified by the application, and used as the source for GL drawing and image specification commands.</li>
     * <li><em>READ</em> - The data store contents are modified by reading data from the GL, and used to return that data when queried by the application.</li>
     * <li><em>COPY</em> - The data store contents are modified by reading data from the GL, and used as the source for GL drawing and image specification commands.</li>
     * </ul>
     *
     * @param target the target buffer object.
     * @param data   a pointer to data that will be copied into the data store for initialization, or {@code NULL} if no data is to be copied
     * @param usage  the expected usage pattern of the data store.
     */
    public void glBufferData(int target, ShortBuffer data, int usage);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glBufferData">Reference Page</a></p>
     * <p>
     * Creates and initializes a buffer object's data store.
     *
     * <p>{@code usage} is a hint to the GL implementation as to how a buffer object's data store will be accessed. This enables the GL implementation to make
     * more intelligent decisions that may significantly impact buffer object performance. It does not, however, constrain the actual usage of the data store.
     * {@code usage} can be broken down into two parts: first, the frequency of access (modification and usage), and second, the nature of that access. The
     * frequency of access may be one of these:</p>
     *
     * <ul>
     * <li><em>STREAM</em> - The data store contents will be modified once and used at most a few times.</li>
     * <li><em>STATIC</em> - The data store contents will be modified once and used many times.</li>
     * <li><em>DYNAMIC</em> - The data store contents will be modified repeatedly and used many times.</li>
     * </ul>
     *
     * <p>The nature of access may be one of these:</p>
     *
     * <ul>
     * <li><em>DRAW</em> - The data store contents are modified by the application, and used as the source for GL drawing and image specification commands.</li>
     * <li><em>READ</em> - The data store contents are modified by reading data from the GL, and used to return that data when queried by the application.</li>
     * <li><em>COPY</em> - The data store contents are modified by reading data from the GL, and used as the source for GL drawing and image specification commands.</li>
     * </ul>
     *
     * @param target the target buffer object.
     * @param data   a pointer to data that will be copied into the data store for initialization, or {@code NULL} if no data is to be copied.
     * @param usage  the expected usage pattern of the data store.
     */
    public void glBufferData(int target, ByteBuffer data, int usage);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glBufferSubData">Reference Page</a></p>
     * <p>
     * Updates a subset of a buffer object's data store.
     *
     * @param target the target buffer object.
     * @param offset the offset into the buffer object's data store where data replacement will begin, measured in bytes.
     * @param data   a pointer to the new data that will be copied into the data store.
     */
    public void glBufferSubData(int target, long offset, FloatBuffer data);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glBufferSubData">Reference Page</a></p>
     * <p>
     * Updates a subset of a buffer object's data store.
     *
     * @param target the target buffer object.
     * @param offset the offset into the buffer object's data store where data replacement will begin, measured in bytes.
     * @param data   a pointer to the new data that will be copied into the data store.
     */
    public void glBufferSubData(int target, long offset, ShortBuffer data);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glBufferSubData">Reference Page</a></p>
     * <p>
     * Updates a subset of a buffer object's data store.
     *
     * @param target the target buffer object.
     * @param offset the offset into the buffer object's data store where data replacement will begin, measured in bytes.
     * @param data   a pointer to the new data that will be copied into the data store.
     */
    public void glBufferSubData(int target, long offset, ByteBuffer data);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glClear">Reference Page</a></p>
     * <p>
     * Sets portions of every pixel in a particular buffer to the same value. The value to which each buffer is cleared depends on the setting of the clear
     * value for that buffer.
     *
     * @param mask Zero or the bitwise OR of one or more values indicating which buffers are to be cleared.
     */
    public void glClear(int mask);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glClearColor">Reference Page</a></p>
     *
     * Sets the clear value for fixed-point and floating-point color buffers in RGBA mode. The specified components are stored as floating-point values.
     *
     * @param red   the value to which to clear the R channel of the color buffer.
     * @param green the value to which to clear the G channel of the color buffer.
     * @param blue  the value to which to clear the B channel of the color buffer.
     * @param alpha the value to which to clear the A channel of the color buffer.
     */
    public void glClearColor(float red, float green, float blue, float alpha);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glColorMask">Reference Page</a></p>
     *
     * Masks the writing of R, G, B and A values to all draw buffers. In the initial state, all color values are enabled for writing for all draw buffers.
     *
     * @param red   whether R values are written or not.
     * @param green whether G values are written or not.
     * @param blue  whether B values are written or not.
     * @param alpha whether A values are written or not.
     */
    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glCompileShader">Reference Page</a></p>
     * <p>
     * Compiles a shader object.
     *
     * @param shader the shader object to be compiled.
     */
    public void glCompileShader(int shader);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glCompressedTexImage2D">Reference Page</a></p>
     * <p>
     * Specifies a two-dimensional texture image in a compressed format.
     *
     * @param target         the target texture.
     * @param level          the level-of-detail number. Level 0 is the base image level. Level n is the nth mipmap reduction image.
     * @param internalFormat the format of the compressed image data.
     * @param width          the width of the texture image
     * @param height         the height of the texture image
     * @param border         must be 0
     * @param data           a pointer to the compressed image data
     */
    public void glCompressedTexImage2D(int target, int level, int internalFormat, int width, int height, int border,
                                ByteBuffer data);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glCompressedTexSubImage2D">Reference Page</a></p>
     *
     * Respecifies only a rectangular subregion of an existing 2D texel array, with incoming data stored in a specific compressed image format.
     *
     * @param target  the target texture.
     * @param level   the level-of-detail number. Level 0 is the base image level. Level n is the nth mipmap reduction image.
     * @param xoffset a texel offset in the x direction within the texture array.
     * @param yoffset a texel offset in the y direction within the texture array.
     * @param width   the width of the texture subimage.
     * @param height  the height of the texture subimage.
     * @param format  the format of the compressed image data stored at address {@code data}.
     * @param data    a pointer to the compressed image data.
     */
    public void glCompressedTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format,
                                   ByteBuffer data);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glCreateProgram">Reference Page</a></p>
     *
     * Creates a program object.
     *
     * @return the ID of the new program, or 0 if unsuccessful
     */
    public int glCreateProgram();

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glCreateShader">Reference Page</a></p>
     *
     * Creates a shader object.
     *
     * @param shaderType the type of shader to be created. One of:
     *  {@link #GL_VERTEX_SHADER VERTEX_SHADER}
     *  {@link #GL_FRAGMENT_SHADER FRAGMENT_SHADER}
     *  {@link GL3#GL_GEOMETRY_SHADER GEOMETRY_SHADER}
     *  {@link GL4#GL_TESS_CONTROL_SHADER TESS_CONTROL_SHADER}
     *  {@link GL4#GL_TESS_EVALUATION_SHADER TESS_EVALUATION_SHADER}
     * @return the ID of the new shader, or 0 if unsuccessful
     */
    public int glCreateShader(int shaderType);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glCullFace">Reference Page</a></p>
     * <p>
     * Specifies which polygon faces are culled if {@link #GL_CULL_FACE CULL_FACE} is enabled. Front-facing polygons are rasterized if either culling is disabled or the
     * CullFace mode is {@link #GL_BACK BACK} while back-facing polygons are rasterized only if either culling is disabled or the CullFace mode is
     * {@link #GL_FRONT FRONT}. The initial setting of the CullFace mode is {@link #GL_BACK BACK}. Initially, culling is disabled.
     *
     * @param mode the CullFace mode. One of:
     *  {@link #GL_FRONT FRONT}
     *  {@link #GL_BACK BACK}
     *  {@link #GL_FRONT_AND_BACK FRONT_AND_BACK}
     */
    public void glCullFace(int mode);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glDeleteBuffers">Reference Page</a></p>
     * <p>
     * Deletes named buffer objects.
     *
     * @param buffers an array of buffer objects to be deleted.
     */
    public void glDeleteBuffers(IntBuffer buffers);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glDeleteProgram">Reference Page</a></p>
     * <p>
     * Deletes a program object.
     *
     * @param program the program object to be deleted.
     */
    public void glDeleteProgram(int program);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glDeleteShader">Reference Page</a></p>
     * <p>
     * Deletes a shader object.
     *
     * @param shader the shader object to be deleted.
     */
    public void glDeleteShader(int shader);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glDeleteTextures">Reference Page</a></p>
     * <p>
     * Deletes texture objects. After a texture object is deleted, it has no contents or dimensionality, and its name is again unused. If a texture that is
     * currently bound to any of the target bindings of {@link #glBindTexture BindTexture} is deleted, it is as though {@link #glBindTexture BindTexture} had been executed with the
     * same target and texture zero. Additionally, special care must be taken when deleting a texture if any of the images of the texture are attached to a
     * framebuffer object.
     *
     * <p>Unused names in textures that have been marked as used for the purposes of {@link #glGenTextures GenTextures} are marked as unused again. Unused names in textures are
     * silently ignored, as is the name zero.</p>
     *
     * @param textures contains {@code n} names of texture objects to be deleted.
     */
    public void glDeleteTextures(IntBuffer textures);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glDepthFunc">Reference Page</a></p>
     * <p>
     * Specifies the comparison that takes place during the depth buffer test (when {@link #GL_DEPTH_TEST DEPTH_TEST} is enabled).
     *
     * @param func the depth test comparison. One of:
     *  {@link #GL_NEVER NEVER}
     *  {@link #GL_ALWAYS ALWAYS}
     *  {@link #GL_LESS LESS}
     *  {@link #GL_LEQUAL LEQUAL}
     *  {@link #GL_EQUAL EQUAL}
     *  {@link #GL_GREATER GREATER}
     *  {@link #GL_GEQUAL GEQUAL}
     *  {@link #GL_NOTEQUAL NOTEQUAL}
     */
    public void glDepthFunc(int func);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glDepthMask">Reference Page</a></p>
     * <p>
     * Masks the writing of depth values to the depth buffer. In the initial state, the depth buffer is enabled for writing.
     *
     * @param flag whether depth values are written or not.
     */
    public void glDepthMask(boolean flag);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glDepthRange">Reference Page</a></p>
     * <p>
     * Sets the depth range for all viewports to the same values.
     *
     * @param nearVal the near depth range.
     * @param farVal  the far depth range.
     */
    public void glDepthRange(double nearVal, double farVal);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glDetachShader">Reference Page</a></p>
     * <p>
     * Detaches a shader object from a program object to which it is attached.
     *
     * @param program the program object from which to detach the shader object.
     * @param shader  the shader object to be detached.
     */
    public void glDetachShader(int program, int shader);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glDisable">Reference Page</a></p>
     * <p>
     * Disables the specified OpenGL state.
     *
     * @param cap the OpenGL state to disable.
     */
    public void glDisable(int cap);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glDisableVertexAttribArray">Reference Page</a></p>
     * <p>
     * Disables a generic vertex attribute array.
     *
     * @param index the index of the generic vertex attribute to be disabled.
     */
    public void glDisableVertexAttribArray(int index);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glDrawArrays">Reference Page</a></p>
     * <p>
     * Constructs a sequence of geometric primitives by successively transferring elements for {@code count} vertices. Elements {@code first} through
     * <code>first + count &ndash; 1</code> of each enabled non-instanced array are transferred to the GL.
     *
     * <p>If an array corresponding to an attribute required by a vertex shader is not enabled, then the corresponding element is taken from the current attribute
     * state. If an array is enabled, the corresponding current vertex attribute value is unaffected by the execution of this function.</p>
     *
     * @param mode  the kind of primitives being constructed.
     * @param first the first vertex to transfer to the GL.
     * @param count the number of vertices after {@code first} to transfer to the GL.
     */
    public void glDrawArrays(int mode, int first, int count);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glDrawRangeElements">Reference Page</a></p>
     *
     * <p>Implementations denote recommended maximum amounts of vertex and index data, which may be queried by calling glGet with argument
     * {@link GL2#GL_MAX_ELEMENTS_VERTICES MAX_ELEMENTS_VERTICES} and {@link GL2#GL_MAX_ELEMENTS_INDICES MAX_ELEMENTS_INDICES}. If end - start + 1 is greater than the value of GL_MAX_ELEMENTS_VERTICES, or if
     * count is greater than the value of GL_MAX_ELEMENTS_INDICES, then the call may operate at reduced performance. There is no requirement that all vertices
     * in the range start end be referenced. However, the implementation may partially process unused vertices, reducing performance from what could be
     * achieved with an optimal index set.</p>
     *
     * <p>When glDrawRangeElements is called, it uses count sequential elements from an enabled array, starting at start to construct a sequence of geometric
     * primitives. mode specifies what kind of primitives are constructed, and how the array elements construct these primitives. If more than one array is
     * enabled, each is used.</p>
     *
     * <p>Vertex attributes that are modified by glDrawRangeElements have an unspecified value after glDrawRangeElements returns. Attributes that aren't modified
     * maintain their previous values.</p>
     *
     * Errors
     *
     * <p>It is an error for indices to lie outside the range start end, but implementations may not check for this situation. Such indices cause
     * implementation-dependent behavior.</p>
     *
     * <ul>
     * <li>GL_INVALID_ENUM is generated if mode is not an accepted value.</li>
     * <li>GL_INVALID_VALUE is generated if count is negative.</li>
     * <li>GL_INVALID_VALUE is generated if end &lt; start.</li>
     * <li>GL_INVALID_OPERATION is generated if a geometry shader is active and mode is incompatible with the input primitive type of the geometry shader in the
     * currently installed program object.</li>
     * <li>GL_INVALID_OPERATION is generated if a non-zero buffer object name is bound to an enabled array or the element array and the buffer object's data
     * store is currently mapped.</li>
     * </ul>
     *
     * @param mode    the kind of primitives to render.
     * @param start   the minimum array index contained in {@code indices}.
     * @param end     the maximum array index contained in {@code indices}.
     * @param count   the number of elements to be rendered.
     * @param type    the type of the values in {@code indices}.
     * @param indices a pointer to the location where the indices are stored.
     */
    public void glDrawRangeElements(int mode, int start, int end, int count, int type, long indices); /// GL2+

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glEnable">Reference Page</a></p>
     * <p>
     * Enables the specified OpenGL state.
     *
     * @param cap the OpenGL state to enable.
     */
    public void glEnable(int cap);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glEnableVertexAttribArray">Reference Page</a></p>
     * <p>
     * Enables a generic vertex attribute array.
     *
     * @param index the index of the generic vertex attribute to be enabled.
     */
    public void glEnableVertexAttribArray(int index);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glEndQuery">Reference Page</a></p>
     * <p>
     * Marks the end of the sequence of commands to be tracked for the active query specified by {@code target}.
     *
     * @param target the query object target.
     */
    public void glEndQuery(int target);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glGenBuffers">Reference Page</a></p>
     * <p>
     * Generates buffer object names.
     *
     * @param buffers a buffer in which the generated buffer object names are stored.
     */
    public void glGenBuffers(IntBuffer buffers);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glGenTextures">Reference Page</a></p>
     * <p>
     * Returns n previously unused texture names in textures. These names are marked as used, for the purposes of GenTextures only, but they acquire texture
     * state and a dimensionality only when they are first bound, just as if they were unused.
     *
     * @param textures a scalar or buffer in which to place the returned texture names.
     */
    public void glGenTextures(IntBuffer textures);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glGenQueries">Reference Page</a></p>
     *
     * Generates query object names.
     *
     * @param number the number of query object names to be generated
     * @param ids a buffer in which the generated query object names are stored.
     */
    public void glGenQueries(int number, IntBuffer ids);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glGetAttribLocation">Reference Page</a></p>
     *
     * Returns the location of an attribute variable.
     *
     * @param program the program object to be queried.
     * @param name    a null terminated string containing the name of the attribute variable whose location is to be queried.
     * @return        the location
     */
    public int glGetAttribLocation(int program, String name);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glGetBooleanv">Reference Page</a></p>
     *
     * Returns the current boolean value of the specified state variable.
     *
     * <p><b>LWJGL note</b>: The state that corresponds to the state variable may be a single value or an array of values. In the case of an array of values,
     * LWJGL will <b>not</b> validate if {@code params} has enough space to store that array. Doing so would introduce significant overhead, as the
     * OpenGL state variables are too many. It is the user's responsibility to avoid JVM crashes by ensuring enough space for the returned values.</p>
     *
     * @param pname  the state variable.
     * @param params a scalar or buffer in which to place the returned data.
     */
    public void glGetBoolean(int pname, ByteBuffer params);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glGetBufferSubData">Reference Page</a></p>
     *
     * Returns a subset of a buffer object's data store.
     *
     * @param target the target buffer object.
     * @param offset the offset into the buffer object's data store from which data will be returned, measured in bytes.
     * @param data   a pointer to the location where buffer object data is returned.
     */
    public void glGetBufferSubData(int target, long offset, ByteBuffer data);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glGetError">Reference Page</a></p>
     *
     * Returns error information. Each detectable error is assigned a numeric code. When an error is detected, a flag is set and the code is recorded. Further
     * errors, if they occur, do not affect this recorded code. When {@code GetError} is called, the code is returned and the flag is cleared, so that a
     * further error will again record its code. If a call to {@code GetError} returns {@link #GL_NO_ERROR NO_ERROR}, then there has been no detectable error since
     * the last call to {@code GetError} (or since the GL was initialized).
     * @return the error code, or NO_ERROR if none
     */
    public int glGetError();

    /**
     * Determine the current single-precision floating-point value(s) of the
     * specified parameter.
     *
     * @param parameterId which parameter
     * @param storeValues storage for the value(s)
     */
    public void glGetFloat(int parameterId, FloatBuffer storeValues);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glGetIntegerv">Reference Page</a></p>
     *
     * Returns the current integer value of the specified state variable.
     *
     * <p><b>LWJGL note</b>: The state that corresponds to the state variable may be a single value or an array of values. In the case of an array of values,
     * LWJGL will <b>not</b> validate if {@code params} has enough space to store that array. Doing so would introduce significant overhead, as the
     * OpenGL state variables are too many. It is the user's responsibility to avoid JVM crashes by ensuring enough space for the returned values.</p>
     *
     * @param pname  the state variable.
     * @param params a scalar or buffer in which to place the returned data.
     */
    public void glGetInteger(int pname, IntBuffer params);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glGetProgram">Reference Page</a></p>
     * <p>
     * Returns a parameter from a program object.
     *
     * @param program the program object to be queried.
     * @param pname   the object parameter.
     * @param params  the requested object parameter.
     */
    public void glGetProgram(int program, int pname, IntBuffer params);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glGetProgramInfoLog">Reference Page</a></p>
     * <p>
     * Returns the information log for a program object.
     *
     * @param program the program object whose information log is to be queried.
     * @param maxSize the size of the character buffer for storing the returned information log.
     * @return the contents of the information log
     */
    public String glGetProgramInfoLog(int program, int maxSize);

    /**
     * Unsigned version.
     *
     * @param query the name of a query object
     * @param pname the symbolic name of a query object parameter
     * @return the value of the parameter
     */
    public long glGetQueryObjectui64(int query, int pname);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glGetQueryObject">Reference Page</a></p>
     * <p>
     * Returns the integer value of a query object parameter.
     *
     * @param query the name of a query object
     * @param pname the symbolic name of a query object parameter. One of:
     *  {@link #GL_QUERY_RESULT QUERY_RESULT}
     *  {@link #GL_QUERY_RESULT_AVAILABLE QUERY_RESULT_AVAILABLE}
     * @return the value of the parameter
     */
    public int glGetQueryObjectiv(int query, int pname);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glGetShader">Reference Page</a></p>
     * <p>
     * Returns a parameter from a shader object.
     *
     * @param shader the shader object to be queried.
     * @param pname  the object parameter.
     * @param params the requested object parameter.
     */
    public void glGetShader(int shader, int pname, IntBuffer params);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glGetShaderInfoLog">Reference Page</a></p>
     * <p>
     * Returns the information log for a shader object.
     *
     * @param shader  the shader object whose information log is to be queried.
     * @param maxSize the size of the character buffer for storing the returned information log.
     * @return the contents of the information log
     */
    public String glGetShaderInfoLog(int shader, int maxSize);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glGetString">Reference Page</a></p>
     * <p>
     * Return strings describing properties of the current GL context.
     *
     * @param name the property to query. One of:
     *  {@link #GL_RENDERER RENDERER}
     *  {@link #GL_VENDOR VENDOR}
     *  {@link #GL_EXTENSIONS EXTENSIONS}
     *  {@link #GL_VERSION VERSION}
     *  {@link GL2#GL_SHADING_LANGUAGE_VERSION SHADING_LANGUAGE_VERSION}
     * @return the value of the property
     */
    public String glGetString(int name);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glGetUniformLocation">Reference Page</a></p>
     * <p>
     * Returns the location of a uniform variable.
     *
     * @param program the program object to be queried.
     * @param name    a null terminated string containing the name of the uniform variable whose location is to be queried.
     * @return the location
     */
    public int glGetUniformLocation(int program, String name);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glIsEnabled">Reference Page</a></p>
     * <p>
     * Determines if {@code cap} is currently enabled (as with {@link #glEnable Enable}) or disabled.
     *
     * @param cap the enable state to query.
     * @return true if enabled, otherwise false
     */
    public boolean glIsEnabled(int cap);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glLineWidth">Reference Page</a></p>
     *
     * Sets the width of rasterized line segments. The default width is 1.0.
     *
     * @param width the line width.
     */
    public void glLineWidth(float width);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glLinkProgram">Reference Page</a></p>
     *
     * Links a program object.
     *
     * @param program the program object to be linked.
     */
    public void glLinkProgram(int program);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glPixelStorei">Reference Page</a></p>
     * <p>
     * Sets the integer value of a pixel store parameter.
     *
     * @param pname the pixel store parameter to set.
     * @param param the parameter value
     */
    public void glPixelStorei(int pname, int param);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glPolygonOffset">Reference Page</a></p>
     *
     * The depth values of all fragments generated by the rasterization of a polygon may be offset by a single value that is computed for that polygon. This
     * function determines that value.
     *
     * <p>{@code factor} scales the maximum depth slope of the polygon, and {@code units} scales an implementation-dependent constant that relates to the usable
     * resolution of the depth buffer. The resulting values are summed to produce the polygon offset value.</p>
     *
     * @param factor the maximum depth slope factor.
     * @param units  the constant scale.
     */
    public void glPolygonOffset(float factor, float units);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glReadPixels">Reference Page</a></p>
     * <p>
     * ReadPixels obtains values from the selected read buffer from each pixel with lower left hand corner at {@code (x + i, y + j)} for {@code 0 <= i < width}
     * and {@code 0 <= j < height}; this pixel is said to be the i<sup>th</sup> pixel in the j<sup>th</sup> row. If any of these pixels lies outside of the
     * window allocated to the current GL context, or outside of the image attached to the currently bound read framebuffer object, then the values obtained
     * for those pixels are undefined. When {@link GLFbo#GL_READ_FRAMEBUFFER_BINDING_EXT READ_FRAMEBUFFER_BINDING} is zero, values are also undefined for individual pixels that are not owned by
     * the current context. Otherwise, {@code ReadPixels} obtains values from the selected buffer, regardless of how those values were placed there.
     *
     * @param x      the left pixel coordinate
     * @param y      the lower pixel coordinate
     * @param width  the number of pixels to read in the x-dimension
     * @param height the number of pixels to read in the y-dimension
     * @param format the pixel format.
     * @param type   the pixel type.
     * @param data   a buffer in which to place the returned pixel data.
     */
    public void glReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer data);


    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glReadPixels">Reference Page</a></p>
     * <p>
     * ReadPixels obtains values from the selected read buffer from each pixel with lower left hand corner at {@code (x + i, y + j)} for {@code 0 <= i < width}
     * and {@code 0 <= j < height}; this pixel is said to be the i<sup>th</sup> pixel in the j<sup>th</sup> row. If any of these pixels lies outside of the
     * window allocated to the current GL context, or outside of the image attached to the currently bound read framebuffer object, then the values obtained
     * for those pixels are undefined. When {@link GLFbo#GL_READ_FRAMEBUFFER_BINDING_EXT READ_FRAMEBUFFER_BINDING} is zero, values are also undefined for individual pixels that are not owned by
     * the current context. Otherwise, {@code ReadPixels} obtains values from the selected buffer, regardless of how those values were placed there.
     *
     * @param x      the left pixel coordinate
     * @param y      the lower pixel coordinate
     * @param width  the number of pixels to read in the x-dimension
     * @param height the number of pixels to read in the y-dimension
     * @param format the pixel format.
     * @param type   the pixel type.
     * @param offset a buffer in which to place the returned pixel data/
     */
    public void glReadPixels(int x, int y, int width, int height, int format, int type, long offset);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glScissor">Reference Page</a></p>
     * <p>
     * Defines the scissor rectangle for all viewports. The scissor test is enabled or disabled for all viewports using {@link #glEnable Enable} or {@link #glDisable Disable}
     * with the symbolic constant {@link #GL_SCISSOR_TEST SCISSOR_TEST}. When disabled, it is as if the scissor test always passes. When enabled, if
     * left &lt;= x<sub>w</sub> &lt; left + width and bottom &lt;= y<sub>w</sub> &lt; bottom + height for the scissor rectangle, then the scissor
     * test passes. Otherwise, the test fails and the fragment is discarded.
     *
     * @param x      the left scissor rectangle coordinate.
     * @param y      the bottom scissor rectangle coordinate.
     * @param width  the scissor rectangle width.
     * @param height the scissor rectangle height.
     */
    public void glScissor(int x, int y, int width, int height);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glShaderSource">Reference Page</a></p>
     * <p>
     * Sets the source code in {@code shader} to the source code in the array of strings specified by {@code strings}. Any source code previously stored in the
     * shader object is completely replaced. The number of strings in the array is specified by {@code count}. If {@code length} is {@code NULL}, each string is
     * assumed to be null terminated. If {@code length} is a value other than {@code NULL}, it points to an array containing a string length for each of the
     * corresponding elements of {@code strings}. Each element in the length array may contain the length of the corresponding string (the null character is not
     * counted as part of the string length) or a value less than 0 to indicate that the string is null terminated. The source code strings are not scanned or
     * parsed at this time; they are simply copied into the specified shader object.
     *
     * @param shader  the shader object whose source code is to be replaced,
     * @param strings an array of pointers to strings containing the source code to be loaded into the shader
     * @param length  storage for the string lengths, or null for
     * null-terminated strings
     */
    public void glShaderSource(int shader, String[] strings, IntBuffer length);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glStencilFuncSeparate">Reference Page</a></p>
     * <p>
     * Sets front and/or back function and reference value for stencil testing.
     *
     * @param face whether front and/or back stencil state is updated. One of:
     *  {@link GL#GL_FRONT FRONT}
     *  {@link GL#GL_BACK BACK}
     *  {@link GL#GL_FRONT_AND_BACK FRONT_AND_BACK}
     * @param func the test function. The initial value is GL_ALWAYS. One of:
     *  {@link GL#GL_NEVER NEVER}
     *  {@link GL#GL_LESS LESS}
     *  {@link GL#GL_LEQUAL LEQUAL}
     *  {@link GL#GL_GREATER GREATER}
     *  {@link GL#GL_GEQUAL GEQUAL}
     *  {@link GL#GL_EQUAL EQUAL}
     *  {@link GL#GL_NOTEQUAL NOTEQUAL}
     *  {@link GL#GL_ALWAYS ALWAYS}
     * @param ref  the reference value for the stencil test. {@code ref} is clamped to the range [0, 2n &ndash; 1], where {@code n} is the number of bitplanes in the stencil
     *             buffer. The initial value is 0.
     * @param mask a mask that is ANDed with both the reference value and the stored stencil value when the test is done. The initial value is all 1's.
     */
    public void glStencilFuncSeparate(int face, int func, int ref, int mask);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glStencilOpSeparate">Reference Page</a></p>
     * <p>
     * Sets front and/or back stencil test actions.
     *
     * @param face   whether front and/or back stencil state is updated. One of:
     *  {@link GL#GL_FRONT FRONT}
     *  {@link GL#GL_BACK BACK}
     *  {@link GL#GL_FRONT_AND_BACK FRONT_AND_BACK}
     * @param sfail  the action to take when the stencil test fails. The initial value is GL_KEEP. One of:
     *  {@link GL#GL_KEEP KEEP}
     *  {@link GL#GL_ZERO ZERO}
     *  {@link GL#GL_REPLACE REPLACE}
     *  {@link GL#GL_INCR INCR}
     *  {@link GL#GL_INCR_WRAP INCR_WRAP}
     *  {@link GL#GL_DECR DECR}
     *  {@link GL#GL_DECR_WRAP DECR_WRAP}
     *  {@link GL#GL_INVERT INVERT}
     * @param dpfail the stencil action when the stencil test passes, but the depth test fails. The initial value is GL_KEEP.
     * @param dppass the stencil action when both the stencil test and the depth test pass, or when the stencil test passes and either there is no depth buffer or depth
     *               testing is not enabled. The initial value is GL_KEEP.
     */
    public void glStencilOpSeparate(int face, int sfail, int dpfail, int dppass);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glTexImage2D">Reference Page</a></p>
     * <p>
     * Specifies a two-dimensional texture image.
     *
     * @param target         the texture target.
     * @param level          the level-of-detail number.
     * @param internalFormat the texture internal format.
     * @param width          the texture width.
     * @param height         the texture height.
     * @param border         the texture border width.
     * @param format         the texel data format.
     * @param type           the texel data type.
     * @param data           the texel data.
     */
    public void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format,
                      int type, ByteBuffer data);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glTexParameterf">Reference Page</a></p>
     * <p>
     * Float version of {@link #glTexParameteri TexParameteri}.
     *
     * @param target the texture target.
     * @param pname  the parameter to set.
     * @param param  the parameter value.
     */
    public void glTexParameterf(int target, int pname, float param);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glTexParameteri">Reference Page</a></p>
     * <p>
     * Sets the integer value of a texture parameter, which controls how the texel array is treated when specified or changed, and when applied to a fragment.
     *
     * @param target the texture target.
     * @param pname  the parameter to set.
     * @param param  the parameter value.
     */
    public void glTexParameteri(int target, int pname, int param);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glTexSubImage2D">Reference Page</a></p>
     * <p>
     * Respecifies a rectangular subregion of an existing texel array. No change is made to the internalformat, width, height, depth, or border parameters of
     * the specified texel array, nor is any change made to texel values outside the specified subregion.
     *
     * @param target  the texture target.
     * @param level   the level-of-detail-number
     * @param xoffset the left coordinate of the texel subregion
     * @param yoffset the bottom coordinate of the texel subregion
     * @param width   the subregion width
     * @param height  the subregion height
     * @param format  the pixel data format.
     * @param type    the pixel data type.
     * @param data    the pixel data.
     */
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type,
                         ByteBuffer data);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glUniform">Reference Page</a></p>
     * <p>
     * Specifies the value of a single float uniform variable or a float uniform variable array for the current program object.
     *
     * @param location the location of the uniform variable to be modified.
     * @param value    a pointer to an array of {@code count} values that will be used to update the specified uniform variable.
     */
    public void glUniform1(int location, FloatBuffer value);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glUniform">Reference Page</a></p>
     * <p>
     * Specifies the value of a single int uniform variable or an int uniform variable array for the current program object.
     *
     * @param location the location of the uniform variable to be modified.
     * @param value    a pointer to an array of {@code count} values that will be used to update the specified uniform variable.
     */
    public void glUniform1(int location, IntBuffer value);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glUniform">Reference Page</a></p>
     * <p>
     * Specifies the value of a float uniform variable for the current program object.
     *
     * @param location the location of the uniform variable to be modified.
     * @param v0       the uniform value.
     */
    public void glUniform1f(int location, float v0);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glUniform">Reference Page</a></p>
     * <p>
     * Specifies the value of an int uniform variable for the current program object.
     *
     * @param location the location of the uniform variable to be modified.
     * @param v0       the uniform value.
     */
    public void glUniform1i(int location, int v0);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glUniform">Reference Page</a></p>
     * <p>
     * Specifies the value of a single ivec2 uniform variable or an ivec2 uniform variable array for the current program object.
     *
     * @param location the location of the uniform variable to be modified.
     * @param value    a pointer to an array of {@code count} values that will be used to update the specified uniform variable.
     */
    public void glUniform2(int location, IntBuffer value);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glUniform">Reference Page</a></p>
     * <p>
     * Specifies the value of a single vec2 uniform variable or a vec2 uniform variable array for the current program object.
     *
     * @param location the location of the uniform variable to be modified.
     * @param value    a pointer to an array of {@code count} values that will be used to update the specified uniform variable.
     */
    public void glUniform2(int location, FloatBuffer value);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glUniform">Reference Page</a></p>
     * <p>
     * Specifies the value of a vec2 uniform variable for the current program object.
     *
     * @param location the location of the uniform variable to be modified.
     * @param v0       the uniform x value.
     * @param v1       the uniform y value.
     */
    public void glUniform2f(int location, float v0, float v1);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glUniform">Reference Page</a></p>
     * <p>
     * Specifies the value of a single ivec3 uniform variable or an ivec3 uniform variable array for the current program object.
     *
     * @param location the location of the uniform variable to be modified.
     * @param value    a pointer to an array of {@code count} values that will be used to update the specified uniform variable.
     */
    public void glUniform3(int location, IntBuffer value);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glUniform">Reference Page</a></p>
     * <p>
     * Specifies the value of a single vec3 uniform variable or a vec3 uniform variable array for the current program object.
     *
     * @param location the location of the uniform variable to be modified.
     * @param value    a pointer to an array of {@code count} values that will be used to update the specified uniform variable.
     */
    public void glUniform3(int location, FloatBuffer value);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glUniform">Reference Page</a></p>
     * <p>
     * Specifies the value of a vec3 uniform variable for the current program object.
     *
     * @param location the location of the uniform variable to be modified.
     * @param v0       the uniform x value.
     * @param v1       the uniform y value.
     * @param v2       the uniform z value.
     */
    public void glUniform3f(int location, float v0, float v1, float v2);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glUniform">Reference Page</a></p>
     * <p>
     * Specifies the value of a single vec4 uniform variable or a vec4 uniform variable array for the current program object.
     *
     * @param location the location of the uniform variable to be modified.
     * @param value    a pointer to an array of {@code count} values that will be used to update the specified uniform variable.
     */
    public void glUniform4(int location, FloatBuffer value);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glUniform">Reference Page</a></p>
     * <p>
     * Specifies the value of a single ivec4 uniform variable or an ivec4 uniform variable array for the current program object.
     *
     * @param location the location of the uniform variable to be modified.
     * @param value    a pointer to an array of {@code count} values that will be used to update the specified uniform variable.
     */
    public void glUniform4(int location, IntBuffer value);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glUniform">Reference Page</a></p>
     * <p>
     * Specifies the value of a vec4 uniform variable for the current program object.
     *
     * @param location the location of the uniform variable to be modified.
     * @param v0       the uniform x value.
     * @param v1       the uniform y value.
     * @param v2       the uniform z value.
     * @param v3       the uniform w value.
     */
    public void glUniform4f(int location, float v0, float v1, float v2, float v3);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glUniform">Reference Page</a></p>
     * <p>
     * Specifies the value of a single mat3 uniform variable or a mat3 uniform variable array for the current program object.
     *
     * @param location  the location of the uniform variable to be modified.
     * @param transpose whether to transpose the matrix as the values are loaded into the uniform variable.
     * @param value     a pointer to an array of {@code count} values that will be used to update the specified uniform variable.
     */
    public void glUniformMatrix3(int location, boolean transpose, FloatBuffer value);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glUniform">Reference Page</a></p>
     * <p>
     * Specifies the value of a single mat4 uniform variable or a mat4 uniform variable array for the current program object.
     *
     * @param location  the location of the uniform variable to be modified.
     * @param transpose whether to transpose the matrix as the values are loaded into the uniform variable.
     * @param value     a pointer to an array of {@code count} values that will be used to update the specified uniform variable.
     */
    public void glUniformMatrix4(int location, boolean transpose, FloatBuffer value);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glUseProgram">Reference Page</a></p>
     * <p>
     * Installs a program object as part of current rendering state.
     *
     * @param program the program object whose executables are to be used as part of current rendering state.
     */
    public void glUseProgram(int program);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glVertexAttribPointer">Reference Page</a></p>
     * <p>
     * Specifies the location and organization of a vertex attribute array.
     *
     * @param index      the index of the generic vertex attribute to be modified
     * @param size       the number of values per vertex that are stored in the array.
     * @param type       the data type of each component in the array. The initial value is GL_FLOAT.
     * @param normalized whether fixed-point data values should be normalized or converted directly as fixed-point values when they are accessed
     * @param stride     the byte offset between consecutive generic vertex attributes. If stride is 0, the generic vertex attributes are understood to be tightly packed in
     *                   the array. The initial value is 0.
     * @param pointer    the vertex attribute data or the offset of the first component of the first generic vertex attribute in the array in the data store of the buffer
     *                   currently bound to the {@link GL#GL_ARRAY_BUFFER ARRAY_BUFFER} target. The initial value is 0.
     */
    public void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glViewport">Reference Page</a></p>
     *
     * Specifies the viewport transformation parameters for all viewports.
     * 
     * <p>In the initial state, {@code width} and {@code height} for each viewport are set to the width and height, respectively, of the window into which the GL is to do
     * its rendering. If the default framebuffer is bound but no default framebuffer is associated with the GL context, then {@code width} and {@code height} are
     * initially set to zero.</p>
     *
     * @param x      the left viewport coordinate.
     * @param y      the bottom viewport coordinate.
     * @param width  the viewport width.
     * @param height the viewport height.
     */
    public void glViewport(int x, int y, int width, int height);
}
