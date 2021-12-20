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
package com.jme3.renderer.ios;

import com.jme3.renderer.RendererException;
import com.jme3.renderer.opengl.GL;
import com.jme3.renderer.opengl.GL2;
import com.jme3.renderer.opengl.GLES_30;
import com.jme3.renderer.opengl.GLExt;
import com.jme3.renderer.opengl.GLFbo;
import com.jme3.util.BufferUtils;
import java.nio.Buffer;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Implements OpenGL ES 2.0 and 3.0 for iOS. 
 * 
 * @author Kirill Vainer, Jesus Oliver
 */
public class IosGL implements GL, GL2, GLES_30, GLExt, GLFbo {
    
    private final float[] tmpFloatArray = new float[16];
    private final int[] temp_array = new int[16];
    private final IntBuffer tmpBuff = BufferUtils.createIntBuffer(1);
    
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
    
    private void fromArray(int n, float[] array, FloatBuffer buffer) {
        if (buffer.remaining() < n) { 
            throw new BufferOverflowException();
        }
        int pos = buffer.position();
        buffer.put(array, 0, n);
        buffer.position(pos);
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
    
    @Override
    public void glActiveTexture(int texture) {
        JmeIosGLES.glActiveTexture(texture);
    }

    @Override
    public void glAttachShader(int program, int shader) {
        JmeIosGLES.glAttachShader(program, shader);
    }

    @Override
    public void glBeginQuery(int target, int query) {
        JmeIosGLES.glBeginQuery(target, query);
    }

    @Override
    public void glBindBuffer(int target, int buffer) {
        JmeIosGLES.glBindBuffer(target, buffer);
    }

    @Override
    public void glBindTexture(int target, int texture) {
        JmeIosGLES.glBindTexture(target, texture);
    }

    @Override
    public void glBlendFunc(int sFactor, int dFactor) {
        JmeIosGLES.glBlendFunc(sFactor, dFactor);
    }
    
    @Override
    public void glBlendFuncSeparate(int sFactorRGB, int dFactorRGB, int sFactorAlpha, int dFactorAlpha) {
        JmeIosGLES.glBlendFuncSeparate(sFactorRGB, dFactorRGB, sFactorAlpha, dFactorAlpha);
    }


    @Override
    public void glBufferData(int target, FloatBuffer data, int usage) {
        JmeIosGLES.glBufferData(target, getLimitBytes(data), data, usage);
    }

    @Override
    public void glBufferData(int target, ShortBuffer data, int usage) {
        JmeIosGLES.glBufferData(target, getLimitBytes(data), data, usage);
    }

    @Override
    public void glBufferData(int target, ByteBuffer data, int usage) {
        JmeIosGLES.glBufferData(target, getLimitBytes(data), data, usage);
    }

    @Override
    public void glBufferData(int target, long data_size, int usage) {
        JmeIosGLES.glBufferData(target, (int) data_size, null, usage);
    }

    @Override
    public void glBufferSubData(int target, long offset, FloatBuffer data) {
        JmeIosGLES.glBufferSubData(target, (int) offset, getLimitBytes(data), data);
    }

    @Override
    public void glBufferSubData(int target, long offset, ShortBuffer data) {
        JmeIosGLES.glBufferSubData(target, (int) offset, getLimitBytes(data), data);
    }

    @Override
    public void glBufferSubData(int target, long offset, ByteBuffer data) {
        JmeIosGLES.glBufferSubData(target, (int) offset, getLimitBytes(data), data);
    }
    
    @Override
    public void glGetBufferSubData(int target, long offset, ByteBuffer data) {
        throw new UnsupportedOperationException("OpenGL ES 2 does not support glGetBufferSubData");
    }

    @Override
    public void glClear(int mask) {
        JmeIosGLES.glClear(mask);
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        JmeIosGLES.glClearColor(red, green, blue, alpha);
    }

    @Override
    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        JmeIosGLES.glColorMask(red, green, blue, alpha);
    }

    @Override
    public void glCompileShader(int shader) {
        JmeIosGLES.glCompileShader(shader);
    }

    @Override
    public void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, ByteBuffer data) {
        JmeIosGLES.glCompressedTexImage2D(target, level, internalformat, width, height, 0, getLimitBytes(data), data);
    }

    @Override
    public void glCompressedTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, ByteBuffer data) {
        JmeIosGLES.glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, getLimitBytes(data), data);
    }

    @Override
    public int glCreateProgram() {
        return JmeIosGLES.glCreateProgram();
    }

    @Override
    public int glCreateShader(int shaderType) {
        return JmeIosGLES.glCreateShader(shaderType);
    }

    @Override
    public void glCullFace(int mode) {
        JmeIosGLES.glCullFace(mode);
    }

    @Override
    public void glDeleteBuffers(IntBuffer buffers) {
        checkLimit(buffers);
        int n = toArray(buffers);
        JmeIosGLES.glDeleteBuffers(n, temp_array, 0);
    }

    @Override
    public void glDeleteProgram(int program) {
        JmeIosGLES.glDeleteProgram(program);
    }

    @Override
    public void glDeleteShader(int shader) {
        JmeIosGLES.glDeleteShader(shader);
    }

    @Override
    public void glDeleteTextures(IntBuffer textures) {
        checkLimit(textures);
        int n = toArray(textures);
        JmeIosGLES.glDeleteTextures(n, temp_array, 0);
    }

    @Override
    public void glDepthFunc(int func) {
        JmeIosGLES.glDepthFunc(func);
    }

    @Override
    public void glDepthMask(boolean flag) {
        JmeIosGLES.glDepthMask(flag);
    }

    @Override
    public void glDepthRange(double nearVal, double farVal) {
        JmeIosGLES.glDepthRangef((float)nearVal, (float)farVal);
    }

    @Override
    public void glDetachShader(int program, int shader) {
        JmeIosGLES.glDetachShader(program, shader);
    }

    @Override
    public void glDisable(int cap) {
        JmeIosGLES.glDisable(cap);
    }

    @Override
    public void glDisableVertexAttribArray(int index) {
        JmeIosGLES.glDisableVertexAttribArray(index);
    }

    @Override
    public void glDrawArrays(int mode, int first, int count) {
        JmeIosGLES.glDrawArrays(mode, first, count);
    }

    @Override
    public void glDrawRangeElements(int mode, int start, int end, int count, int type, long indices) {
        JmeIosGLES.glDrawElementsIndex(mode, count, type, (int)indices);
    }

    @Override
    public void glEnable(int cap) {
        JmeIosGLES.glEnable(cap);
    }

    @Override
    public void glEnableVertexAttribArray(int index) {
        JmeIosGLES.glEnableVertexAttribArray(index);
    }

    @Override
    public void glEndQuery(int target) {
        JmeIosGLES.glEndQuery(target);
    }

    @Override
    public void glGenBuffers(IntBuffer buffers) {
        checkLimit(buffers);
        JmeIosGLES.glGenBuffers(buffers.remaining(), temp_array, 0);
        fromArray(buffers.remaining(), temp_array, buffers);
    }

    @Override
    public void glGenTextures(IntBuffer textures) {
        checkLimit(textures);
        JmeIosGLES.glGenTextures(textures.remaining(), temp_array, 0);
        fromArray(textures.remaining(), temp_array, textures);
    }

    @Override
    public void glGenQueries(int num, IntBuffer buff) {
        JmeIosGLES.glGenQueries(num, buff);
    }

    @Override
    public int glGetAttribLocation(int program, String name) {
        return JmeIosGLES.glGetAttribLocation(program, name);
    }

    @Override
    public void glGetBoolean(int pname, ByteBuffer params) {
        JmeIosGLES.glGetBoolean(pname, params);
    }

    @Override
    public int glGetError() {
        return JmeIosGLES.glGetError();
    }

    @Override
    public void glGetFloat(int parameterId, FloatBuffer storeValues) {
        checkLimit(storeValues);
        JmeIosGLES.glGetFloatv(parameterId, tmpFloatArray, 0);
        fromArray(storeValues.remaining(), tmpFloatArray, storeValues);
    }

    @Override
    public void glGetInteger(int pname, IntBuffer params) {
        checkLimit(params);
        JmeIosGLES.glGetIntegerv(pname, temp_array, 0);
        fromArray(params.remaining(), temp_array, params);
    }

    @Override
    public void glGetProgram(int program, int pname, IntBuffer params) {
        checkLimit(params);
        JmeIosGLES.glGetProgramiv(program, pname, temp_array, 0);
        fromArray(params.remaining(), temp_array, params);
    }

    @Override
    public String glGetProgramInfoLog(int program, int maxLength) {
        return JmeIosGLES.glGetProgramInfoLog(program);
    }

    @Override
    public long glGetQueryObjectui64(int query, int pname) {
        JmeIosGLES.glGetQueryObjectuiv(query, pname, temp_array);
        return temp_array[0];
    }

    @Override
    public int glGetQueryObjectiv(int query, int pname) {
        JmeIosGLES.glGetQueryiv(query, pname, temp_array);
        return temp_array[0];
    }

    @Override
    public void glGetShader(int shader, int pname, IntBuffer params) {
        checkLimit(params);
        JmeIosGLES.glGetShaderiv(shader, pname, temp_array, 0);
        fromArray(params.remaining(), temp_array, params);
    }

    @Override
    public String glGetShaderInfoLog(int shader, int maxLength) {
        return JmeIosGLES.glGetShaderInfoLog(shader);
    }

    @Override
    public String glGetString(int name) {
        return JmeIosGLES.glGetString(name);
    }

    @Override
    public int glGetUniformLocation(int program, String name) {
        return JmeIosGLES.glGetUniformLocation(program, name);
    }

    @Override
    public boolean glIsEnabled(int cap) {
        // kept this always returning true for compatibility
        if (cap == GLExt.GL_MULTISAMPLE_ARB) {
            return true;
        } else {
            return JmeIosGLES.glIsEnabled(cap);
        }
    }

    @Override
    public void glLineWidth(float width) {
        JmeIosGLES.glLineWidth(width);
    }

    @Override
    public void glLinkProgram(int program) {
        JmeIosGLES.glLinkProgram(program);
    }

    @Override
    public void glPixelStorei(int pname, int param) {
        JmeIosGLES.glPixelStorei(pname, param);
    }

    @Override
    public void glPolygonOffset(float factor, float units) {
        JmeIosGLES.glPolygonOffset(factor, units);
    }

    @Override
    public void glReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer data) {
        JmeIosGLES.glReadPixels(x, y, width, height, format, type, data);
    }

    @Override
    public void glScissor(int x, int y, int width, int height) {
        JmeIosGLES.glScissor(x, y, width, height);
    }

    @Override
    public void glShaderSource(int shader, String[] string, IntBuffer length) {
        if (string.length != 1) {
            throw new UnsupportedOperationException("Today is not a good day");
        }
        JmeIosGLES.glShaderSource(shader, string[0]);
    }

    @Override
    public void glStencilFuncSeparate(int face, int func, int ref, int mask) {
        JmeIosGLES.glStencilFuncSeparate(face, func, ref, mask);
    }

    @Override
    public void glStencilOpSeparate(int face, int sfail, int dpfail, int dppass) {
        JmeIosGLES.glStencilOpSeparate(face, sfail, dpfail, dppass);
    }

    @Override
    public void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, ByteBuffer data) {
        JmeIosGLES.glTexImage2D(target, level, internalFormat, width, height, 0, format, type, data);
    }

    @Override
    public void glTexParameterf(int target, int pname, float param) {
        JmeIosGLES.glTexParameterf(target, pname, param);
    }

    @Override
    public void glTexParameteri(int target, int pname, int param) {
        JmeIosGLES.glTexParameteri(target, pname, param);
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, ByteBuffer data) {
        JmeIosGLES.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, data);
    }

    @Override
    public void glUniform1(int location, FloatBuffer value) {
        JmeIosGLES.glUniform1fv(location, getLimitCount(value, 1), value);
    }

    @Override
    public void glUniform1(int location, IntBuffer value) {
        JmeIosGLES.glUniform1iv(location, getLimitCount(value, 1), value);
    }

    @Override
    public void glUniform1f(int location, float v0) {
        JmeIosGLES.glUniform1f(location, v0);
    }

    @Override
    public void glUniform1i(int location, int v0) {
        JmeIosGLES.glUniform1i(location, v0);
    }

    @Override
    public void glUniform2(int location, IntBuffer value) {
        // TODO: fix me!!!
        // JmeIosGLES.glUniform2iv(location, getLimitCount(value, 2), value);
        throw new UnsupportedOperationException();
    }

    @Override
    public void glUniform2(int location, FloatBuffer value) {
        JmeIosGLES.glUniform2fv(location, getLimitCount(value, 2), value);
    }

    @Override
    public void glUniform2f(int location, float v0, float v1) {
        JmeIosGLES.glUniform2f(location, v0, v1);
    }

    @Override
    public void glUniform3(int location, IntBuffer value) {
        // TODO: fix me!!!
        // JmeIosGLES.glUniform3iv(location, getLimitCount(value, 3), value);
        throw new UnsupportedOperationException();
    }

    @Override
    public void glUniform3(int location, FloatBuffer value) {
        JmeIosGLES.glUniform3fv(location, getLimitCount(value, 3), value);
    }

    @Override
    public void glUniform3f(int location, float v0, float v1, float v2) {
        JmeIosGLES.glUniform3f(location, v0, v1, v2);
    }

    @Override
    public void glUniform4(int location, FloatBuffer value) {
        JmeIosGLES.glUniform4fv(location, getLimitCount(value, 4), value);
    }

    @Override
    public void glUniform4(int location, IntBuffer value) {
        // TODO: fix me!!!
        // JmeIosGLES.glUniform4iv(location, getLimitCount(value, 4), value);
        throw new UnsupportedOperationException();
    }

    @Override
    public void glUniform4f(int location, float v0, float v1, float v2, float v3) {
        JmeIosGLES.glUniform4f(location, v0, v1, v2, v3);
    }

    @Override
    public void glUniformMatrix3(int location, boolean transpose, FloatBuffer value) {
        JmeIosGLES.glUniformMatrix3fv(location, getLimitCount(value, 3 * 3), transpose, value);
    }

    @Override
    public void glUniformMatrix4(int location, boolean transpose, FloatBuffer value) {
        JmeIosGLES.glUniformMatrix4fv(location, getLimitCount(value, 4 * 4), transpose, value);
    }

    @Override
    public void glUseProgram(int program) {
        JmeIosGLES.glUseProgram(program);
    }

    @Override
    public void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
        JmeIosGLES.glVertexAttribPointer2(index, size, type, normalized, stride, (int)pointer);
    }

    @Override
    public void glViewport(int x, int y, int width, int height) {
        JmeIosGLES.glViewport(x, y, width, height);
    }

    @Override
    public void glBlitFramebufferEXT(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        JmeIosGLES.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }

    @Override
    public void glBufferData(int target, IntBuffer data, int usage) {
        JmeIosGLES.glBufferData(target, getLimitBytes(data), data, usage);
    }

    @Override
    public void glBufferSubData(int target, long offset, IntBuffer data) {
        JmeIosGLES.glBufferSubData(target, (int)offset, getLimitBytes(data), data);
    }

    @Override
    public void glDrawArraysInstancedARB(int mode, int first, int count, int primcount) {
        JmeIosGLES.glDrawArraysInstanced(mode, first, count, primcount);
    }

    @Override
    public void glDrawBuffers(IntBuffer bufs) {
        JmeIosGLES.glDrawBuffers(getLimitBytes(bufs), bufs);
    }

    @Override
    public void glDrawElementsInstancedARB(int mode, int indices_count, int type, long indices_buffer_offset, int primcount) {
        JmeIosGLES.glDrawElementsInstanced(mode, indices_count, type, indices_buffer_offset, primcount);
    }

    @Override
    public void glGetMultisample(int pname, int index, FloatBuffer val) {
        throw new UnsupportedOperationException("Multisample renderbuffers not available on iOS");
    }

    @Override
    public void glRenderbufferStorageMultisampleEXT(int target, int samples, int internalformat, int width, int height) {
        throw new UnsupportedOperationException("Multisample renderbuffers not available on iOS");
    }

    @Override
    public void glTexImage2DMultisample(int target, int samples, int internalformat, int width, int height, boolean fixedSampleLocations) {
        throw new UnsupportedOperationException("Multisample textures not available on iOS");
    }

    @Override
    public void glVertexAttribDivisorARB(int index, int divisor) {
        JmeIosGLES.glVertexAttribDivisor(index, divisor);
    }

    @Override
    public void glBindFramebufferEXT(int param1, int param2) {
        JmeIosGLES.glBindFramebuffer(param1, param2);
    }

    @Override
    public void glBindRenderbufferEXT(int param1, int param2) {
        JmeIosGLES.glBindRenderbuffer(param1, param2);
    }

    @Override
    public int glCheckFramebufferStatusEXT(int param1) {
        return JmeIosGLES.glCheckFramebufferStatus(param1);
    }

    @Override
    public void glDeleteFramebuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        int n = toArray(param1);
        JmeIosGLES.glDeleteFramebuffers(n, temp_array, 0);
    }

    @Override
    public void glDeleteRenderbuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        int n = toArray(param1);
        JmeIosGLES.glDeleteRenderbuffers(n, temp_array, 0);
    }

    @Override
    public void glFramebufferRenderbufferEXT(int param1, int param2, int param3, int param4) {
        JmeIosGLES.glFramebufferRenderbuffer(param1, param2, param3, param4);
    }

    @Override
    public void glFramebufferTexture2DEXT(int param1, int param2, int param3, int param4, int param5) {
        JmeIosGLES.glFramebufferTexture2D(param1, param2, param3, param4, param5);
    }

    @Override
    public void glGenFramebuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        JmeIosGLES.glGenFramebuffers(param1.remaining(), temp_array, 0);
        fromArray(param1.remaining(), temp_array, param1);
    }

    @Override
    public void glGenRenderbuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        JmeIosGLES.glGenRenderbuffers(param1.remaining(), temp_array, 0);
        fromArray(param1.remaining(), temp_array, param1);
    }

    @Override
    public void glGenerateMipmapEXT(int param1) {
        JmeIosGLES.glGenerateMipmap(param1);
    }

    @Override
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
    
    @Override
    public void glFramebufferTextureLayerEXT(int target, int attachment, int texture, int level, int layer) {
        JmeIosGLES.glFramebufferTextureLayer(target, attachment, texture, level, layer);
    }

    // New methods from GL2 interface which are supported in GLES30
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
        ((Buffer)tmpBuff).clear();
        tmpBuff.put(0, mode);
        tmpBuff.rewind();
        glDrawBuffers(tmpBuff);
    }

    @Override
    public void glReadBuffer(int mode) {
        JmeIosGLES.glReadBuffer(mode);
    }

    @Override
    public void glCompressedTexImage3D(int target, int level, int internalFormat, int width, int height, int depth,
                                           int border, ByteBuffer data) {
        JmeIosGLES.glCompressedTexImage3D(target, level, internalFormat, width, height, depth, border, getLimitBytes(data), data);
    }

    @Override
    public void glCompressedTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width,
                                              int height, int depth, int format, ByteBuffer data) {
        JmeIosGLES.glCompressedTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, getLimitBytes(data), data);
    }

    @Override
    public void glTexImage3D(int target, int level, int internalFormat, int width, int height, int depth, int border,
                                 int format, int type, ByteBuffer data) {
        JmeIosGLES.glTexImage3D(target, level, internalFormat, width, height, depth, border, format, type, data);
    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height,
                                    int depth, int format, int type, ByteBuffer data) {
        JmeIosGLES.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, data);
    }


}
