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
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Baseline GL methods that must be available on all platforms.
 * 
 * This is the subset of vanilla desktop OpenGL 2 and OpenGL ES 2.
 * 
 * @author Kirill Vainer
 */
public interface GL {

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

        public void resetStats();
        
	public void glActiveTexture(int texture);
	public void glAttachShader(int program, int shader);
    public void glBeginQuery(int target, int query);
	public void glBindBuffer(int target, int buffer);
	public void glBindTexture(int target, int texture);
	public void glBlendEquationSeparate(int colorMode, int alphaMode);
	public void glBlendFunc(int sfactor, int dfactor);
        public void glBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha);
        public void glBufferData(int target, long data_size, int usage);
	public void glBufferData(int target, FloatBuffer data, int usage);
	public void glBufferData(int target, ShortBuffer data, int usage);
	public void glBufferData(int target, ByteBuffer data, int usage);
	public void glBufferSubData(int target, long offset, FloatBuffer data);
	public void glBufferSubData(int target, long offset, ShortBuffer data);
	public void glBufferSubData(int target, long offset, ByteBuffer data);
	public void glClear(int mask);
	public void glClearColor(float red, float green, float blue, float alpha);
	public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha);
	public void glCompileShader(int shader);
	public void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, ByteBuffer data);
	public void glCompressedTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, ByteBuffer data);
	public int glCreateProgram();
	public int glCreateShader(int shaderType);
	public void glCullFace(int mode);
	public void glDeleteBuffers(IntBuffer buffers);
	public void glDeleteProgram(int program);
	public void glDeleteShader(int shader);
	public void glDeleteTextures(IntBuffer textures);
	public void glDepthFunc(int func);
	public void glDepthMask(boolean flag);
	public void glDepthRange(double nearVal, double farVal);
	public void glDetachShader(int program, int shader);
	public void glDisable(int cap);
	public void glDisableVertexAttribArray(int index);
	public void glDrawArrays(int mode, int first, int count);
        
	public void glDrawRangeElements(int mode, int start, int end, int count, int type, long indices); /// GL2+
	public void glEnable(int cap);
	public void glEnableVertexAttribArray(int index);
    public void glEndQuery(int target);
	public void glGenBuffers(IntBuffer buffers);
	public void glGenTextures(IntBuffer textures);
    public void glGenQueries(int number, IntBuffer ids);
	public int glGetAttribLocation(int program, String name);
	public void glGetBoolean(int pname, ByteBuffer params);
        public void glGetBufferSubData(int target, long offset, ByteBuffer data);
        public int glGetError();
	public void glGetInteger(int pname, IntBuffer params);
	public void glGetProgram(int program, int pname, IntBuffer params);
	public String glGetProgramInfoLog(int program, int maxSize);
    public long glGetQueryObjectui64(int query, int pname);
    public int glGetQueryObjectiv(int query, int pname);
    public void glGetShader(int shader, int pname, IntBuffer params);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glGetShaderInfoLog">Reference Page</a></p>
     * <p>
     * Returns the information log for a shader object.
     *
     * @param shader  the shader object whose information log is to be queried.
     * @param maxSize the size of the character buffer for storing the returned information log.
     */
    public String glGetShaderInfoLog(int shader, int maxSize);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glGetString">Reference Page</a></p>
     * <p>
     * Return strings describing properties of the current GL context.
     *
     * @param name the property to query. One of:<br><table><tr><td>{@link #GL_RENDERER RENDERER}</td><td>{@link #GL_VENDOR VENDOR}</td><td>{@link #GL_EXTENSIONS EXTENSIONS}</td><td>{@link #GL_VERSION VERSION}</td><td>{@link GL2#GL_SHADING_LANGUAGE_VERSION SHADING_LANGUAGE_VERSION}</td></tr></table>
     */
    public String glGetString(int name);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glGetUniformLocation">Reference Page</a></p>
     * <p>
     * Returns the location of a uniform variable.
     *
     * @param program the program object to be queried.
     * @param name    a null terminated string containing the name of the uniform variable whose location is to be queried.
     */
    public int glGetUniformLocation(int program, String name);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glIsEnabled">Reference Page</a></p>
     * <p>
     * Determines if {@code cap} is currently enabled (as with {@link #glEnable Enable}) or disabled.
     *
     * @param cap the enable state to query.
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
     * <p>
     * The depth values of all fragments generated by the rasterization of a polygon may be offset by a single value that is computed for that polygon. This
     * function determines that value.
     * <p>
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
	public void glShaderSource(int shader, String[] string, IntBuffer length);
    public void glStencilFuncSeparate(int face, int func, int ref, int mask);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glStencilOpSeparate">Reference Page</a></p>
     * <p>
     * Sets front and/or back stencil test actions.
     *
     * @param face   whether front and/or back stencil state is updated. One of:<br><table><tr><td>{@link GL#GL_FRONT FRONT}</td><td>{@link GL#GL_BACK BACK}</td><td>{@link GL#GL_FRONT_AND_BACK FRONT_AND_BACK}</td></tr></table>
     * @param sfail  the action to take when the stencil test fails. The initial value is GL_KEEP. One of:<br><table><tr><td>{@link GL#GL_KEEP KEEP}</td><td>{@link GL#GL_ZERO ZERO}</td><td>{@link GL#GL_REPLACE REPLACE}</td><td>{@link GL#GL_INCR INCR}</td><td>{@link GL#GL_INCR_WRAP INCR_WRAP}</td><td>{@link GL#GL_DECR DECR}</td><td>{@link GL#GL_DECR_WRAP DECR_WRAP}</td><td>{@link GL#GL_INVERT INVERT}</td></tr></table>
     * @param dpfail the stencil action when the stencil test passes, but the depth test fails. The initial value is GL_KEEP.
     * @param dppass the stencil action when both the stencil test and the depth test pass, or when the stencil test passes and either there is no depth buffer or depth
     *               testing is not enabled. The initial value is GL_KEEP.
     */
    public void glStencilOpSeparate(int face, int sfail, int dpfail, int dppass);
	public void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, ByteBuffer data);
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
	public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, ByteBuffer data);
    public void glUniform1(int location, FloatBuffer value);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glUniform">Reference Page</a></p>
     * <p>
     * Specifies the value of a single int uniform variable or a int uniform variable array for the current program object.
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
     * <p>
     * Specifies the viewport transformation parameters for all viewports.
     * <p>
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
