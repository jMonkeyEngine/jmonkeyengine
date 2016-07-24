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
package com.jme3.renderer.android;

import android.opengl.GLES20;
import com.jme3.renderer.RendererException;
import com.jme3.renderer.opengl.GL;
import com.jme3.renderer.opengl.GLExt;
import com.jme3.renderer.opengl.GLFbo;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class AndroidGL implements GL, GLExt, GLFbo {

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
        GLES20.glActiveTexture(texture);
    }

    public void glAttachShader(int program, int shader) {
        GLES20.glAttachShader(program, shader);
    }

    public void glBindBuffer(int target, int buffer) {
        GLES20.glBindBuffer(target, buffer);
    }

    public void glBindTexture(int target, int texture) {
        GLES20.glBindTexture(target, texture);
    }

    public void glBlendFunc(int sfactor, int dfactor) {
        GLES20.glBlendFunc(sfactor, dfactor);
    }
    
    public void glBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha) {
       GLES20.glBlendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha);
    }

    public void glBufferData(int target, FloatBuffer data, int usage) {
        GLES20.glBufferData(target, getLimitBytes(data), data, usage);
    }

    public void glBufferData(int target, ShortBuffer data, int usage) {
        GLES20.glBufferData(target, getLimitBytes(data), data, usage);
    }

    public void glBufferData(int target, ByteBuffer data, int usage) {
        GLES20.glBufferData(target, getLimitBytes(data), data, usage);
    }

    public void glBufferData(int target, long data_size, int usage) {
        GLES20.glBufferData(target, (int) data_size, null, usage);
    }

    public void glBufferSubData(int target, long offset, FloatBuffer data) {
        GLES20.glBufferSubData(target, (int) offset, getLimitBytes(data), data);
    }

    public void glBufferSubData(int target, long offset, ShortBuffer data) {
        GLES20.glBufferSubData(target, (int) offset, getLimitBytes(data), data);
    }

    public void glBufferSubData(int target, long offset, ByteBuffer data) {
        GLES20.glBufferSubData(target, (int) offset, getLimitBytes(data), data);
    }

    public void glGetBufferSubData(int target, long offset, ByteBuffer data) {
        throw new UnsupportedOperationException("OpenGL ES 2 does not support glGetBufferSubData");
    }

    public void glClear(int mask) {
        GLES20.glClear(mask);
    }

    public void glClearColor(float red, float green, float blue, float alpha) {
        GLES20.glClearColor(red, green, blue, alpha);
    }

    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GLES20.glColorMask(red, green, blue, alpha);
    }

    public void glCompileShader(int shader) {
        GLES20.glCompileShader(shader);
    }

    public void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, ByteBuffer data) {
        GLES20.glCompressedTexImage2D(target, level, internalformat, width, height, 0, getLimitBytes(data), data);
    }

    public void glCompressedTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, ByteBuffer data) {
        GLES20.glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, getLimitBytes(data), data);
    }

    public int glCreateProgram() {
        return GLES20.glCreateProgram();
    }

    public int glCreateShader(int shaderType) {
        return GLES20.glCreateShader(shaderType);
    }

    public void glCullFace(int mode) {
        GLES20.glCullFace(mode);
    }

    public void glDeleteBuffers(IntBuffer buffers) {
        checkLimit(buffers);
        GLES20.glDeleteBuffers(buffers.limit(), buffers);
    }

    public void glDeleteProgram(int program) {
        GLES20.glDeleteProgram(program);
    }

    public void glDeleteShader(int shader) {
        GLES20.glDeleteShader(shader);
    }

    public void glDeleteTextures(IntBuffer textures) {
        checkLimit(textures);
        GLES20.glDeleteTextures(textures.limit(), textures);
    }

    public void glDepthFunc(int func) {
        GLES20.glDepthFunc(func);
    }

    public void glDepthMask(boolean flag) {
        GLES20.glDepthMask(flag);
    }

    public void glDepthRange(double nearVal, double farVal) {
        GLES20.glDepthRangef((float)nearVal, (float)farVal);
    }

    public void glDetachShader(int program, int shader) {
        GLES20.glDetachShader(program, shader);
    }

    public void glDisable(int cap) {
        GLES20.glDisable(cap);
    }

    public void glDisableVertexAttribArray(int index) {
        GLES20.glDisableVertexAttribArray(index);
    }

    public void glDrawArrays(int mode, int first, int count) {
        GLES20.glDrawArrays(mode, first, count);
    }

    public void glDrawRangeElements(int mode, int start, int end, int count, int type, long indices) {
        GLES20.glDrawElements(mode, count, type, (int)indices);
    }

    public void glEnable(int cap) {
        GLES20.glEnable(cap);
    }

    public void glEnableVertexAttribArray(int index) {
        GLES20.glEnableVertexAttribArray(index);
    }

    public void glGenBuffers(IntBuffer buffers) {
        checkLimit(buffers);
        GLES20.glGenBuffers(buffers.limit(), buffers);
    }

    public void glGenTextures(IntBuffer textures) {
        checkLimit(textures);
        GLES20.glGenTextures(textures.limit(), textures);
    }

    public int glGetAttribLocation(int program, String name) {
        return GLES20.glGetAttribLocation(program, name);
    }

    public void glGetBoolean(int pname, ByteBuffer params) {
        // GLES20.glGetBoolean(pname, params);
        throw new UnsupportedOperationException("Today is not a good day for this");
    }

    public int glGetError() {
        return GLES20.glGetError();
    }

    public void glGetInteger(int pname, IntBuffer params) {
        checkLimit(params);
        GLES20.glGetIntegerv(pname, params);
    }

    public void glGetProgram(int program, int pname, IntBuffer params) {
        checkLimit(params);
        GLES20.glGetProgramiv(program, pname, params);
    }

    public String glGetProgramInfoLog(int program, int maxLength) {
        return GLES20.glGetProgramInfoLog(program);
    }

    public void glGetShader(int shader, int pname, IntBuffer params) {
        checkLimit(params);
        GLES20.glGetShaderiv(shader, pname, params);
    }

    public String glGetShaderInfoLog(int shader, int maxLength) {
        return GLES20.glGetShaderInfoLog(shader);
    }

    public String glGetString(int name) {
        return GLES20.glGetString(name);
    }

    public int glGetUniformLocation(int program, String name) {
        return GLES20.glGetUniformLocation(program, name);
    }

    public boolean glIsEnabled(int cap) {
        return GLES20.glIsEnabled(cap);
    }

    public void glLineWidth(float width) {
        GLES20.glLineWidth(width);
    }

    public void glLinkProgram(int program) {
        GLES20.glLinkProgram(program);
    }

    public void glPixelStorei(int pname, int param) {
        GLES20.glPixelStorei(pname, param);
    }

    public void glPolygonOffset(float factor, float units) {
        GLES20.glPolygonOffset(factor, units);
    }

    public void glReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer data) {
        GLES20.glReadPixels(x, y, width, height, format, type, data);
    }

    public void glScissor(int x, int y, int width, int height) {
        GLES20.glScissor(x, y, width, height);
    }

    public void glShaderSource(int shader, String[] string, IntBuffer length) {
        if (string.length != 1) {
            throw new UnsupportedOperationException("Today is not a good day");
        }
        GLES20.glShaderSource(shader, string[0]);
    }

    public void glStencilFuncSeparate(int face, int func, int ref, int mask) {
        GLES20.glStencilFuncSeparate(face, func, ref, mask);
    }

    public void glStencilOpSeparate(int face, int sfail, int dpfail, int dppass) {
        GLES20.glStencilOpSeparate(face, sfail, dpfail, dppass);
    }

    public void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, ByteBuffer data) {
        GLES20.glTexImage2D(target, level, format, width, height, 0, format, type, data);
    }

    public void glTexParameterf(int target, int pname, float param) {
        GLES20.glTexParameterf(target, pname, param);
    }

    public void glTexParameteri(int target, int pname, int param) {
        GLES20.glTexParameteri(target, pname, param);
    }

    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, ByteBuffer data) {
        GLES20.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, data);
    }

    public void glUniform1(int location, FloatBuffer value) {
        GLES20.glUniform1fv(location, getLimitCount(value, 1), value);
    }

    public void glUniform1(int location, IntBuffer value) {
        GLES20.glUniform1iv(location, getLimitCount(value, 1), value);
    }

    public void glUniform1f(int location, float v0) {
        GLES20.glUniform1f(location, v0);
    }

    public void glUniform1i(int location, int v0) {
        GLES20.glUniform1i(location, v0);
    }

    public void glUniform2(int location, IntBuffer value) {
        GLES20.glUniform2iv(location, getLimitCount(value, 2), value);
    }

    public void glUniform2(int location, FloatBuffer value) {
        GLES20.glUniform2fv(location, getLimitCount(value, 2), value);
    }

    public void glUniform2f(int location, float v0, float v1) {
        GLES20.glUniform2f(location, v0, v1);
    }

    public void glUniform3(int location, IntBuffer value) {
        GLES20.glUniform3iv(location, getLimitCount(value, 3), value);
    }

    public void glUniform3(int location, FloatBuffer value) {
        GLES20.glUniform3fv(location, getLimitCount(value, 3), value);
    }

    public void glUniform3f(int location, float v0, float v1, float v2) {
        GLES20.glUniform3f(location, v0, v1, v2);
    }

    public void glUniform4(int location, FloatBuffer value) {
        GLES20.glUniform4fv(location, getLimitCount(value, 4), value);
    }

    public void glUniform4(int location, IntBuffer value) {
        GLES20.glUniform4iv(location, getLimitCount(value, 4), value);
    }

    public void glUniform4f(int location, float v0, float v1, float v2, float v3) {
        GLES20.glUniform4f(location, v0, v1, v2, v3);
    }

    public void glUniformMatrix3(int location, boolean transpose, FloatBuffer value) {
        GLES20.glUniformMatrix3fv(location, getLimitCount(value, 3 * 3), transpose, value);
    }

    public void glUniformMatrix4(int location, boolean transpose, FloatBuffer value) {
        GLES20.glUniformMatrix4fv(location, getLimitCount(value, 4 * 4), transpose, value);
    }

    public void glUseProgram(int program) {
        GLES20.glUseProgram(program);
    }

    public void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
        GLES20.glVertexAttribPointer(index, size, type, normalized, stride, (int)pointer);
    }

    public void glViewport(int x, int y, int width, int height) {
        GLES20.glViewport(x, y, width, height);
    }

    public void glBlitFramebufferEXT(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        throw new UnsupportedOperationException("FBO blit not available on Android");
    }

    public void glBufferData(int target, IntBuffer data, int usage) {
        GLES20.glBufferData(target, getLimitBytes(data), data, usage);
    }

    public void glBufferSubData(int target, long offset, IntBuffer data) {
        GLES20.glBufferSubData(target, (int)offset, getLimitBytes(data), data);
    }

    public void glDrawArraysInstancedARB(int mode, int first, int count, int primcount) {
        throw new UnsupportedOperationException("Instancing not available on Android");
    }

    public void glDrawBuffers(IntBuffer bufs) {
        throw new UnsupportedOperationException("MRT not available on Android");
    }

    public void glDrawElementsInstancedARB(int mode, int indices_count, int type, long indices_buffer_offset, int primcount) {
        throw new UnsupportedOperationException("Instancing not available on Android");
    }

    public void glGetMultisample(int pname, int index, FloatBuffer val) {
        throw new UnsupportedOperationException("Multisample renderbuffers not available on Android");
    }

    public void glRenderbufferStorageMultisampleEXT(int target, int samples, int internalformat, int width, int height) {
        throw new UnsupportedOperationException("Multisample renderbuffers not available on Android");
    }

    public void glTexImage2DMultisample(int target, int samples, int internalformat, int width, int height, boolean fixedsamplelocations) {
        throw new UnsupportedOperationException("Multisample textures not available on Android");
    }

    public void glVertexAttribDivisorARB(int index, int divisor) {
        throw new UnsupportedOperationException("Instancing not available on Android");
    }

    public void glBindFramebufferEXT(int param1, int param2) {
        GLES20.glBindFramebuffer(param1, param2);
    }

    public void glBindRenderbufferEXT(int param1, int param2) {
        GLES20.glBindRenderbuffer(param1, param2);
    }

    public int glCheckFramebufferStatusEXT(int param1) {
        return GLES20.glCheckFramebufferStatus(param1);
    }

    public void glDeleteFramebuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        GLES20.glDeleteFramebuffers(param1.limit(), param1);
    }

    public void glDeleteRenderbuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        GLES20.glDeleteRenderbuffers(param1.limit(), param1);
    }

    public void glFramebufferRenderbufferEXT(int param1, int param2, int param3, int param4) {
        GLES20.glFramebufferRenderbuffer(param1, param2, param3, param4);
    }

    public void glFramebufferTexture2DEXT(int param1, int param2, int param3, int param4, int param5) {
        GLES20.glFramebufferTexture2D(param1, param2, param3, param4, param5);
    }

    public void glGenFramebuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        GLES20.glGenFramebuffers(param1.limit(), param1);
    }

    public void glGenRenderbuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        GLES20.glGenRenderbuffers(param1.limit(), param1);
    }

    public void glGenerateMipmapEXT(int param1) {
        GLES20.glGenerateMipmap(param1);
    }

    public void glRenderbufferStorageEXT(int param1, int param2, int param3, int param4) {
        GLES20.glRenderbufferStorage(param1, param2, param3, param4);
    }

    @Override
    public void glReadPixels(int x, int y, int width, int height, int format, int type, long offset) {
        // TODO: no offset???
        GLES20.glReadPixels(x, y, width, height, format, type, null);
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

    @Override
    public void glBlendEquationSeparate(int colorMode, int alphaMode) {
        GLES20.glBlendEquationSeparate(colorMode, alphaMode);
    }
}
