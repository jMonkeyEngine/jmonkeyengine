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
package com.jme3.renderer.android;

import android.opengl.*;
import com.jme3.renderer.RendererException;
import com.jme3.renderer.opengl.*;
import com.jme3.util.BufferUtils;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class AndroidGL implements GL, GL2, GLES_30, GLExt, GLFbo {

    IntBuffer tmpBuff = BufferUtils.createIntBuffer(1);

    @Override
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

    @Override
    public void glActiveTexture(int texture) {
        GLES20.glActiveTexture(texture);
    }

    @Override
    public void glAttachShader(int program, int shader) {
        GLES20.glAttachShader(program, shader);
    }

    @Override
    public void glBeginQuery(int target, int query) {
        GLES30.glBeginQuery(target, query);
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
        GLES20.glBufferData(target, getLimitBytes(data), data, usage);
    }

    @Override
    public void glBufferData(int target, ShortBuffer data, int usage) {
        GLES20.glBufferData(target, getLimitBytes(data), data, usage);
    }

    @Override
    public void glBufferData(int target, ByteBuffer data, int usage) {
        GLES20.glBufferData(target, getLimitBytes(data), data, usage);
    }

    @Override
    public void glBufferData(int target, long dataSize, int usage) {
        GLES20.glBufferData(target, (int) dataSize, null, usage);
    }

    @Override
    public void glBufferSubData(int target, long offset, FloatBuffer data) {
        GLES20.glBufferSubData(target, (int) offset, getLimitBytes(data), data);
    }

    @Override
    public void glBufferSubData(int target, long offset, ShortBuffer data) {
        GLES20.glBufferSubData(target, (int) offset, getLimitBytes(data), data);
    }

    @Override
    public void glBufferSubData(int target, long offset, ByteBuffer data) {
        GLES20.glBufferSubData(target, (int) offset, getLimitBytes(data), data);
    }

    @Override
    public void glGetBufferSubData(int target, long offset, ByteBuffer data) {
        throw new UnsupportedOperationException("OpenGL ES 2 does not support glGetBufferSubData");
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
    public void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, ByteBuffer data) {
        GLES20.glCompressedTexImage2D(target, level, internalformat, width, height, 0, getLimitBytes(data), data);
    }

    @Override
    public void glCompressedTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, ByteBuffer data) {
        GLES20.glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, getLimitBytes(data), data);
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
        GLES20.glDeleteBuffers(buffers.limit(), buffers);
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
        GLES20.glDeleteTextures(textures.limit(), textures);
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
        GLES20.glDepthRangef((float)nearVal, (float)farVal);
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
        GLES20.glDrawElements(mode, count, type, (int)indices);
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
    public void glEndQuery(int target) {
        GLES30.glEndQuery(target);
    }

    @Override
    public void glGenBuffers(IntBuffer buffers) {
        checkLimit(buffers);
        GLES20.glGenBuffers(buffers.limit(), buffers);
    }

    @Override
    public void glGenTextures(IntBuffer textures) {
        checkLimit(textures);
        GLES20.glGenTextures(textures.limit(), textures);
    }

    @Override
    public void glGenQueries(int num, IntBuffer buff) {
        GLES30.glGenQueries(num, buff);
    }

    @Override
    public int glGetAttribLocation(int program, String name) {
        return GLES20.glGetAttribLocation(program, name);
    }

    @Override
    public void glGetBoolean(int pname, ByteBuffer params) {
        // GLES20.glGetBoolean(pname, params);
        throw new UnsupportedOperationException("Today is not a good day for this");
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
        return GLES20.glGetProgramInfoLog(program);
    }

    @Override
    public long glGetQueryObjectui64(int query, int pname) {
        IntBuffer buff = IntBuffer.allocate(1);
        //FIXME This is wrong IMO should be glGetQueryObjectui64v with a LongBuffer but it seems the API doesn't provide it.
        GLES30.glGetQueryObjectuiv(query, pname, buff);
        return buff.get(0);
    }

    @Override
    public int glGetQueryObjectiv(int query, int pname) {
        IntBuffer buff = IntBuffer.allocate(1);
        GLES30.glGetQueryiv(query, pname, buff);
        return buff.get(0);
    }

    @Override
    public void glGetShader(int shader, int pname, IntBuffer params) {
        checkLimit(params);
        GLES20.glGetShaderiv(shader, pname, params);
    }

    @Override
    public String glGetShaderInfoLog(int shader, int maxLength) {
        return GLES20.glGetShaderInfoLog(shader);
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
        GLES20.glReadPixels(x, y, width, height, format, type, data);
    }

    @Override
    public void glScissor(int x, int y, int width, int height) {
        GLES20.glScissor(x, y, width, height);
    }

    @Override
    public void glShaderSource(int shader, String[] string, IntBuffer length) {
        if (string.length != 1) {
            throw new UnsupportedOperationException("Today is not a good day");
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
    public void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, ByteBuffer data) {
        GLES20.glTexImage2D(target, level, internalFormat, width, height, 0, format, type, data);
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
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, ByteBuffer data) {
        GLES20.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, data);
    }

    @Override
    public void glUniform1(int location, FloatBuffer value) {
        GLES20.glUniform1fv(location, getLimitCount(value, 1), value);
    }

    @Override
    public void glUniform1(int location, IntBuffer value) {
        GLES20.glUniform1iv(location, getLimitCount(value, 1), value);
    }

    @Override
    public void glUniform1f(int location, float v0) {
        GLES20.glUniform1f(location, v0);
    }

    @Override
    public void glUniform1i(int location, int v0) {
        GLES20.glUniform1i(location, v0);
    }

    @Override
    public void glUniform2(int location, IntBuffer value) {
        GLES20.glUniform2iv(location, getLimitCount(value, 2), value);
    }

    @Override
    public void glUniform2(int location, FloatBuffer value) {
        GLES20.glUniform2fv(location, getLimitCount(value, 2), value);
    }

    @Override
    public void glUniform2f(int location, float v0, float v1) {
        GLES20.glUniform2f(location, v0, v1);
    }

    @Override
    public void glUniform3(int location, IntBuffer value) {
        GLES20.glUniform3iv(location, getLimitCount(value, 3), value);
    }

    @Override
    public void glUniform3(int location, FloatBuffer value) {
        GLES20.glUniform3fv(location, getLimitCount(value, 3), value);
    }

    @Override
    public void glUniform3f(int location, float v0, float v1, float v2) {
        GLES20.glUniform3f(location, v0, v1, v2);
    }

    @Override
    public void glUniform4(int location, FloatBuffer value) {
        GLES20.glUniform4fv(location, getLimitCount(value, 4), value);
    }

    @Override
    public void glUniform4(int location, IntBuffer value) {
        GLES20.glUniform4iv(location, getLimitCount(value, 4), value);
    }

    @Override
    public void glUniform4f(int location, float v0, float v1, float v2, float v3) {
        GLES20.glUniform4f(location, v0, v1, v2, v3);
    }

    @Override
    public void glUniformMatrix3(int location, boolean transpose, FloatBuffer value) {
        GLES20.glUniformMatrix3fv(location, getLimitCount(value, 3 * 3), transpose, value);
    }

    @Override
    public void glUniformMatrix4(int location, boolean transpose, FloatBuffer value) {
        GLES20.glUniformMatrix4fv(location, getLimitCount(value, 4 * 4), transpose, value);
    }

    @Override
    public void glUseProgram(int program) {
        GLES20.glUseProgram(program);
    }

    @Override
    public void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
        GLES20.glVertexAttribPointer(index, size, type, normalized, stride, (int)pointer);
    }

    @Override
    public void glViewport(int x, int y, int width, int height) {
        GLES20.glViewport(x, y, width, height);
    }

    @Override
    public void glBlitFramebufferEXT(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        GLES30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }

    @Override
    public void glBufferData(int target, IntBuffer data, int usage) {
        GLES20.glBufferData(target, getLimitBytes(data), data, usage);
    }

    @Override
    public void glBufferSubData(int target, long offset, IntBuffer data) {
        GLES20.glBufferSubData(target, (int)offset, getLimitBytes(data), data);
    }

    @Override
    public void glDrawArraysInstancedARB(int mode, int first, int count, int primcount) {
        GLES30.glDrawArraysInstanced(mode, first, count, primcount);
    }

    @Override
    public void glDrawBuffers(IntBuffer bufs) {
        GLES30.glDrawBuffers(bufs.limit(), bufs);
    }

    @Override
    public void glDrawElementsInstancedARB(int mode, int indicesCount, int type, long indicesBufferOffset, int primcount) {
        GLES30.glDrawElementsInstanced(mode, indicesCount, type, (int)indicesBufferOffset, primcount);
    }

    @Override
    public void glGetMultisample(int pname, int index, FloatBuffer val) {
        GLES31.glGetMultisamplefv(pname, index, val);
    }

    @Override
    public void glRenderbufferStorageMultisampleEXT(int target, int samples, int internalformat, int width, int height) {
        GLES30.glRenderbufferStorageMultisample(target, samples, internalformat, width, height);
    }

    @Override
    public void glTexImage2DMultisample(int target, int samples, int internalformat, int width, int height, boolean fixedSampleLocations) {
        GLES31.glTexStorage2DMultisample(target, samples, internalformat, width, height, fixedSampleLocations);
    }

    @Override
    public void glVertexAttribDivisorARB(int index, int divisor) {
        GLES30.glVertexAttribDivisor(index, divisor);
    }

    @Override
    public void glBindFramebufferEXT(int param1, int param2) {
        GLES20.glBindFramebuffer(param1, param2);
    }

    @Override
    public void glBindRenderbufferEXT(int param1, int param2) {
        GLES20.glBindRenderbuffer(param1, param2);
    }

    @Override
    public int glCheckFramebufferStatusEXT(int param1) {
        return GLES20.glCheckFramebufferStatus(param1);
    }

    @Override
    public void glDeleteFramebuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        GLES20.glDeleteFramebuffers(param1.limit(), param1);
    }

    @Override
    public void glDeleteRenderbuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        GLES20.glDeleteRenderbuffers(param1.limit(), param1);
    }

    @Override
    public void glFramebufferRenderbufferEXT(int param1, int param2, int param3, int param4) {
        GLES20.glFramebufferRenderbuffer(param1, param2, param3, param4);
    }

    @Override
    public void glFramebufferTexture2DEXT(int param1, int param2, int param3, int param4, int param5) {
        GLES20.glFramebufferTexture2D(param1, param2, param3, param4, param5);
    }

    @Override
    public void glGenFramebuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        GLES20.glGenFramebuffers(param1.limit(), param1);
    }

    @Override
    public void glGenRenderbuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        GLES20.glGenRenderbuffers(param1.limit(), param1);
    }

    @Override
    public void glGenerateMipmapEXT(int param1) {
        GLES20.glGenerateMipmap(param1);
    }

    @Override
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
    
    @Override
    public void glFramebufferTextureLayerEXT(int target, int attachment, int texture, int level, int layer) {
        GLES30.glFramebufferTextureLayer(target, attachment, texture, level, layer);
    }

    @Override
    public void glAlphaFunc(int func, float ref) {
    }
    
    @Override
    public void glPointSize(float size) {
    }

    @Override
    public void glPolygonMode(int face, int mode) {
    }

    // Wrapper to DrawBuffers as there's no DrawBuffer method in GLES
    @Override
    public void glDrawBuffer(int mode) {
        tmpBuff.clear();
        tmpBuff.put(0, mode);
        tmpBuff.rewind();
        glDrawBuffers(tmpBuff);
    }

    @Override
    public void glReadBuffer(int mode) {
        GLES30.glReadBuffer(mode);
    }

    @Override
    public void glCompressedTexImage3D(int target, int level, int internalFormat, int width, int height, int depth,
                                           int border, ByteBuffer data) {
        GLES30.glCompressedTexImage3D(target, level, internalFormat, width, height, depth, border, getLimitBytes(data), data);
    }

    @Override
    public void glCompressedTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width,
                                              int height, int depth, int format, ByteBuffer data) {
        GLES30.glCompressedTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, getLimitBytes(data), data);
    }

    @Override
    public void glTexImage3D(int target, int level, int internalFormat, int width, int height, int depth, int border,
                                 int format, int type, ByteBuffer data) {
        GLES30.glTexImage3D(target, level, internalFormat, width, height, depth, border, format, type, data);
    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height,
                                    int depth, int format, int type, ByteBuffer data) {
        GLES30.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, data);
    }

}

