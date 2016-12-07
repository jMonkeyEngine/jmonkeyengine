package com.jme3.renderer.ios;

import com.jme3.renderer.RendererException;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>iOS GLES interface</code> iOS alternative to Android's GLES20 class
 * 
 * @author Kostyantyn Hushchyn
 */
public class JmeIosGLES {
    private static final Logger logger = Logger.getLogger(JmeIosGLES.class.getName());
    
	private static boolean ENABLE_ERROR_CHECKING = true;

    public static final int GL_ALPHA = 0x00001906;
    public static final int GL_ALWAYS = 0x00000207;
	public static final int GL_ARRAY_BUFFER = 0x00008892;
	public static final int GL_BACK = 0x00000405;
	public static final int GL_BLEND = 0x00000be2;
	public static final int GL_BYTE = 0x00001400;
	public static final int GL_CLAMP_TO_EDGE = 0x0000812f;
	public static final int GL_COLOR_ATTACHMENT0 = 0x00008ce0;
	public static final int GL_COLOR_BUFFER_BIT = 0x00004000;
	public static final int GL_COMPILE_STATUS = 0x00008b81;
	public static final int GL_COMPRESSED_TEXTURE_FORMATS = 0x000086a3;
	public static final int GL_CULL_FACE = 0x00000b44;
	public static final int GL_DEPTH_ATTACHMENT = 0x00008d00;
	public static final int GL_DEPTH_BUFFER_BIT = 0x00000100;
    public static final int GL_DEPTH_COMPONENT = 0x00001902;
    public static final int GL_DEPTH_COMPONENT16 = 0x000081a5;
	public static final int GL_DEPTH_TEST = 0x00000b71;
	public static final int GL_DITHER = 0x00000bd0;
	public static final int GL_DST_COLOR = 0x00000306;
	public static final int GL_DYNAMIC_DRAW = 0x000088e8;
	public static final int GL_EQUAL = 0x00000202;
	public static final int GL_ELEMENT_ARRAY_BUFFER = 0x00008893;
	public static final int GL_EXTENSIONS = 0x00001f03;
	public static final int GL_FALSE = 0x00000000;
	public static final int GL_FLOAT = 0x00001406;
	public static final int GL_FRAGMENT_SHADER = 0x00008b30;
	public static final int GL_FRAMEBUFFER = 0x00008d40;
	public static final int GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME = 0x00008cd1;
	public static final int GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE = 0x00008cd0;
	public static final int GL_FRAMEBUFFER_COMPLETE = 0x00008cd5;
	public static final int GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT = 0x00008cd6;
	public static final int GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS = 0x00008cd9;
	public static final int GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = 0x00008cd7;
	public static final int GL_FRAMEBUFFER_UNSUPPORTED = 0x00008cdd;
	public static final int GL_FRONT = 0x00000404;
	public static final int GL_FRONT_AND_BACK = 0x00000408;
	public static final int GL_GEQUAL = 0x00000206;
	public static final int GL_GREATER = 0x00000204;
	public static final int GL_HIGH_FLOAT = 0x00008df2;
	public static final int GL_INFO_LOG_LENGTH = 0x00008b84;
	public static final int GL_INT = 0x00001404;
	public static final int GL_LEQUAL = 0x00000203;
	public static final int GL_LESS = 0x00000201;
	public static final int GL_LINEAR = 0x00002601;
	public static final int GL_LINEAR_MIPMAP_LINEAR = 0x00002703;
	public static final int GL_LINEAR_MIPMAP_NEAREST = 0x00002701;
	public static final int GL_LINES = 0x00000001;
	public static final int GL_LINE_LOOP = 0x00000002;
	public static final int GL_LINE_STRIP = 0x00000003;
	public static final int GL_LINK_STATUS = 0x00008b82;
	public static final int GL_LUMINANCE = 0x00001909;
	public static final int GL_LUMINANCE_ALPHA = 0x0000190a;
	public static final int GL_MAX_CUBE_MAP_TEXTURE_SIZE = 0x0000851c;
	public static final int GL_MAX_FRAGMENT_UNIFORM_VECTORS = 0x00008dfd;
	public static final int GL_MAX_RENDERBUFFER_SIZE = 0x000084e8;
	public static final int GL_MAX_TEXTURE_IMAGE_UNITS = 0x00008872;
	public static final int GL_MAX_TEXTURE_SIZE = 0x00000d33;
	public static final int GL_MAX_VARYING_VECTORS = 0x00008dfc;
	public static final int GL_MAX_VERTEX_ATTRIBS = 0x00008869;
	public static final int GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS = 0x00008b4c;
	public static final int GL_MAX_VERTEX_UNIFORM_VECTORS = 0x00008dfb;
	public static final int GL_MIRRORED_REPEAT = 0x00008370;
	public static final int GL_NEAREST = 0x00002600;
	public static final int GL_NEAREST_MIPMAP_LINEAR = 0x00002702;
	public static final int GL_NEAREST_MIPMAP_NEAREST = 0x00002700;
	public static final int GL_NEVER = 0x00000200;
	public static final int GL_NONE = 0x00000000;
	public static final int GL_NOTEQUAL = 0x00000205;
	public static final int GL_NUM_COMPRESSED_TEXTURE_FORMATS = 0x000086a2;
	public static final int GL_ONE = 0x00000001;
	public static final int GL_ONE_MINUS_SRC_ALPHA = 0x00000303;
	public static final int GL_ONE_MINUS_SRC_COLOR = 0x00000301;
	public static final int GL_POINTS = 0x00000000;
	public static final int GL_POLYGON_OFFSET_FILL = 0x00008037;
	public static final int GL_RENDERBUFFER = 0x00008d41;
	public static final int GL_RENDERER = 0x00001f01;
	public static final int GL_REPEAT = 0x00002901;
    public static final int GL_RGB = 0x00001907;
	public static final int GL_RGB565 = 0x00008d62;
	public static final int GL_RGB5_A1 = 0x00008057;
	public static final int GL_RGBA = 0x00001908;
    public static final int GL_RGBA4 = 0x00008056;
	public static final int GL_SAMPLE_ALPHA_TO_COVERAGE = 0x0000809e;
	public static final int GL_SCISSOR_TEST = 0x00000c11;
	public static final int GL_SHADING_LANGUAGE_VERSION = 0x00008b8c;
	public static final int GL_SHORT = 0x00001402;
	public static final int GL_SRC_COLOR = 0x00000300;
	public static final int GL_SRC_ALPHA = 0x00000302;
	public static final int GL_STATIC_DRAW = 0x000088e4;
	public static final int GL_STENCIL_BUFFER_BIT = 0x00000400;
	public static final int GL_STREAM_DRAW = 0x000088e0;
	public static final int GL_SUBPIXEL_BITS = 0x00000d50;
	public static final int GL_TEXTURE = 0x00001702;
	public static final int GL_TEXTURE0 = 0x000084c0;
	public static final int GL_TEXTURE_2D = 0x00000de1;
	public static final int GL_TEXTURE_CUBE_MAP = 0x00008513;
	public static final int GL_TEXTURE_CUBE_MAP_POSITIVE_X = 0x00008515;
	public static final int GL_TEXTURE_MAG_FILTER = 0x00002800;
	public static final int GL_TEXTURE_MIN_FILTER = 0x00002801;
	public static final int GL_TEXTURE_WRAP_S = 0x00002802;
	public static final int GL_TEXTURE_WRAP_T = 0x00002803;
	public static final int GL_TRIANGLES = 0x00000004;
	public static final int GL_TRIANGLE_FAN = 0x00000006;
	public static final int GL_TRIANGLE_STRIP = 0x00000005;
	public static final int GL_TRUE = 0x00000001;
	public static final int GL_UNPACK_ALIGNMENT = 0x00000cf5;
	public static final int GL_UNSIGNED_BYTE = 0x00001401;
	public static final int GL_UNSIGNED_INT = 0x00001405;
	public static final int GL_UNSIGNED_SHORT = 0x00001403;
    public static final int GL_UNSIGNED_SHORT_4_4_4_4 = 0x00008033;
    public static final int GL_UNSIGNED_SHORT_5_5_5_1 = 0x00008034;
    public static final int GL_UNSIGNED_SHORT_5_6_5 = 0x00008363;
	public static final int GL_VENDOR = 0x00001f00;
	public static final int GL_VERSION = 0x00001f02;
	public static final int GL_VERTEX_SHADER = 0x00008b31;
	public static final int GL_ZERO = 0x00000000;

	public static native void glActiveTexture(int texture);
	public static native void glAttachShader(int program, int shader);
	public static native void glBindBuffer(int target, int buffer);
	public static native void glBindFramebuffer(int target, int framebuffer);
	public static native void glBindRenderbuffer(int target, int renderbuffer);
	public static native void glBindTexture(int target, int texture);
//	public static native void glBindVertexArray // TODO: Investigate this 
    public static native void glBlendEquationSeparate(int colorMode, int alphaMode);
	public static native void glBlendFunc(int sfactor, int dfactor);
	public static native void glBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha);
	public static native void glBufferData(int target, int size, Buffer data, int usage);
	public static native void glBufferData2(int target, int size, byte[] data, int offset, int usage);
	public static native void glBufferSubData(int target, int offset, int size, Buffer data);
	public static native void glBufferSubData2(int target, int offset, int size, byte[] data, int dataoffset);
	public static native int glCheckFramebufferStatus(int target);
	public static native void glClear(int mask);
	public static native void glClearColor(float red, float green, float blue, float alpha);
	public static native void glColorMask(boolean red, boolean green, boolean blue, boolean alpha);
	public static native void glCompileShader(int shader);
	public static native void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, int imageSize, Buffer pixels);
	public static native void glCompressedTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int imageSize, Buffer pixels);
	public static native int glCreateProgram();
	public static native int glCreateShader(int type);
	public static native void glCullFace(int mode);
	public static native void glDeleteBuffers(int n, int[] buffers, int offset);
	public static native void glDeleteFramebuffers(int n, int[] framebuffers, int offset);
	public static native void glDeleteProgram(int program);
	public static native void glDeleteRenderbuffers(int n, int[] renderbuffers, int offset);
	public static native void glDeleteShader(int shader);
	public static native void glDeleteTextures(int n, int[] textures, int offset);
	public static native void glDepthFunc(int func);
	public static native void glDepthMask(boolean flag);
	public static native void glDepthRangef(float zNear, float zFar);
	public static native void glDetachShader(int program, int shader);
	public static native void glDisableVertexAttribArray(int index);
	public static native void glDisable(int cap);
	public static native void glDrawArrays(int mode, int first, int count);
	public static native void glDrawElements(int mode, int count, int type, Buffer indices);
	public static native void glDrawElements2(int mode, int count, int type, byte[] indices, int offset);
	public static native void glDrawElementsIndex(int mode, int count, int type, int offset);
	public static native void glEnable(int cap);
	public static native void glEnableVertexAttribArray(int index);
	public static native void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer);
	public static native void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level);
	public static native void glGenBuffers(int n, int[] buffers, int offset);
	public static native void glGenFramebuffers(int n, int[] framebuffers, int offset);
	public static native void glGenRenderbuffers(int n, int[] renderbuffers, int offset);
	public static native void glGenTextures(int n, int[] textures, int offset);
	public static native void glGenerateMipmap(int target);
	public static native int glGetAttribLocation(int program, String name);
	public static native int glGetError();
	public static native void glGetFramebufferAttachmentParameteriv(int target, int attachment, int pname, int[] params, int offset);
	public static native void glGetIntegerv (int pname, int[] params, int offset);
	public static native String glGetProgramInfoLog(int program);
	public static native void glGetProgramiv(int program, int pname, int[] params, int offset);
	public static native String glGetShaderInfoLog(int shader);
	public static native void glGetShaderiv(int shader, int pname, int[] params, int offset);
	public static native String glGetString(int name);
	public static native int glGetUniformLocation(int program, String name);
	public static native boolean glIsFramebuffer(int framebuffer);
	public static native boolean glIsRenderbuffer(int renderbuffer);
	public static native void glLineWidth(float width);
	public static native void glLinkProgram(int program);
	public static native void glPixelStorei(int pname, int param);
	public static native void glPolygonOffset(float factor, float units);
	public static native void glReadPixels(int vpX, int vpY, int vpW, int vpH, int format, int type, Buffer pixels);
	public static native void glReadPixels2(int vpX, int vpY, int vpW, int vpH, int format, int type, byte[] pixels, int offset, int size);
	public static native void glRenderbufferStorage(int target, int internalformat, int width, int height);
	public static native void glScissor(int x, int y, int width, int height);
	public static native void glShaderSource(int shader, String string);
	public static native void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, Buffer pixels);
	public static native void glTexParameteri(int target, int pname, int param);
	public static native void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, Buffer pixels);
	public static native void glUniform1f(int location, float x);
	public static native void glUniform1fv(int location, int count, FloatBuffer v);
	public static native void glUniform1fv2(int location, int count, float[] v, int offset);
	public static native void glUniform1i(int location, int x);
	public static native void glUniform1iv(int location, int count, IntBuffer v);
	public static native void glUniform1iv2(int location, int count, int[] v, int offset);
	public static native void glUniform2f(int location, float x, float y);
	public static native void glUniform2fv(int location, int count, FloatBuffer v);
	public static native void glUniform2fv2(int location, int count, float[] v, int offset);
	public static native void glUniform3f(int location, float x, float y, float z);
	public static native void glUniform3fv(int location, int count, FloatBuffer v);
	public static native void glUniform3fv2(int location, int count, float[] v, int offset);
	public static native void glUniform4f(int location, float x, float y, float z, float w);
	public static native void glUniform4fv(int location, int count, FloatBuffer v);
	public static native void glUniform4fv2(int location, int count, float[] v, int offset);
	public static native void glUniformMatrix3fv(int location, int count, boolean transpose, FloatBuffer value);
	public static native void glUniformMatrix3fv2(int location, int count, boolean transpose, float[] value, int offset);
	public static native void glUniformMatrix4fv(int location, int count, boolean transpose, FloatBuffer value);
	public static native void glUniformMatrix4fv2(int location, int count, boolean transpose, float[] value, int offset);
	public static native void glUseProgram(int program);
	//public static native void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, byte[] ptr, int offset);
	public static native void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, Buffer ptr);
	public static native void glVertexAttribPointer2(int indx, int size, int type, boolean normalized, int stride, int offset);
	public static native void glViewport(int x, int y, int width, int height);
	
	
    public static void checkGLError() {
        if (!ENABLE_ERROR_CHECKING) {
            return;
        }
        int error = glGetError();
        if (error != 0) {
            String message = null;//GLU.gluErrorString(error);
            if (message == null) {
                throw new RendererException("An unknown [" + error + "] OpenGL error has occurred.");
            } else {
                throw new RendererException("An OpenGL error has occurred: " + message);
            }
        }
    }
    
    /*
    public static String gluErrorString(int error) {
        switch (error) {
        case GL10.GL_NO_ERROR:
            return "no error";
        case GL10.GL_INVALID_ENUM:
            return "invalid enum";
        case GL10.GL_INVALID_VALUE:
            return "invalid value";
        case GL10.GL_INVALID_OPERATION:
            return "invalid operation";
        case GL10.GL_STACK_OVERFLOW:
            return "stack overflow";
        case GL10.GL_STACK_UNDERFLOW:
            return "stack underflow";
        case GL10.GL_OUT_OF_MEMORY:
            return "out of memory";
        default:
            return null;
        }
    }
    */

}