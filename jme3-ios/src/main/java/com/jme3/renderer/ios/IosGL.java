/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
package com.jme3.renderer.ios;

import com.jme3.renderer.RendererException;
import com.jme3.renderer.opengl.GL;
import com.jme3.renderer.opengl.GLExt;
import com.jme3.renderer.opengl.GLFbo;
import java.nio.Buffer;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Implements OpenGL ES 2.0 for iOS. 
 * 
 * @author Kirill Vainer
 */
public class IosGL implements GL, GLExt, GLFbo {
    
    private final int[] temp_array = new int[16];
    
    public void resetStats() {
    }
    
    private static int getLimitBytes(ByteBuffer buffer) {
        checkLimit(buffer);
        return buffer.limit();
    }
    
    private static int getLimitBytes(ShortBuffer buffer) {
        checkLimit(buffer);
        return buffer.limit() * 2;
    }
    
    private static int getLimitBytes(IntBuffer buffer) {
        checkLimit(buffer);
        return buffer.limit() * 4;
    }
    
    private static int getLimitBytes(FloatBuffer buffer) {
        checkLimit(buffer);
        return buffer.limit() * 4;
    }
    
    private static int getLimitCount(Buffer buffer, int elementSize) {
        checkLimit(buffer);
        return buffer.limit() / elementSize;
    }

    @Override
    public void glBlendEquationSeparate(int colorMode, int alphaMode) {
        JmeIosGLES.glBlendEquationSeparate(colorMode, alphaMode);
    }
    
    private int toArray(IntBuffer buffer) {
        int remain = buffer.remaining();
        if (buffer.remaining() > 16) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int pos = buffer.position();
        buffer.get(temp_array, 0, remain);
        buffer.position(pos);
        return remain;
    }
    
    private void fromArray(int n, int[] array, IntBuffer buffer) {
        if (buffer.remaining() < n) { 
            throw new BufferOverflowException();
        }
        int pos = buffer.position();
        buffer.put(array, 0, n);
        buffer.position(pos);
    }
    
    private static void checkLimit(Buffer buffer) {
        if (buffer == null) {
            return;
        }
        if (buffer.limit() == 0) {
            throw new RendererException("Attempting to upload empty buffer (limit = 0), that's an error");
        }
        if (buffer.remaining() == 0) {
            throw new RendererException("Attempting to upload empty buffer (remaining = 0), that's an error");
        }
    }
    
    public void glActiveTexture(int texture) {
        JmeIosGLES.glActiveTexture(texture);
    }

    public void glAttachShader(int program, int shader) {
        JmeIosGLES.glAttachShader(program, shader);
    }

    @Override
    public void glBeginQuery(int target, int query) {
        throw new UnsupportedOperationException("Today is not a good day for this");
    }

    public void glBindBuffer(int target, int buffer) {
        JmeIosGLES.glBindBuffer(target, buffer);
    }

    public void glBindTexture(int target, int texture) {
        JmeIosGLES.glBindTexture(target, texture);
    }

    public void glBlendFunc(int sfactor, int dfactor) {
        JmeIosGLES.glBlendFunc(sfactor, dfactor);
    }
    
    public void glBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha) {
        JmeIosGLES.glBlendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha);
    }


    public void glBufferData(int target, FloatBuffer data, int usage) {
        JmeIosGLES.glBufferData(target, getLimitBytes(data), data, usage);
    }

    public void glBufferData(int target, ShortBuffer data, int usage) {
        JmeIosGLES.glBufferData(target, getLimitBytes(data), data, usage);
    }

    public void glBufferData(int target, ByteBuffer data, int usage) {
        JmeIosGLES.glBufferData(target, getLimitBytes(data), data, usage);
    }

    public void glBufferData(int target, long data_size, int usage) {
        JmeIosGLES.glBufferData(target, (int) data_size, null, usage);
    }

    public void glBufferSubData(int target, long offset, FloatBuffer data) {
        JmeIosGLES.glBufferSubData(target, (int) offset, getLimitBytes(data), data);
    }

    public void glBufferSubData(int target, long offset, ShortBuffer data) {
        JmeIosGLES.glBufferSubData(target, (int) offset, getLimitBytes(data), data);
    }

    public void glBufferSubData(int target, long offset, ByteBuffer data) {
        JmeIosGLES.glBufferSubData(target, (int) offset, getLimitBytes(data), data);
    }
    
    public void glGetBufferSubData(int target, long offset, ByteBuffer data) {
        throw new UnsupportedOperationException("OpenGL ES 2 does not support glGetBufferSubData");
    }

    public void glClear(int mask) {
        JmeIosGLES.glClear(mask);
    }

    public void glClearColor(float red, float green, float blue, float alpha) {
        JmeIosGLES.glClearColor(red, green, blue, alpha);
    }

    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        JmeIosGLES.glColorMask(red, green, blue, alpha);
    }

    public void glCompileShader(int shader) {
        JmeIosGLES.glCompileShader(shader);
    }

    public void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, ByteBuffer data) {
        JmeIosGLES.glCompressedTexImage2D(target, level, internalformat, width, height, 0, getLimitBytes(data), data);
    }

    public void glCompressedTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, ByteBuffer data) {
        JmeIosGLES.glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, getLimitBytes(data), data);
    }

    public int glCreateProgram() {
        return JmeIosGLES.glCreateProgram();
    }

    public int glCreateShader(int shaderType) {
        return JmeIosGLES.glCreateShader(shaderType);
    }

    public void glCullFace(int mode) {
        JmeIosGLES.glCullFace(mode);
    }

    public void glDeleteBuffers(IntBuffer buffers) {
        checkLimit(buffers);
        int n = toArray(buffers);
        JmeIosGLES.glDeleteBuffers(n, temp_array, 0);
    }

    public void glDeleteProgram(int program) {
        JmeIosGLES.glDeleteProgram(program);
    }

    public void glDeleteShader(int shader) {
        JmeIosGLES.glDeleteShader(shader);
    }

    public void glDeleteTextures(IntBuffer textures) {
        checkLimit(textures);
        int n = toArray(textures);
        JmeIosGLES.glDeleteTextures(n, temp_array, 0);
    }

    public void glDepthFunc(int func) {
        JmeIosGLES.glDepthFunc(func);
    }

    public void glDepthMask(boolean flag) {
        JmeIosGLES.glDepthMask(flag);
    }

    public void glDepthRange(double nearVal, double farVal) {
        JmeIosGLES.glDepthRangef((float)nearVal, (float)farVal);
    }

    public void glDetachShader(int program, int shader) {
        JmeIosGLES.glDetachShader(program, shader);
    }

    public void glDisable(int cap) {
        JmeIosGLES.glDisable(cap);
    }

    public void glDisableVertexAttribArray(int index) {
        JmeIosGLES.glDisableVertexAttribArray(index);
    }

    public void glDrawArrays(int mode, int first, int count) {
        JmeIosGLES.glDrawArrays(mode, first, count);
    }

    public void glDrawRangeElements(int mode, int start, int end, int count, int type, long indices) {
        JmeIosGLES.glDrawElementsIndex(mode, count, type, (int)indices);
    }

    public void glEnable(int cap) {
        JmeIosGLES.glEnable(cap);
    }

    public void glEnableVertexAttribArray(int index) {
        JmeIosGLES.glEnableVertexAttribArray(index);
    }

    @Override
    public void glEndQuery(int target) {
        throw new UnsupportedOperationException("Today is not a good day for this");
    }

    public void glGenBuffers(IntBuffer buffers) {
        checkLimit(buffers);
        JmeIosGLES.glGenBuffers(buffers.remaining(), temp_array, 0);
        fromArray(buffers.remaining(), temp_array, buffers);
    }

    public void glGenTextures(IntBuffer textures) {
        checkLimit(textures);
        JmeIosGLES.glGenTextures(textures.remaining(), temp_array, 0);
        fromArray(textures.remaining(), temp_array, textures);
    }

    @Override
    public void glGenQueries(int num, IntBuffer buff) {
        throw new UnsupportedOperationException("Today is not a good day for this");
    }

    public int glGetAttribLocation(int program, String name) {
        return JmeIosGLES.glGetAttribLocation(program, name);
    }

    public void glGetBoolean(int pname, ByteBuffer params) {
        // TODO: fix me!!!
        // JmeIosGLES.glGetBoolean(pname, params);
        throw new UnsupportedOperationException("Today is not a good day for this");
    }

    public int glGetError() {
        return JmeIosGLES.glGetError();
    }

    public void glGetInteger(int pname, IntBuffer params) {
        checkLimit(params);
        JmeIosGLES.glGetIntegerv(pname, temp_array, 0);
        fromArray(params.remaining(), temp_array, params);
    }

    public void glGetProgram(int program, int pname, IntBuffer params) {
        checkLimit(params);
        JmeIosGLES.glGetProgramiv(program, pname, temp_array, 0);
        fromArray(params.remaining(), temp_array, params);
    }

    public String glGetProgramInfoLog(int program, int maxLength) {
        return JmeIosGLES.glGetProgramInfoLog(program);
    }

    @Override
    public long glGetQueryObjectui64(int query, int pname) {
        throw new UnsupportedOperationException("Today is not a good day for this");
    }

    @Override
    public int glGetQueryObjectiv(int query, int pname) {
        throw new UnsupportedOperationException("Today is not a good day for this");
    }

    public void glGetShader(int shader, int pname, IntBuffer params) {
        checkLimit(params);
        JmeIosGLES.glGetShaderiv(shader, pname, temp_array, 0);
        fromArray(params.remaining(), temp_array, params);
    }

    public String glGetShaderInfoLog(int shader, int maxLength) {
        return JmeIosGLES.glGetShaderInfoLog(shader);
    }

    public String glGetString(int name) {
        return JmeIosGLES.glGetString(name);
    }

    public int glGetUniformLocation(int program, String name) {
        return JmeIosGLES.glGetUniformLocation(program, name);
    }

    public boolean glIsEnabled(int cap) {
        // TODO: fix me!!!
        if (cap == GLExt.GL_MULTISAMPLE_ARB) {
            return true;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public void glLineWidth(float width) {
        JmeIosGLES.glLineWidth(width);
    }

    public void glLinkProgram(int program) {
        JmeIosGLES.glLinkProgram(program);
    }

    public void glPixelStorei(int pname, int param) {
        JmeIosGLES.glPixelStorei(pname, param);
    }

    public void glPolygonOffset(float factor, float units) {
        JmeIosGLES.glPolygonOffset(factor, units);
    }

    public void glReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer data) {
        JmeIosGLES.glReadPixels(x, y, width, height, format, type, data);
    }

    public void glScissor(int x, int y, int width, int height) {
        JmeIosGLES.glScissor(x, y, width, height);
    }

    public void glShaderSource(int shader, String[] string, IntBuffer length) {
        if (string.length != 1) {
            throw new UnsupportedOperationException("Today is not a good day");
        }
        JmeIosGLES.glShaderSource(shader, string[0]);
    }

    public void glStencilFuncSeparate(int face, int func, int ref, int mask) {
        // TODO: fix me!!!
        // JmeIosGLES.glStencilFuncSeparate(face, func, ref, mask);
    }

    public void glStencilOpSeparate(int face, int sfail, int dpfail, int dppass) {
        // TODO: fix me!!!
        // JmeIosGLES.glStencilOpSeparate(face, sfail, dpfail, dppass);
    }

    public void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, ByteBuffer data) {
        JmeIosGLES.glTexImage2D(target, level, format, width, height, 0, format, type, data);
    }

    public void glTexParameterf(int target, int pname, float param) {
        // TODO: fix me!!!
        // JmeIosGLES.glTexParameterf(target, pname, param);
    }

    public void glTexParameteri(int target, int pname, int param) {
        JmeIosGLES.glTexParameteri(target, pname, param);
    }

    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, ByteBuffer data) {
        JmeIosGLES.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, data);
    }

    public void glUniform1(int location, FloatBuffer value) {
        JmeIosGLES.glUniform1fv(location, getLimitCount(value, 1), value);
    }

    public void glUniform1(int location, IntBuffer value) {
        JmeIosGLES.glUniform1iv(location, getLimitCount(value, 1), value);
    }

    public void glUniform1f(int location, float v0) {
        JmeIosGLES.glUniform1f(location, v0);
    }

    public void glUniform1i(int location, int v0) {
        JmeIosGLES.glUniform1i(location, v0);
    }

    public void glUniform2(int location, IntBuffer value) {
        // TODO: fix me!!!
        // JmeIosGLES.glUniform2iv(location, getLimitCount(value, 2), value);
        throw new UnsupportedOperationException();
    }

    public void glUniform2(int location, FloatBuffer value) {
        JmeIosGLES.glUniform2fv(location, getLimitCount(value, 2), value);
    }

    public void glUniform2f(int location, float v0, float v1) {
        JmeIosGLES.glUniform2f(location, v0, v1);
    }

    public void glUniform3(int location, IntBuffer value) {
        // TODO: fix me!!!
        // JmeIosGLES.glUniform3iv(location, getLimitCount(value, 3), value);
        throw new UnsupportedOperationException();
    }

    public void glUniform3(int location, FloatBuffer value) {
        JmeIosGLES.glUniform3fv(location, getLimitCount(value, 3), value);
    }

    public void glUniform3f(int location, float v0, float v1, float v2) {
        JmeIosGLES.glUniform3f(location, v0, v1, v2);
    }

    public void glUniform4(int location, FloatBuffer value) {
        JmeIosGLES.glUniform4fv(location, getLimitCount(value, 4), value);
    }

    public void glUniform4(int location, IntBuffer value) {
        // TODO: fix me!!!
        // JmeIosGLES.glUniform4iv(location, getLimitCount(value, 4), value);
        throw new UnsupportedOperationException();
    }

    public void glUniform4f(int location, float v0, float v1, float v2, float v3) {
        JmeIosGLES.glUniform4f(location, v0, v1, v2, v3);
    }

    public void glUniformMatrix3(int location, boolean transpose, FloatBuffer value) {
        JmeIosGLES.glUniformMatrix3fv(location, getLimitCount(value, 3 * 3), transpose, value);
    }

    public void glUniformMatrix4(int location, boolean transpose, FloatBuffer value) {
        JmeIosGLES.glUniformMatrix4fv(location, getLimitCount(value, 4 * 4), transpose, value);
    }

    public void glUseProgram(int program) {
        JmeIosGLES.glUseProgram(program);
    }

    public void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
        JmeIosGLES.glVertexAttribPointer2(index, size, type, normalized, stride, (int)pointer);
    }

    public void glViewport(int x, int y, int width, int height) {
        JmeIosGLES.glViewport(x, y, width, height);
    }

    public void glBlitFramebufferEXT(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        throw new UnsupportedOperationException("FBO blit not available on iOS");
    }

    public void glBufferData(int target, IntBuffer data, int usage) {
        JmeIosGLES.glBufferData(target, getLimitBytes(data), data, usage);
    }

    public void glBufferSubData(int target, long offset, IntBuffer data) {
        JmeIosGLES.glBufferSubData(target, (int)offset, getLimitBytes(data), data);
    }

    public void glDrawArraysInstancedARB(int mode, int first, int count, int primcount) {
        throw new UnsupportedOperationException("Instancing not available on iOS");
    }

    public void glDrawBuffers(IntBuffer bufs) {
        throw new UnsupportedOperationException("MRT not available on iOS");
    }

    public void glDrawElementsInstancedARB(int mode, int indices_count, int type, long indices_buffer_offset, int primcount) {
        throw new UnsupportedOperationException("Instancing not available on iOS");
    }

    public void glGetMultisample(int pname, int index, FloatBuffer val) {
        throw new UnsupportedOperationException("Multisample renderbuffers not available on iOS");
    }

    public void glRenderbufferStorageMultisampleEXT(int target, int samples, int internalformat, int width, int height) {
        throw new UnsupportedOperationException("Multisample renderbuffers not available on iOS");
    }

    public void glTexImage2DMultisample(int target, int samples, int internalformat, int width, int height, boolean fixedsamplelocations) {
        throw new UnsupportedOperationException("Multisample textures not available on iOS");
    }

    public void glVertexAttribDivisorARB(int index, int divisor) {
        throw new UnsupportedOperationException("Instancing not available on iOS");
    }

    public void glBindFramebufferEXT(int param1, int param2) {
        JmeIosGLES.glBindFramebuffer(param1, param2);
    }

    public void glBindRenderbufferEXT(int param1, int param2) {
        JmeIosGLES.glBindRenderbuffer(param1, param2);
    }

    public int glCheckFramebufferStatusEXT(int param1) {
        return JmeIosGLES.glCheckFramebufferStatus(param1);
    }

    public void glDeleteFramebuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        int n = toArray(param1);
        JmeIosGLES.glDeleteFramebuffers(n, temp_array, 0);
    }

    public void glDeleteRenderbuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        int n = toArray(param1);
        JmeIosGLES.glDeleteRenderbuffers(n, temp_array, 0);
    }

    public void glFramebufferRenderbufferEXT(int param1, int param2, int param3, int param4) {
        JmeIosGLES.glFramebufferRenderbuffer(param1, param2, param3, param4);
    }

    public void glFramebufferTexture2DEXT(int param1, int param2, int param3, int param4, int param5) {
        JmeIosGLES.glFramebufferTexture2D(param1, param2, param3, param4, param5);
    }

    public void glGenFramebuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        JmeIosGLES.glGenFramebuffers(param1.remaining(), temp_array, 0);
        fromArray(param1.remaining(), temp_array, param1);
    }

    public void glGenRenderbuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        JmeIosGLES.glGenRenderbuffers(param1.remaining(), temp_array, 0);
        fromArray(param1.remaining(), temp_array, param1);
    }

    public void glGenerateMipmapEXT(int param1) {
        JmeIosGLES.glGenerateMipmap(param1);
    }

    public void glRenderbufferStorageEXT(int param1, int param2, int param3, int param4) {
        JmeIosGLES.glRenderbufferStorage(param1, param2, param3, param4);
    }

    @Override
    public void glReadPixels(int x, int y, int width, int height, int format, int type, long offset) {
        // TODO: no offset???
        JmeIosGLES.glReadPixels(x, y, width, height, format, type, null);
    }

    @Override
    public int glClientWaitSync(Object sync, int flags, long timeout) {
        throw new UnsupportedOperationException("OpenGL ES 2 does not support sync fences");
    }

    @Override
    public void glDeleteSync(Object sync) {
        throw new UnsupportedOperationException("OpenGL ES 2 does not support sync fences");
    }

    @Override
    public Object glFenceSync(int condition, int flags) {
        throw new UnsupportedOperationException("OpenGL ES 2 does not support sync fences");
    }
}
