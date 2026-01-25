 
package com.jme3.renderer.lwjgl;

import com.jme3.renderer.RendererException;
import com.jme3.renderer.opengl.GL;
import com.jme3.renderer.opengl.GL2;
import com.jme3.renderer.opengl.GLES_30;
import com.jme3.renderer.opengl.GLExt;
import com.jme3.renderer.opengl.GLFbo;
import com.jme3.util.BufferUtils;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.opengles.GLES20;
import org.lwjgl.opengles.GLES30;
import org.lwjgl.opengles.GLES31;


public class LwjglGLES extends LwjglRender implements GL, GL2, GLES_30, GLExt, GLFbo {

    private final IntBuffer tmpBuff = BufferUtils.createIntBuffer(1);
    private final IntBuffer tmpBuff16 = BufferUtils.createIntBuffer(16);

    @Override
    public void resetStats() {
    }


    public static void checkLimit(Buffer buffer) {
        if (buffer == null) return;
        if (buffer.limit() == 0) {
            throw new RendererException("Attempting to upload empty buffer (limit = 0), that's an error");
        }
        if (buffer.remaining() == 0) {
            throw new RendererException("Attempting to upload empty buffer (remaining = 0), that's an error");
        }
    }

    private static int getLimitBytes(ByteBuffer buffer) {
        checkLimit(buffer);
        return buffer.remaining();
    }

    private static int getLimitBytes(ShortBuffer buffer) {
        checkLimit(buffer);
        return buffer.remaining() * 2;
    }

    private static int getLimitBytes(IntBuffer buffer) {
        checkLimit(buffer);
        return buffer.remaining() * 4;
    }

    private static int getLimitBytes(FloatBuffer buffer) {
        checkLimit(buffer);
        return buffer.remaining() * 4;
    }

    private static int getLimitCount(Buffer buffer, int elementSize) {
        checkLimit(buffer);
        return buffer.remaining() / elementSize;
    }


    @Override
    public void glActiveTexture(int texture) {
        GLES20.glActiveTexture(texture);
    }

    @Override
    public void glAttachShader(int program, int shader) {
        GLES20.glAttachShader(program, shader);
    }

    @Override
    public void glBindBuffer(int target, int buffer) {
        GLES20.glBindBuffer(target, buffer);
    }

    @Override
    public void glBindTexture(int target, int texture) {
        GLES20.glBindTexture(target, texture);
    }

    @Override
    public void glBlendFunc(int sfactor, int dfactor) {
        GLES20.glBlendFunc(sfactor, dfactor);
    }

    @Override
    public void glBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha) {
        GLES20.glBlendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha);
    }

    @Override
    public void glBufferData(int target, FloatBuffer data, int usage) {
        checkLimit(data);
        GLES20.glBufferData(target, data, usage);
    }

    @Override
    public void glBufferData(int target, ShortBuffer data, int usage) {
        checkLimit(data);
        GLES20.glBufferData(target, data, usage);
    }

    @Override
    public void glBufferData(int target, ByteBuffer data, int usage) {
        checkLimit(data);
        GLES20.glBufferData(target, data, usage);
    }

    @Override
    public void glBufferData(int target, IntBuffer data, int usage) {
        checkLimit(data);
        GLES20.glBufferData(target, data, usage);
    }

    @Override
    public void glBufferData(int target, long dataSize, int usage) {
        GLES20.glBufferData(target, dataSize, usage);
    }

    @Override
    public void glBufferSubData(int target, long offset, FloatBuffer data) {
        checkLimit(data);
        GLES20.glBufferSubData(target, offset, data);
    }

    @Override
    public void glBufferSubData(int target, long offset, ShortBuffer data) {
        checkLimit(data);
        GLES20.glBufferSubData(target, offset, data);
    }

    @Override
    public void glBufferSubData(int target, long offset, ByteBuffer data) {
        checkLimit(data);
        GLES20.glBufferSubData(target, offset, data);
    }

    @Override
    public void glBufferSubData(int target, long offset, IntBuffer data) {
        checkLimit(data);
        GLES20.glBufferSubData(target, offset, data);
    }

    @Override
    public void glGetBufferSubData(int target, long offset, ByteBuffer data) {
        throw new UnsupportedOperationException("OpenGL ES does not support glGetBufferSubData");
    }

    @Override
    public void glClear(int mask) {
        GLES20.glClear(mask);
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        GLES20.glClearColor(red, green, blue, alpha);
    }

    @Override
    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GLES20.glColorMask(red, green, blue, alpha);
    }

    @Override
    public void glCompileShader(int shader) {
        GLES20.glCompileShader(shader);
    }

    @Override
    public void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height,
            int border, ByteBuffer data) {
        checkLimit(data);
        GLES20.glCompressedTexImage2D(target, level, internalformat, width, height, border, data);
    }

    @Override
    public void glCompressedTexSubImage2D(int target, int level, int xoffset, int yoffset, int width,
            int height, int format, ByteBuffer data) {
        checkLimit(data);
        GLES20.glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, data);
    }

    @Override
    public int glCreateProgram() {
        return GLES20.glCreateProgram();
    }

    @Override
    public int glCreateShader(int shaderType) {
        return GLES20.glCreateShader(shaderType);
    }

    @Override
    public void glCullFace(int mode) {
        GLES20.glCullFace(mode);
    }

    @Override
    public void glDeleteBuffers(IntBuffer buffers) {
        checkLimit(buffers);
        GLES20.glDeleteBuffers(buffers);
    }

    @Override
    public void glDeleteProgram(int program) {
        GLES20.glDeleteProgram(program);
    }

    @Override
    public void glDeleteShader(int shader) {
        GLES20.glDeleteShader(shader);
    }

    @Override
    public void glDeleteTextures(IntBuffer textures) {
        checkLimit(textures);
        GLES20.glDeleteTextures(textures);
    }

    @Override
    public void glDepthFunc(int func) {
        GLES20.glDepthFunc(func);
    }

    @Override
    public void glDepthMask(boolean flag) {
        GLES20.glDepthMask(flag);
    }

    @Override
    public void glDepthRange(double nearVal, double farVal) {
        GLES20.glDepthRangef((float) nearVal, (float) farVal);
    }

    @Override
    public void glDetachShader(int program, int shader) {
        GLES20.glDetachShader(program, shader);
    }

    @Override
    public void glDisable(int cap) {
        GLES20.glDisable(cap);
    }

    @Override
    public void glDisableVertexAttribArray(int index) {
        GLES20.glDisableVertexAttribArray(index);
    }

    @Override
    public void glDrawArrays(int mode, int first, int count) {
        GLES20.glDrawArrays(mode, first, count);
    }

    @Override
    public void glDrawRangeElements(int mode, int start, int end, int count, int type, long indices) {
        GLES20.glDrawElements(mode, count, type, indices);
    }

    @Override
    public void glEnable(int cap) {
        GLES20.glEnable(cap);
    }

    @Override
    public void glEnableVertexAttribArray(int index) {
        GLES20.glEnableVertexAttribArray(index);
    }

    @Override
    public void glGenBuffers(IntBuffer buffers) {
        checkLimit(buffers);
        GLES20.glGenBuffers(buffers);
    }

    @Override
    public void glGenTextures(IntBuffer textures) {
        checkLimit(textures);
        GLES20.glGenTextures(textures);
    }

    @Override
    public int glGetAttribLocation(int program, String name) {
        return GLES20.glGetAttribLocation(program, name);
    }

    @Override
    public void glGetBoolean(int pname, ByteBuffer params) {
        checkLimit(params);
        GLES20.glGetBooleanv(pname, params);
    }

    @Override
    public int glGetError() {
        return GLES20.glGetError();
    }

    @Override
    public void glGetFloat(int parameterId, FloatBuffer storeValues) {
        checkLimit(storeValues);
        GLES20.glGetFloatv(parameterId, storeValues);
    }

    @Override
    public void glGetInteger(int pname, IntBuffer params) {
        checkLimit(params);
        GLES20.glGetIntegerv(pname, params);
    }

    @Override
    public void glGetProgram(int program, int pname, IntBuffer params) {
        checkLimit(params);
        GLES20.glGetProgramiv(program, pname, params);
    }

    @Override
    public String glGetProgramInfoLog(int program, int maxLength) {
        String s = GLES20.glGetProgramInfoLog(program);
        if (s == null) return "";
        return s.length() > maxLength ? s.substring(0, maxLength) : s;
    }

    @Override
    public void glGetShader(int shader, int pname, IntBuffer params) {
        checkLimit(params);
        GLES20.glGetShaderiv(shader, pname, params);
    }

    @Override
    public String glGetShaderInfoLog(int shader, int maxLength) {
        String s = GLES20.glGetShaderInfoLog(shader);
        if (s == null) return "";
        return s.length() > maxLength ? s.substring(0, maxLength) : s;
    }

    @Override
    public String glGetString(int name) {
        return GLES20.glGetString(name);
    }

    @Override
    public int glGetUniformLocation(int program, String name) {
        return GLES20.glGetUniformLocation(program, name);
    }

    @Override
    public boolean glIsEnabled(int cap) {
        return GLES20.glIsEnabled(cap);
    }

    @Override
    public void glLineWidth(float width) {
        GLES20.glLineWidth(width);
    }

    @Override
    public void glLinkProgram(int program) {
        GLES20.glLinkProgram(program);
    }

    @Override
    public void glPixelStorei(int pname, int param) {
        GLES20.glPixelStorei(pname, param);
    }

    @Override
    public void glPolygonOffset(float factor, float units) {
        GLES20.glPolygonOffset(factor, units);
    }

    @Override
    public void glReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer data) {
        checkLimit(data);
        GLES20.glReadPixels(x, y, width, height, format, type, data);
    }

    @Override
    public void glScissor(int x, int y, int width, int height) {
        GLES20.glScissor(x, y, width, height);
    }

    @Override
    public void glShaderSource(int shader, String[] string, IntBuffer length) {
        if (string == null || string.length != 1) {
            throw new UnsupportedOperationException("Expected exactly one shader source string");
        }
        GLES20.glShaderSource(shader, string[0]);
    }

    @Override
    public void glStencilFuncSeparate(int face, int func, int ref, int mask) {
        GLES20.glStencilFuncSeparate(face, func, ref, mask);
    }

    @Override
    public void glStencilOpSeparate(int face, int sfail, int dpfail, int dppass) {
        GLES20.glStencilOpSeparate(face, sfail, dpfail, dppass);
    }

    @Override
    public void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border,
            int format, int type, ByteBuffer data) {
        if (data != null) checkLimit(data);
        GLES20.glTexImage2D(target, level, internalFormat, width, height, border, format, type, data);
    }

    @Override
    public void glTexParameterf(int target, int pname, float param) {
        GLES20.glTexParameterf(target, pname, param);
    }

    @Override
    public void glTexParameteri(int target, int pname, int param) {
        GLES20.glTexParameteri(target, pname, param);
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height,
            int format, int type, ByteBuffer data) {
        checkLimit(data);
        GLES20.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, data);
    }

    @Override
    public void glUniform1(final int location, final FloatBuffer value) {
        checkLimit(value);
        GLES20.glUniform1fv(location, value);
    }

    @Override
    public void glUniform1(final int location, final IntBuffer value) {
        checkLimit(value);
        GLES20.glUniform1iv(location, value);
    }

    @Override
    public void glUniform1f(final int location, final float v0) {
        GLES20.glUniform1f(location, v0);
    }

    @Override
    public void glUniform1i(final int location, final int v0) {
        GLES20.glUniform1i(location, v0);
    }

    @Override
    public void glUniform2(final int location, final IntBuffer value) {
        checkLimit(value);
        GLES20.glUniform2iv(location, value);
    }

    @Override
    public void glUniform2(final int location, final FloatBuffer value) {
        checkLimit(value);
        GLES20.glUniform2fv(location, value);
    }

    @Override
    public void glUniform2f(final int location, final float v0, final float v1) {
        GLES20.glUniform2f(location, v0, v1);
    }

    @Override
    public void glUniform3(final int location, final IntBuffer value) {
        checkLimit(value);
        GLES20.glUniform3iv(location, value);
    }

    @Override
    public void glUniform3(final int location, final FloatBuffer value) {
        checkLimit(value);
        GLES20.glUniform3fv(location, value);
    }

    @Override
    public void glUniform3f(final int location, final float v0, final float v1, final float v2) {
        GLES20.glUniform3f(location, v0, v1, v2);
    }

    @Override
    public void glUniform4(final int location, final FloatBuffer value) {
        checkLimit(value);
        GLES20.glUniform4fv(location, value);
    }

    @Override
    public void glUniform4(final int location, final IntBuffer value) {
        checkLimit(value);
        GLES20.glUniform4iv(location, value);
    }

    @Override
    public void glUniform4f(final int location, final float v0, final float v1, final float v2, final float v3) {
        GLES20.glUniform4f(location, v0, v1, v2, v3);
    }

    @Override
    public void glUniformMatrix3(final int location, final boolean transpose, final FloatBuffer value) {
        checkLimit(value);
        GLES20.glUniformMatrix3fv(location, transpose, value);
    }

    @Override
    public void glUniformMatrix4(final int location, final boolean transpose, final FloatBuffer value) {
        checkLimit(value);
        GLES20.glUniformMatrix4fv(location, transpose, value);
    }


    @Override
    public void glUseProgram(int program) {
        GLES20.glUseProgram(program);
    }

    @Override
    public void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride,
            long pointer) {
        GLES20.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
    }

    @Override
    public void glViewport(int x, int y, int width, int height) {
        GLES20.glViewport(x, y, width, height);
    }


    @Override
    public void glBeginQuery(int target, int query) {
        GLES30.glBeginQuery(target, query);
    }

    @Override
    public void glEndQuery(int target) {
        GLES30.glEndQuery(target);
    }

    @Override
    public void glGenQueries(int num, IntBuffer buff) {
        checkLimit(buff);
        int oldLimit = buff.limit();
        int pos = buff.position();
        buff.limit(pos + num);
        GLES30.glGenQueries(buff);
        buff.limit(oldLimit);
    }

    @Override
    public int glGetQueryObjectiv(int query, int pname) {
        IntBuffer b = tmpBuff.clear();
        GLES30.glGetQueryObjectuiv(query, pname, b); 
        return b.get(0);
    }

    @Override
    public long glGetQueryObjectui64(int query, int pname) {
        IntBuffer b = tmpBuff.clear();
        GLES30.glGetQueryObjectuiv(query, pname, b);
        return b.get(0) & 0xFFFFFFFFL;
    }

    @Override
    public void glDrawArraysInstancedARB(int mode, int first, int count, int primcount) {
        GLES30.glDrawArraysInstanced(mode, first, count, primcount);
    }

    @Override
    public void glDrawElementsInstancedARB(int mode, int indicesCount, int type, long indicesBufferOffset,
            int primcount) {
        GLES30.glDrawElementsInstanced(mode, indicesCount, type, indicesBufferOffset, primcount);
    }

    @Override
    public void glVertexAttribDivisorARB(int index, int divisor) {
        GLES30.glVertexAttribDivisor(index, divisor);
    }

    @Override
    public void glDrawBuffers(IntBuffer bufs) {
        checkLimit(bufs);
        GLES30.glDrawBuffers(bufs);
    }

    @Override
    public void glBlitFramebufferEXT(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0,
            int dstX1, int dstY1, int mask, int filter) {
        GLES30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }

    @Override
    public void glReadBuffer(int mode) {
        GLES30.glReadBuffer(mode);
    }

    @Override
    public void glReadPixels(int x, int y, int width, int height, int format, int type, long offset) {
        GLES30.glReadPixels(x, y, width, height, format, type, offset);
    }


    @Override
    public void glBindFramebufferEXT(int target, int framebuffer) {
        GLES20.glBindFramebuffer(target, framebuffer);
    }

    @Override
    public void glBindRenderbufferEXT(int target, int renderbuffer) {
        GLES20.glBindRenderbuffer(target, renderbuffer);
    }

    @Override
    public int glCheckFramebufferStatusEXT(int target) {
        return GLES20.glCheckFramebufferStatus(target);
    }

    @Override
    public void glDeleteFramebuffersEXT(IntBuffer framebuffers) {
        checkLimit(framebuffers);
        GLES20.glDeleteFramebuffers(framebuffers);
    }

    @Override
    public void glDeleteRenderbuffersEXT(IntBuffer renderbuffers) {
        checkLimit(renderbuffers);
        GLES20.glDeleteRenderbuffers(renderbuffers);
    }

    @Override
    public void glFramebufferRenderbufferEXT(int target, int attachment, int renderbuffertarget,
            int renderbuffer) {
        GLES20.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
    }

    @Override
    public void glFramebufferTexture2DEXT(int target, int attachment, int textarget, int texture, int level) {
        GLES20.glFramebufferTexture2D(target, attachment, textarget, texture, level);
    }

    @Override
    public void glGenFramebuffersEXT(IntBuffer framebuffers) {
        checkLimit(framebuffers);
        GLES20.glGenFramebuffers(framebuffers);
    }

    @Override
    public void glGenRenderbuffersEXT(IntBuffer renderbuffers) {
        checkLimit(renderbuffers);
        GLES20.glGenRenderbuffers(renderbuffers);
    }

    @Override
    public void glGenerateMipmapEXT(int target) {
        GLES20.glGenerateMipmap(target);
    }

    @Override
    public void glRenderbufferStorageEXT(int target, int internalformat, int width, int height) {
        GLES20.glRenderbufferStorage(target, internalformat, width, height);
    }

    @Override
    public void glFramebufferTextureLayerEXT(int target, int attachment, int texture, int level, int layer) {
        GLES30.glFramebufferTextureLayer(target, attachment, texture, level, layer);
    }

    @Override
    public void glRenderbufferStorageMultisampleEXT(int target, int samples, int internalformat, int width,
            int height) {
        GLES30.glRenderbufferStorageMultisample(target, samples, internalformat, width, height);
    }


    @Override
    public void glGetMultisample(int pname, int index, FloatBuffer val) {
        checkLimit(val);
        GLES31.glGetMultisamplefv(pname, index, val);
    }

    @Override
    public void glTexImage2DMultisample(int target, int samples, int internalformat, int width, int height,
            boolean fixedSampleLocations) {
        GLES31.glTexStorage2DMultisample(target, samples, internalformat, width, height,
                fixedSampleLocations);
    }


    @Override
    public void glCompressedTexImage3D(int target, int level, int internalFormat, int width, int height,
            int depth, int border, ByteBuffer data) {
        checkLimit(data);
        GLES30.glCompressedTexImage3D(target, level, internalFormat, width, height, depth, border, data);
    }

    @Override
    public void glCompressedTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset,
            int width, int height, int depth, int format, ByteBuffer data) {
        checkLimit(data);
        GLES30.glCompressedTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth,
                format, data);
    }

    @Override
    public void glTexImage3D(int target, int level, int internalFormat, int width, int height, int depth,
            int border, int format, int type, ByteBuffer data) {
        if (data != null) checkLimit(data);
        GLES30.glTexImage3D(target, level, internalFormat, width, height, depth, border, format, type, data);
    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width,
            int height, int depth, int format, int type, ByteBuffer data) {
        checkLimit(data);
        GLES30.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type,
                data);
    }


    @Override
    public void glBindVertexArray(int array) {
        GLES30.glBindVertexArray(array);
    }

    @Override
    public void glDeleteVertexArrays(IntBuffer arrays) {
        checkLimit(arrays);
        GLES30.glDeleteVertexArrays(arrays);
    }

    @Override
    public void glGenVertexArrays(IntBuffer arrays) {
        checkLimit(arrays);
        GLES30.glGenVertexArrays(arrays);
    }


    @Override
    public void glAlphaFunc(int func, float ref) {
        // Not in GLES
    }

    @Override
    public void glPointSize(float size) {
        // Not core in GLES2
    }

    @Override
    public void glPolygonMode(int face, int mode) {
        // Not in GLES
    }

    @Override
    public void glBlendEquationSeparate(int colorMode, int alphaMode) {
        GLES20.glBlendEquationSeparate(colorMode, alphaMode);
    }

    @Override
    public void glDrawBuffer(int mode) {
        int nBuffers = (mode - GLFbo.GL_COLOR_ATTACHMENT0_EXT) + 1;
        if (nBuffers <= 0 || nBuffers > 16) {
            throw new IllegalArgumentException("Draw buffer outside range: " + Integer.toHexString(mode));
        }
        tmpBuff16.clear();
        for (int i = 0; i < nBuffers - 1; i++) {
            tmpBuff16.put(GL.GL_NONE);
        }
        tmpBuff16.put(mode);
        tmpBuff16.flip();
        glDrawBuffers(tmpBuff16);
    }


    @Override
    public int glClientWaitSync(Object sync, int flags, long timeout) {
        throw new UnsupportedOperationException("Sync fences not wired in this GLES wrapper yet");
    }

    @Override
    public void glDeleteSync(Object sync) {
        throw new UnsupportedOperationException("Sync fences not wired in this GLES wrapper yet");
    }

    @Override
    public Object glFenceSync(int condition, int flags) {
        throw new UnsupportedOperationException("Sync fences not wired in this GLES wrapper yet");
    }


    @Override
    public String glGetString(int name, int index) {
        return GLES30.glGetStringi(name, index);
    }


    @Override
    public void glGetBufferSubData(int target, long offset, IntBuffer data) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'glGetBufferSubData'");
    }
}
