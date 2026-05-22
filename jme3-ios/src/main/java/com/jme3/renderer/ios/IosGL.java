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
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import org.ngengine.libjglios.gles.GLES;

/**
 * Implements OpenGL ES 2.0 and 3.0 for iOS. 
 * 
 * @author Kirill Vainer, Jesus Oliver
 */
public class IosGL implements GL, GL2, GLES_30, GLExt, GLFbo {
    
    private float[] tmpFloatArray = new float[16];
    private int[] tempArray = new int[16];
    private final IntBuffer tmpBuff = BufferUtils.createIntBuffer(1);
    private byte[] tempByteArray = new byte[256];
    
    @Override
    public void resetStats() {
    }

    @Override
    public boolean supportsGpuTimerQuery() {
        return false;
    }

    @Override
    public void glBlendEquationSeparate(int colorMode, int alphaMode) {
        GLES.glBlendEquationSeparate(colorMode, alphaMode);
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

    private static long getSyncHandle(Object sync) {
        if (!(sync instanceof Long)) {
            throw new IllegalArgumentException("Expected a sync object returned by glFenceSync");
        }
        return (Long) sync;
    }

    private byte[] stringToCAscii(String value) {
        if (tempByteArray.length < value.length() + 1) {
            tempByteArray = new byte[value.length() + 1];
        }

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch > 0x7f) {
                throw new IllegalArgumentException("GL names must be ASCII");
            }
            tempByteArray[i] = (byte) ch;
        }

        tempByteArray[value.length()] = 0;
        return tempByteArray;
    }
    
    @Override
    public void glActiveTexture(int texture) {
        GLES.glActiveTexture(texture);
    }

    @Override
    public void glAttachShader(int program, int shader) {
        GLES.glAttachShader(program, shader);
    }

    @Override
    public void glBeginQuery(int target, int query) {
        if (target == GL.GL_TIME_ELAPSED) {
            throw new UnsupportedOperationException(
                    "64-bit GPU timer queries are not implemented by the iOS GLES binding");
        }
        GLES.glBeginQuery(target, query);
    }

    @Override
    public void glBindBuffer(int target, int buffer) {
        GLES.glBindBuffer(target, buffer);
    }

    @Override
    public void glBindTexture(int target, int texture) {
        GLES.glBindTexture(target, texture);
    }

    @Override
    public void glBlendFunc(int sFactor, int dFactor) {
        GLES.glBlendFunc(sFactor, dFactor);
    }
    
    @Override
    public void glBlendFuncSeparate(int sFactorRGB, int dFactorRGB, int sFactorAlpha, int dFactorAlpha) {
        GLES.glBlendFuncSeparate(sFactorRGB, dFactorRGB, sFactorAlpha, dFactorAlpha);
    }


    @Override
    public void glBufferData(int target, FloatBuffer data, int usage) {
        if (data == null) {
            return;
        }
        checkLimit(data);
        GLES.glBufferData(target, data.remaining() * 4L, data, data.position() * 4L, usage);
    }

    @Override
    public void glBufferData(int target, ShortBuffer data, int usage) {
        if (data == null) {
            return;
        }
        checkLimit(data);
        GLES.glBufferData(target, data.remaining() * 2L, data, data.position() * 2L, usage);
    }

    @Override
    public void glBufferData(int target, ByteBuffer data, int usage) {
        if (data == null) {
            return;
        }
        checkLimit(data);
        GLES.glBufferData(target, data.remaining(), data, data.position(), usage);
    }

    @Override
    public void glBufferData(int target, long dataSize, int usage) {
        GLES.glBufferData(target, dataSize, (Buffer) null, 0L, usage);
    }

    @Override
    public void glBufferSubData(int target, long offset, FloatBuffer data) {
        if (data == null) {
            return;
        }
        checkLimit(data);
        GLES.glBufferSubData(target, offset, data.remaining() * 4L, data, data.position() * 4L);
    }

    @Override
    public void glBufferSubData(int target, long offset, ShortBuffer data) {
        if (data == null) {
            return;
        }
        checkLimit(data);
        GLES.glBufferSubData(target, offset, data.remaining() * 2L, data, data.position() * 2L);
    }

    @Override
    public void glBufferSubData(int target, long offset, ByteBuffer data) {
        if (data == null) {
            return;
        }
        checkLimit(data);
        GLES.glBufferSubData(target, offset, data.remaining(), data, data.position());
    }
    
    @Override
    public void glGetBufferSubData(int target, long offset, ByteBuffer data) {
        checkLimit(data);
        GLES.glGetBufferSubData(target, offset, data.remaining(), data, data.position());
    }

    @Override
    public void glGetBufferSubData(int target, long offset, IntBuffer data) {
        checkLimit(data);
        GLES.glGetBufferSubData(target, offset, data.remaining() * 4L, data, data.position() * 4L);
    }

    @Override
    public void glClear(int mask) {
        GLES.glClear(mask);
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        GLES.glClearColor(red, green, blue, alpha);
    }

    @Override
    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GLES.glColorMask((byte) (red ? 1 : 0), (byte) (green ? 1 : 0),
                (byte) (blue ? 1 : 0), (byte) (alpha ? 1 : 0));
    }

    @Override
    public void glCompileShader(int shader) {
        GLES.glCompileShader(shader);
    }

    @Override
    public void glCompressedTexImage2D(int target, int level, int internalformat,
            int width, int height, int border, ByteBuffer data) {
        if (data == null) {
            return;
        }
        checkLimit(data);
        GLES.glCompressedTexImage2D(target, level, internalformat, width, height, 0,
                data.remaining(), data, data.position());
    }

    @Override
    public void glCompressedTexSubImage2D(int target, int level, int xoffset,
            int yoffset, int width, int height, int format, ByteBuffer data) {
        if (data == null) {
            return;
        }
        checkLimit(data);
        GLES.glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height,
                format, data.remaining(), data, data.position());
    }

    @Override
    public int glCreateProgram() {
        return GLES.glCreateProgram();
    }

    @Override
    public int glCreateShader(int shaderType) {
        return GLES.glCreateShader(shaderType);
    }

    @Override
    public void glCullFace(int mode) {
        GLES.glCullFace(mode);
    }

    @Override
    public void glDeleteBuffers(IntBuffer buffers) {
        checkLimit(buffers);
        GLES.glDeleteBuffers(buffers.remaining(), buffers, buffers.position() * 4L);
    }

    @Override
    public void glDeleteProgram(int program) {
        GLES.glDeleteProgram(program);
    }

    @Override
    public void glDeleteShader(int shader) {
        GLES.glDeleteShader(shader);
    }

    @Override
    public void glDeleteTextures(IntBuffer textures) {
        checkLimit(textures);
        GLES.glDeleteTextures(textures.remaining(), textures, textures.position() * 4L);
    }

    @Override
    public void glDepthFunc(int func) {
        GLES.glDepthFunc(func);
    }

    @Override
    public void glDepthMask(boolean flag) {
        GLES.glDepthMask((byte) (flag ? 1 : 0));
    }

    @Override
    public void glDepthRange(double nearVal, double farVal) {
        GLES.glDepthRangef((float) nearVal, (float) farVal);
    }

    @Override
    public void glDetachShader(int program, int shader) {
        GLES.glDetachShader(program, shader);
    }

    @Override
    public void glDisable(int cap) {
        GLES.glDisable(cap);
    }

    @Override
    public void glDisableVertexAttribArray(int index) {
        GLES.glDisableVertexAttribArray(index);
    }

    @Override
    public void glDrawArrays(int mode, int first, int count) {
        GLES.glDrawArrays(mode, first, count);
    }

    @Override
    public void glDrawRangeElements(int mode, int start, int end, int count, int type, long indices) {
        GLES.glDrawRangeElements(mode, start, end, count, type, indices);
    }

    @Override
    public void glEnable(int cap) {
        GLES.glEnable(cap);
    }

    @Override
    public void glEnableVertexAttribArray(int index) {
        GLES.glEnableVertexAttribArray(index);
    }

    @Override
    public void glEndQuery(int target) {
        if (target == GL.GL_TIME_ELAPSED) {
            throw new UnsupportedOperationException(
                    "64-bit GPU timer queries are not implemented by the iOS GLES binding");
        }
        GLES.glEndQuery(target);
    }

    @Override
    public void glGenBuffers(IntBuffer buffers) {
        checkLimit(buffers);
        GLES.glGenBuffers(buffers.remaining(), buffers, buffers.position() * 4L);
    }

    @Override
    public void glGenTextures(IntBuffer textures) {
        checkLimit(textures);
        GLES.glGenTextures(textures.remaining(), textures, textures.position() * 4L);
    }

    @Override
    public void glGenQueries(int num, IntBuffer buff) {
        GLES.glGenQueries(num, buff, buff.position() * 4L);
    }

    @Override
    public int glGetAttribLocation(int program, String name) {
        return GLES.glGetAttribLocation(program, stringToCAscii(name));
    }

    @Override
    public void glGetBoolean(int pname, ByteBuffer params) {
        checkLimit(params);
        if (tempByteArray.length < params.remaining()) {
            tempByteArray = new byte[params.remaining()];
        }
        GLES.glGetBooleanv(pname, tempByteArray);
        int pos = params.position();
        params.put(tempByteArray, 0, params.remaining());
        params.position(pos);
    }

    @Override
    public int glGetError() {
        return GLES.glGetError();
    }

    @Override
    public void glGetFloat(int parameterId, FloatBuffer storeValues) {
        checkLimit(storeValues);
        if (tmpFloatArray.length < storeValues.remaining()) {
            tmpFloatArray = new float[storeValues.remaining()];
        }
        GLES.glGetFloatv(parameterId, tmpFloatArray);
        int pos = storeValues.position();
        storeValues.put(tmpFloatArray, 0, storeValues.remaining());
        storeValues.position(pos);
    }

    @Override
    public void glGetInteger(int pname, IntBuffer params) {
        checkLimit(params);
        if (tempArray.length < params.remaining()) {
            tempArray = new int[params.remaining()];
        }
        GLES.glGetIntegerv(pname, tempArray);
        int pos = params.position();
        params.put(tempArray, 0, params.remaining());
        params.position(pos);
    }

    @Override
    public void glGetProgram(int program, int pname, IntBuffer params) {
        checkLimit(params);
        if (tempArray.length < params.remaining()) {
            tempArray = new int[params.remaining()];
        }
        GLES.glGetProgramiv(program, pname, tempArray);
        int pos = params.position();
        params.put(tempArray, 0, params.remaining());
        params.position(pos);
    }

    @Override
    public String glGetProgramInfoLog(int program, int maxLength) {
        if (tempArray.length < 1) {
            tempArray = new int[1];
        }
        GLES.glGetProgramiv(program, GLES.GL_INFO_LOG_LENGTH, tempArray);
        int length = Math.max(tempArray[0], 1);
        if (tempByteArray.length < length) {
            tempByteArray = new byte[length];
        }
        GLES.glGetProgramInfoLog(program, length, tempArray, tempByteArray);
        int stringLength = 0;
        while (stringLength < length && tempByteArray[stringLength] != 0) {
            stringLength++;
        }
        return new String(tempByteArray, 0, stringLength, StandardCharsets.UTF_8);
    }

    @Override
    public long glGetQueryObjectui64(int query, int pname) {
        throw new UnsupportedOperationException(
                "64-bit query results are not implemented by the iOS GLES binding");
    }

    @Override
    public int glGetQueryObjectiv(int query, int pname) {
        GLES.glGetQueryiv(query, pname, tempArray);
        return tempArray[0];
    }

    @Override
    public void glGetShader(int shader, int pname, IntBuffer params) {
        checkLimit(params);
        if (tempArray.length < params.remaining()) {
            tempArray = new int[params.remaining()];
        }
        GLES.glGetShaderiv(shader, pname, tempArray);
        int pos = params.position();
        params.put(tempArray, 0, params.remaining());
        params.position(pos);
    }

    @Override
    public String glGetShaderInfoLog(int shader, int maxLength) {
        if (tempArray.length < 1) {
            tempArray = new int[1];
        }
        GLES.glGetShaderiv(shader, GLES.GL_INFO_LOG_LENGTH, tempArray);
        int length = Math.max(tempArray[0], 1);
        if (tempByteArray.length < length) {
            tempByteArray = new byte[length];
        }
        GLES.glGetShaderInfoLog(shader, length, tempArray, tempByteArray);
        int stringLength = 0;
        while (stringLength < length && tempByteArray[stringLength] != 0) {
            stringLength++;
        }
        return new String(tempByteArray, 0, stringLength, StandardCharsets.UTF_8);
    }

    @Override
    public String glGetString(int name) {
        byte[] bytes = GLES.glGetString(name);
        if (bytes == null) {
            return null;
        }
        int length = 0;
        while (length < bytes.length && bytes[length] != 0) {
            length++;
        }
        return new String(bytes, 0, length, StandardCharsets.UTF_8);
    }

    @Override
    public int glGetUniformLocation(int program, String name) {
        return GLES.glGetUniformLocation(program, stringToCAscii(name));
    }

    @Override
    public boolean glIsEnabled(int cap) {
        // kept this always returning true for compatibility
        if (cap == GLExt.GL_MULTISAMPLE_ARB) {
            return true;
        } else {
            return GLES.glIsEnabled(cap) != 0;
        }
    }

    @Override
    public void glLineWidth(float width) {
        GLES.glLineWidth(width);
    }

    @Override
    public void glLinkProgram(int program) {
        GLES.glLinkProgram(program);
    }

    @Override
    public void glPixelStorei(int pname, int param) {
        GLES.glPixelStorei(pname, param);
    }

    @Override
    public void glPolygonOffset(float factor, float units) {
        GLES.glPolygonOffset(factor, units);
    }

    @Override
    public void glReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer data) {
        if (data == null) {
            return;
        }
        checkLimit(data);
        GLES.glReadPixels(x, y, width, height, format, type, data, data.position());
    }

    @Override
    public void glScissor(int x, int y, int width, int height) {
        GLES.glScissor(x, y, width, height);
    }

    @Override
    public void glShaderSource(int shader, String[] string, IntBuffer length) {
        StringBuilder shaderSource = new StringBuilder();
        int lengthPosition = length == null ? 0 : length.position();
        for (int i = 0; i < string.length; i++) {
            String source = string[i];
            if (length != null) {
                int sourceLength = length.get(lengthPosition + i);
                if (sourceLength >= 0 && sourceLength < source.length()) {
                    shaderSource.append(source, 0, sourceLength);
                    continue;
                }
            }
            shaderSource.append(source);
        }
        byte[] sourceBytes = shaderSource.toString().getBytes(StandardCharsets.UTF_8);
        tempArray[0] = sourceBytes.length;
        GLES.glShaderSource(shader, 1, sourceBytes, tempArray);
    }

    @Override
    public void glStencilFuncSeparate(int face, int func, int ref, int mask) {
        GLES.glStencilFuncSeparate(face, func, ref, mask);
    }

    @Override
    public void glStencilOpSeparate(int face, int sfail, int dpfail, int dppass) {
        GLES.glStencilOpSeparate(face, sfail, dpfail, dppass);
    }

    @Override
    public void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border,
            int format, int type, ByteBuffer data) {
        checkLimit(data);
        GLES.glTexImage2D(target, level, internalFormat, width, height, 0, format, type,
                data, data == null ? 0L : data.position());
    }

    @Override
    public void glTexParameterf(int target, int pname, float param) {
        GLES.glTexParameterf(target, pname, param);
    }

    @Override
    public void glTexParameteri(int target, int pname, int param) {
        GLES.glTexParameteri(target, pname, param);
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset,
            int width, int height, int format, int type, ByteBuffer data) {
        checkLimit(data);
        GLES.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type,
                data, data == null ? 0L : data.position());
    }

    @Override
    public void glUniform1(int location, FloatBuffer value) {
        checkLimit(value);
        if (tmpFloatArray.length < value.remaining()) {
            tmpFloatArray = new float[value.remaining()];
        }
        int pos = value.position();
        value.get(tmpFloatArray, 0, value.remaining());
        value.position(pos);
        GLES.glUniform1fv(location, value.remaining(), tmpFloatArray);
    }

    @Override
    public void glUniform1(int location, IntBuffer value) {
        checkLimit(value);
        if (tempArray.length < value.remaining()) {
            tempArray = new int[value.remaining()];
        }
        int pos = value.position();
        value.get(tempArray, 0, value.remaining());
        value.position(pos);
        GLES.glUniform1iv(location, value.remaining(), tempArray);
    }

    @Override
    public void glUniform1f(int location, float v0) {
        GLES.glUniform1f(location, v0);
    }

    @Override
    public void glUniform1i(int location, int v0) {
        GLES.glUniform1i(location, v0);
    }

    @Override
    public void glUniform2(int location, IntBuffer value) {
        checkLimit(value);
        if (tempArray.length < value.remaining()) {
            tempArray = new int[value.remaining()];
        }
        int pos = value.position();
        value.get(tempArray, 0, value.remaining());
        value.position(pos);
        GLES.glUniform2iv(location, value.remaining() / 2, tempArray);
    }

    @Override
    public void glUniform2(int location, FloatBuffer value) {
        checkLimit(value);
        if (tmpFloatArray.length < value.remaining()) {
            tmpFloatArray = new float[value.remaining()];
        }
        int pos = value.position();
        value.get(tmpFloatArray, 0, value.remaining());
        value.position(pos);
        GLES.glUniform2fv(location, value.remaining() / 2, tmpFloatArray);
    }

    @Override
    public void glUniform2f(int location, float v0, float v1) {
        GLES.glUniform2f(location, v0, v1);
    }

    @Override
    public void glUniform3(int location, IntBuffer value) {
        checkLimit(value);
        if (tempArray.length < value.remaining()) {
            tempArray = new int[value.remaining()];
        }
        int pos = value.position();
        value.get(tempArray, 0, value.remaining());
        value.position(pos);
        GLES.glUniform3iv(location, value.remaining() / 3, tempArray);
    }

    @Override
    public void glUniform3(int location, FloatBuffer value) {
        checkLimit(value);
        if (tmpFloatArray.length < value.remaining()) {
            tmpFloatArray = new float[value.remaining()];
        }
        int pos = value.position();
        value.get(tmpFloatArray, 0, value.remaining());
        value.position(pos);
        GLES.glUniform3fv(location, value.remaining() / 3, tmpFloatArray);
    }

    @Override
    public void glUniform3f(int location, float v0, float v1, float v2) {
        GLES.glUniform3f(location, v0, v1, v2);
    }

    @Override
    public void glUniform4(int location, FloatBuffer value) {
        checkLimit(value);
        if (tmpFloatArray.length < value.remaining()) {
            tmpFloatArray = new float[value.remaining()];
        }
        int pos = value.position();
        value.get(tmpFloatArray, 0, value.remaining());
        value.position(pos);
        GLES.glUniform4fv(location, value.remaining() / 4, tmpFloatArray);
    }

    @Override
    public void glUniform4(int location, IntBuffer value) {
        checkLimit(value);
        if (tempArray.length < value.remaining()) {
            tempArray = new int[value.remaining()];
        }
        int pos = value.position();
        value.get(tempArray, 0, value.remaining());
        value.position(pos);
        GLES.glUniform4iv(location, value.remaining() / 4, tempArray);
    }

    @Override
    public void glUniform4f(int location, float v0, float v1, float v2, float v3) {
        GLES.glUniform4f(location, v0, v1, v2, v3);
    }

    @Override
    public void glUniformMatrix3(int location, boolean transpose, FloatBuffer value) {
        checkLimit(value);
        if (tmpFloatArray.length < value.remaining()) {
            tmpFloatArray = new float[value.remaining()];
        }
        int pos = value.position();
        value.get(tmpFloatArray, 0, value.remaining());
        value.position(pos);
        GLES.glUniformMatrix3fv(location, value.remaining() / 9, (byte) (transpose ? 1 : 0), tmpFloatArray);
    }

    @Override
    public void glUniformMatrix4(int location, boolean transpose, FloatBuffer value) {
        checkLimit(value);
        if (tmpFloatArray.length < value.remaining()) {
            tmpFloatArray = new float[value.remaining()];
        }
        int pos = value.position();
        value.get(tmpFloatArray, 0, value.remaining());
        value.position(pos);
        GLES.glUniformMatrix4fv(location, value.remaining() / 16, (byte) (transpose ? 1 : 0), tmpFloatArray);
    }

    @Override
    public void glUseProgram(int program) {
        GLES.glUseProgram(program);
    }

    @Override
    public void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride,
            long pointer) {
        GLES.glVertexAttribPointer(index, size, type, (byte) (normalized ? 1 : 0), stride, pointer);
    }

    @Override
    public void glViewport(int x, int y, int width, int height) {
        GLES.glViewport(x, y, width, height);
    }

    @Override
    public void glBlitFramebufferEXT(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0,
            int dstY0, int dstX1, int dstY1, int mask, int filter) {
        GLES.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1,
                mask, filter);
    }

    @Override
    public void glBufferData(int target, IntBuffer data, int usage) {
        if (data == null) {
            return;
        }
        checkLimit(data);
        GLES.glBufferData(target, data.remaining() * 4L, data, data.position() * 4L, usage);
    }

    @Override
    public void glBufferSubData(int target, long offset, IntBuffer data) {
        if (data == null) {
            return;
        }
        checkLimit(data);
        GLES.glBufferSubData(target, offset, data.remaining() * 4L, data, data.position() * 4L);
    }

    @Override
    public void glDrawArraysInstancedARB(int mode, int first, int count, int primcount) {
        GLES.glDrawArraysInstanced(mode, first, count, primcount);
    }

    @Override
    public void glDrawBuffers(IntBuffer bufs) {
        checkLimit(bufs);
        if (tempArray.length < bufs.remaining()) {
            tempArray = new int[bufs.remaining()];
        }
        int pos = bufs.position();
        bufs.get(tempArray, 0, bufs.remaining());
        bufs.position(pos);
        GLES.glDrawBuffers(bufs.remaining(), tempArray);
    }

    @Override
    public void glDrawElementsInstancedARB(int mode, int indicesCount, int type, long indicesBufferOffset,
            int primcount) {
        GLES.glDrawElementsInstanced(mode, indicesCount, type, indicesBufferOffset, primcount);
    }

    @Override
    public int glGetUniformBlockIndex(int program, String uniformBlockName) {
        return GLES.glGetUniformBlockIndex(program, stringToCAscii(uniformBlockName));
    }

    @Override
    public void glBindBufferBase(int target, int index, int buffer) {
        GLES.glBindBufferBase(target, index, buffer);
    }

    @Override
    public void glUniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding) {
        GLES.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
    }

    @Override
    public int glGetProgramResourceIndex(int program, int programInterface, String name) {
        throw new UnsupportedOperationException("Shader storage buffer objects require OpenGL ES 3.1");
    }

    @Override
    public void glShaderStorageBlockBinding(int program, int storageBlockIndex, int storageBlockBinding) {
        throw new UnsupportedOperationException("Shader storage buffer objects require OpenGL ES 3.1");
    }

    @Override
    public void glGetMultisample(int pname, int index, FloatBuffer val) {
        throw new UnsupportedOperationException("Multisample textures require OpenGL ES 3.1");
    }

    @Override
    public void glRenderbufferStorageMultisampleEXT(int target, int samples, int internalformat, int width,
            int height) {
        GLES.glRenderbufferStorageMultisample(target, samples, internalformat, width, height);
    }

    @Override
    public void glTexImage2DMultisample(int target, int samples, int internalformat, int width, int height,
            boolean fixedSampleLocations) {
        throw new UnsupportedOperationException("Multisample textures require OpenGL ES 3.1");
    }

    @Override
    public void glVertexAttribDivisorARB(int index, int divisor) {
        GLES.glVertexAttribDivisor(index, divisor);
    }

    @Override
    public void glBindFramebufferEXT(int param1, int param2) {
        GLES.glBindFramebuffer(param1, param2);
    }

    @Override
    public void glBindRenderbufferEXT(int param1, int param2) {
        GLES.glBindRenderbuffer(param1, param2);
    }

    @Override
    public int glCheckFramebufferStatusEXT(int param1) {
        return GLES.glCheckFramebufferStatus(param1);
    }

    @Override
    public void glDeleteFramebuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        GLES.glDeleteFramebuffers(param1.remaining(), param1, param1.position() * 4L);
    }

    @Override
    public void glDeleteRenderbuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        GLES.glDeleteRenderbuffers(param1.remaining(), param1, param1.position() * 4L);
    }

    @Override
    public void glFramebufferRenderbufferEXT(int param1, int param2, int param3, int param4) {
        GLES.glFramebufferRenderbuffer(param1, param2, param3, param4);
    }

    @Override
    public void glFramebufferTexture2DEXT(int param1, int param2, int param3, int param4, int param5) {
        GLES.glFramebufferTexture2D(param1, param2, param3, param4, param5);
    }

    @Override
    public void glGenFramebuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        GLES.glGenFramebuffers(param1.remaining(), param1, param1.position() * 4L);
    }

    @Override
    public void glGenRenderbuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        GLES.glGenRenderbuffers(param1.remaining(), param1, param1.position() * 4L);
    }

    @Override
    public void glGenerateMipmapEXT(int param1) {
        GLES.glGenerateMipmap(param1);
    }

    @Override
    public void glRenderbufferStorageEXT(int param1, int param2, int param3, int param4) {
        GLES.glRenderbufferStorage(param1, param2, param3, param4);
    }

    @Override
    public void glReadPixels(int x, int y, int width, int height, int format, int type, long offset) {
        GLES.glReadPixels(x, y, width, height, format, type, offset);
    }

    @Override
    public int glClientWaitSync(Object sync, int flags, long timeout) {
        return GLES.glClientWaitSync(getSyncHandle(sync), flags, timeout);
    }

    @Override
    public void glDeleteSync(Object sync) {
        GLES.glDeleteSync(getSyncHandle(sync));
    }

    @Override
    public Object glFenceSync(int condition, int flags) {
        return GLES.glFenceSync(condition, flags);
    }
    
    @Override
    public void glFramebufferTextureLayerEXT(int target, int attachment, int texture, int level, int layer) {
        GLES.glFramebufferTextureLayer(target, attachment, texture, level, layer);
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
        ((Buffer) tmpBuff).clear();
        tmpBuff.put(0, mode);
        tmpBuff.rewind();
        glDrawBuffers(tmpBuff);
    }

    @Override
    public void glReadBuffer(int mode) {
        GLES.glReadBuffer(mode);
    }

    @Override
    public void glCompressedTexImage3D(int target, int level, int internalFormat,
            int width, int height, int depth, int border, ByteBuffer data) {
        if (data == null) {
            return;
        }
        checkLimit(data);
        GLES.glCompressedTexImage3D(target, level, internalFormat, width, height,
                depth, border, data.remaining(), data, data.position());
    }

    @Override
    public void glCompressedTexSubImage3D(int target, int level, int xoffset,
            int yoffset, int zoffset, int width, int height, int depth,
            int format, ByteBuffer data) {
        if (data == null) {
            return;
        }
        checkLimit(data);
        GLES.glCompressedTexSubImage3D(target, level, xoffset, yoffset, zoffset,
                width, height, depth, format, data.remaining(), data, data.position());
    }

    @Override
    public void glTexImage3D(int target, int level, int internalFormat,
            int width, int height, int depth, int border, int format, int type,
            ByteBuffer data) {
        checkLimit(data);
        GLES.glTexImage3D(target, level, internalFormat, width, height, depth, border, format, type,
                data, data == null ? 0L : data.position());
    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset,
            int zoffset, int width, int height, int depth, int format, int type,
            ByteBuffer data) {
        checkLimit(data);
        GLES.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width,
                height, depth, format, type, data, data == null ? 0L : data.position());
    }

    @Override
    public void glBindVertexArray(int array) {
        GLES.glBindVertexArray(array);
    }

    @Override
    public void glDeleteVertexArrays(IntBuffer arrays) {
        checkLimit(arrays);
        int count = arrays.remaining();
        if (tempArray.length < count) {
            tempArray = new int[count];
        }
        int pos = arrays.position();
        arrays.get(tempArray, 0, count);
        arrays.position(pos);
        GLES.glDeleteVertexArrays(count, tempArray);
    }

    @Override
    public void glGenVertexArrays(IntBuffer arrays) {
        checkLimit(arrays);
        int count = arrays.remaining();
        if (tempArray.length < count) {
            tempArray = new int[count];
        }
        GLES.glGenVertexArrays(count, tempArray);
        int pos = arrays.position();
        arrays.put(tempArray, 0, count);
        arrays.position(pos);
    }

    @Override
    public String glGetString(int name, int index) {
        byte[] bytes = GLES.glGetStringi(name, index);
        if (bytes == null) {
            return null;
        }
        int length = 0;
        while (length < bytes.length && bytes[length] != 0) {
            length++;
        }
        return new String(bytes, 0, length, StandardCharsets.UTF_8);
    }

}
