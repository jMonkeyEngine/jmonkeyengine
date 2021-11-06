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
package com.jme3.renderer.lwjgl;

import com.jme3.renderer.opengl.GL;
import com.jme3.renderer.opengl.GL2;
import com.jme3.renderer.opengl.GL3;
import com.jme3.renderer.opengl.GL4;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * The LWJGL implementation of interfaces {@link GL}, {@link GL2}, {@link GL3}, {@link GL4}.
 */
public class LwjglGL extends LwjglRender implements GL, GL2, GL3, GL4 {

    @Override
    public void resetStats() {
    }

    @Override
    public void glActiveTexture(final int texture) {
        GL13.glActiveTexture(texture);
    }

    @Override
    public void glAlphaFunc(final int func, final float ref) {
        GL11.glAlphaFunc(func, ref);
    }

    @Override
    public void glAttachShader(final int program, final int shader) {
        GL20.glAttachShader(program, shader);
    }

    @Override
    public void glBeginQuery(final int target, final int query) {
        GL15.glBeginQuery(target, query);
    }

    @Override
    public void glBindBuffer(final int target, final int buffer) {
        GL15.glBindBuffer(target, buffer);
    }

    @Override
    public void glBindTexture(final int target, final int texture) {
        GL11.glBindTexture(target, texture);
    }

    @Override
    public void glBlendEquationSeparate(final int colorMode, final int alphaMode) {
        GL20.glBlendEquationSeparate(colorMode, alphaMode);
    }

    @Override
    public void glBlendFunc(final int sFactor, final int dFactor) {
        GL11.glBlendFunc(sFactor, dFactor);
    }

    @Override
    public void glBlendFuncSeparate(final int sFactorRGB, final int dFactorRGB, final int sFactorAlpha,
                                    final int dFactorAlpha) {
        GL14.glBlendFuncSeparate(sFactorRGB, dFactorRGB, sFactorAlpha, dFactorAlpha);
    }

    @Override
    public void glBufferData(final int target, final long dataSize, final int usage) {
        GL15.glBufferData(target, dataSize, usage);
    }

    @Override
    public void glBufferData(final int target, final FloatBuffer data, final int usage) {
        checkLimit(data);
        GL15.glBufferData(target, data, usage);
    }

    @Override
    public void glBufferData(final int target, final ShortBuffer data, final int usage) {
        checkLimit(data);
        GL15.glBufferData(target, data, usage);
    }

    @Override
    public void glBufferData(final int target, final ByteBuffer data, final int usage) {
        checkLimit(data);
        GL15.glBufferData(target, data, usage);
    }

    @Override
    public void glBufferSubData(final int target, final long offset, final FloatBuffer data) {
        checkLimit(data);
        GL15.glBufferSubData(target, offset, data);
    }

    @Override
    public void glBufferSubData(final int target, final long offset, final ShortBuffer data) {
        checkLimit(data);
        GL15.glBufferSubData(target, offset, data);
    }

    @Override
    public void glBufferSubData(final int target, final long offset, final ByteBuffer data) {
        checkLimit(data);
        GL15.glBufferSubData(target, offset, data);
    }

    @Override
    public void glClear(final int mask) {
        GL11.glClear(mask);
    }

    @Override
    public void glClearColor(final float red, final float green, final float blue, final float alpha) {
        GL11.glClearColor(red, green, blue, alpha);
    }

    @Override
    public void glColorMask(final boolean red, final boolean green, final boolean blue, final boolean alpha) {
        GL11.glColorMask(red, green, blue, alpha);
    }

    @Override
    public void glCompileShader(final int shader) {
        GL20.glCompileShader(shader);
    }

    @Override
    public void glCompressedTexImage2D(final int target, final int level, final int internalFormat, final int width,
                                       final int height, final int border, final ByteBuffer data) {
        checkLimit(data);
        GL13.glCompressedTexImage2D(target, level, internalFormat, width, height, border, data);
    }

    @Override
    public void glCompressedTexImage3D(final int target, final int level, final int internalFormat, final int width,
                                       final int height, final int depth, final int border, final ByteBuffer data) {
        checkLimit(data);
        GL13.glCompressedTexImage3D(target, level, internalFormat, width, height, depth, border, data);
    }

    @Override
    public void glCompressedTexSubImage2D(final int target, final int level, final int xoffset, final int yoffset,
                                          final int width, final int height, final int format, final ByteBuffer data) {
        checkLimit(data);
        GL13.glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, data);
    }

    @Override
    public void glCompressedTexSubImage3D(final int target, final int level, final int xoffset, final int yoffset,
                                          final int zoffset, final int width, final int height, final int depth,
                                          final int format, final ByteBuffer data) {
        checkLimit(data);
        GL13.glCompressedTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, data);
    }

    @Override
    public int glCreateProgram() {
        return GL20.glCreateProgram();
    }

    @Override
    public int glCreateShader(final int shaderType) {
        return GL20.glCreateShader(shaderType);
    }

    @Override
    public void glCullFace(final int mode) {
        GL11.glCullFace(mode);
    }

    @Override
    public void glDeleteBuffers(final IntBuffer buffers) {
        checkLimit(buffers);
        GL15.glDeleteBuffers(buffers);
    }

    @Override
    public void glDeleteProgram(final int program) {
        GL20.glDeleteProgram(program);
    }

    @Override
    public void glDeleteShader(final int shader) {
        GL20.glDeleteShader(shader);
    }

    @Override
    public void glDeleteTextures(final IntBuffer textures) {
        checkLimit(textures);
        GL11.glDeleteTextures(textures);
    }

    @Override
    public void glDepthFunc(final int func) {
        GL11.glDepthFunc(func);
    }

    @Override
    public void glDepthMask(final boolean flag) {
        GL11.glDepthMask(flag);
    }

    @Override
    public void glDepthRange(final double nearVal, final double farVal) {
        GL11.glDepthRange(nearVal, farVal);
    }

    @Override
    public void glDetachShader(final int program, final int shader) {
        GL20.glDetachShader(program, shader);
    }

    @Override
    public void glDisable(final int cap) {
        GL11.glDisable(cap);
    }

    @Override
    public void glDisableVertexAttribArray(final int index) {
        GL20.glDisableVertexAttribArray(index);
    }

    @Override
    public void glDrawArrays(final int mode, final int first, final int count) {
        GL11.glDrawArrays(mode, first, count);
    }

    @Override
    public void glDrawBuffer(final int mode) {
        GL11.glDrawBuffer(mode);
    }

    @Override
    public void glDrawRangeElements(final int mode, final int start, final int end, final int count, final int type,
                                    final long indices) {
        GL12.glDrawRangeElements(mode, start, end, count, type, indices);
    }

    @Override
    public void glEnable(final int cap) {
        GL11.glEnable(cap);
    }

    @Override
    public void glEnableVertexAttribArray(final int index) {
        GL20.glEnableVertexAttribArray(index);
    }

    @Override
    public void glEndQuery(final int target) {
        GL15.glEndQuery(target);
    }

    @Override
    public void glGenBuffers(final IntBuffer buffers) {
        checkLimit(buffers);
        GL15.glGenBuffers(buffers);
    }

    @Override
    public void glGenTextures(final IntBuffer textures) {
        checkLimit(textures);
        GL11.glGenTextures(textures);
    }

    @Override
    public void glGenQueries(final int num, final IntBuffer ids) {
        GL15.glGenQueries(ids);
    }

    @Override
    public void glGetBoolean(final int pname, final ByteBuffer params) {
        checkLimit(params);
        GL11.glGetBooleanv(pname, params);
    }

    @Override
    public void glGetBufferSubData(final int target, final long offset, final ByteBuffer data) {
        checkLimit(data);
        GL15.glGetBufferSubData(target, offset, data);
    }

    @Override
    public int glGetError() {
        return GL11.glGetError();
    }

    @Override
    public void glGetFloat(int parameterId, FloatBuffer storeValues) {
        checkLimit(storeValues);
        GL11.glGetFloatv(parameterId, storeValues);
    }

    @Override
    public void glGetInteger(final int pname, final IntBuffer params) {
        checkLimit(params);
        GL11.glGetIntegerv(pname, params);
    }

    @Override
    public void glGetProgram(final int program, final int pname, final IntBuffer params) {
        checkLimit(params);
        GL20.glGetProgramiv(program, pname, params);
    }

    @Override
    public void glGetShader(final int shader, final int pname, final IntBuffer params) {
        checkLimit(params);
        GL20.glGetShaderiv(shader, pname, params);
    }

    @Override
    public String glGetString(final int name) {
        return GL11.glGetString(name);
    }

    @Override
    public String glGetString(final int name, final int index) {
        return GL30.glGetStringi(name, index);
    }

    @Override
    public boolean glIsEnabled(final int cap) {
        return GL11.glIsEnabled(cap);
    }

    @Override
    public void glLineWidth(final float width) {
        GL11.glLineWidth(width);
    }

    @Override
    public void glLinkProgram(final int program) {
        GL20.glLinkProgram(program);
    }

    @Override
    public void glPixelStorei(final int pname, final int param) {
        GL11.glPixelStorei(pname, param);
    }

    @Override
    public void glPointSize(final float size) {
        GL11.glPointSize(size);
    }

    @Override
    public void glPolygonMode(final int face, final int mode) {
        GL11.glPolygonMode(face, mode);
    }

    @Override
    public void glPolygonOffset(final float factor, final float units) {
        GL11.glPolygonOffset(factor, units);
    }

    @Override
    public void glReadBuffer(final int mode) {
        GL11.glReadBuffer(mode);
    }

    @Override
    public void glReadPixels(final int x, final int y, final int width, final int height, final int format,
                             final int type, final ByteBuffer data) {
        checkLimit(data);
        GL11.glReadPixels(x, y, width, height, format, type, data);
    }

    @Override
    public void glReadPixels(final int x, final int y, final int width, final int height, final int format,
                             final int type, final long offset) {
        GL11.glReadPixels(x, y, width, height, format, type, offset);
    }

    @Override
    public void glScissor(final int x, final int y, final int width, final int height) {
        GL11.glScissor(x, y, width, height);
    }

    @Override
    public void glStencilFuncSeparate(final int face, final int func, final int ref, final int mask) {
        GL20.glStencilFuncSeparate(face, func, ref, mask);
    }

    @Override
    public void glStencilOpSeparate(final int face, final int sfail, final int dpfail, final int dppass) {
        GL20.glStencilOpSeparate(face, sfail, dpfail, dppass);
    }

    @Override
    public void glTexImage2D(final int target, final int level, final int internalFormat, final int width,
                             final int height, final int border, final int format, final int type,
                             final ByteBuffer data) {
        checkLimit(data);
        GL11.glTexImage2D(target, level, internalFormat, width, height, border, format, type, data);
    }

    @Override
    public void glTexImage3D(final int target, final int level, final int internalFormat, final int width,
                             final int height, final int depth, final int border, final int format, final int type,
                             final ByteBuffer data) {
        checkLimit(data);
        GL12.glTexImage3D(target, level, internalFormat, width, height, depth, border, format, type, data);
    }

    @Override
    public void glTexParameterf(final int target, final int pname, final float param) {
        GL11.glTexParameterf(target, pname, param);
    }

    @Override
    public void glTexParameteri(final int target, final int pname, final int param) {
        GL11.glTexParameteri(target, pname, param);
    }

    @Override
    public void glTexSubImage2D(final int target, final int level, final int xoffset, final int yoffset,
                                final int width, final int height, final int format, final int type,
                                final ByteBuffer data) {
        checkLimit(data);
        GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, data);
    }

    @Override
    public void glTexSubImage3D(final int target, final int level, final int xoffset, final int yoffset,
                                final int zoffset, final int width, final int height, final int depth, final int format,
                                final int type, final ByteBuffer data) {
        checkLimit(data);
        GL12.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, data);
    }

    @Override
    public void glUniform1(final int location, final FloatBuffer value) {
        checkLimit(value);
        GL20.glUniform1fv(location, value);
    }

    @Override
    public void glUniform1(final int location, final IntBuffer value) {
        checkLimit(value);
        GL20.glUniform1iv(location, value);
    }

    @Override
    public void glUniform1f(final int location, final float v0) {
        GL20.glUniform1f(location, v0);
    }

    @Override
    public void glUniform1i(final int location, final int v0) {
        GL20.glUniform1i(location, v0);
    }

    @Override
    public void glUniform2(final int location, final IntBuffer value) {
        checkLimit(value);
        GL20.glUniform2iv(location, value);
    }

    @Override
    public void glUniform2(final int location, final FloatBuffer value) {
        checkLimit(value);
        GL20.glUniform2fv(location, value);
    }

    @Override
    public void glUniform2f(final int location, final float v0, final float v1) {
        GL20.glUniform2f(location, v0, v1);
    }

    @Override
    public void glUniform3(final int location, final IntBuffer value) {
        checkLimit(value);
        GL20.glUniform3iv(location, value);
    }

    @Override
    public void glUniform3(final int location, final FloatBuffer value) {
        checkLimit(value);
        GL20.glUniform3fv(location, value);
    }

    @Override
    public void glUniform3f(final int location, final float v0, final float v1, final float v2) {
        GL20.glUniform3f(location, v0, v1, v2);
    }

    @Override
    public void glUniform4(final int location, final FloatBuffer value) {
        checkLimit(value);
        GL20.glUniform4fv(location, value);
    }

    @Override
    public void glUniform4(final int location, final IntBuffer value) {
        checkLimit(value);
        GL20.glUniform4iv(location, value);
    }

    @Override
    public void glUniform4f(final int location, final float v0, final float v1, final float v2, final float v3) {
        GL20.glUniform4f(location, v0, v1, v2, v3);
    }

    @Override
    public void glUniformMatrix3(final int location, final boolean transpose, final FloatBuffer value) {
        checkLimit(value);
        GL20.glUniformMatrix3fv(location, transpose, value);
    }

    @Override
    public void glUniformMatrix4(final int location, final boolean transpose, final FloatBuffer value) {
        checkLimit(value);
        GL20.glUniformMatrix4fv(location, transpose, value);
    }

    @Override
    public void glUseProgram(final int program) {
        GL20.glUseProgram(program);
    }

    @Override
    public void glVertexAttribPointer(final int index, final int size, final int type, final boolean normalized,
                                      final int stride, final long pointer) {
        GL20.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
    }

    @Override
    public void glViewport(final int x, final int y, final int width, final int height) {
        GL11.glViewport(x, y, width, height);
    }

    @Override
    public int glGetAttribLocation(final int program, final String name) {
        // NOTE: LWJGL requires null-terminated strings
        return GL20.glGetAttribLocation(program, name + "\0");
    }

    @Override
    public int glGetUniformLocation(final int program, final String name) {
        // NOTE: LWJGL requires null-terminated strings
        return GL20.glGetUniformLocation(program, name + "\0");
    }

    @Override
    public void glShaderSource(final int shader, final String[] strings, final IntBuffer length) {
        checkLimit(length);
        GL20.glShaderSource(shader, strings);
    }

    @Override
    public String glGetProgramInfoLog(final int program, final int maxSize) {
        return GL20.glGetProgramInfoLog(program, maxSize);
    }

    @Override
    public long glGetQueryObjectui64(int query, int target) {
        return ARBTimerQuery.glGetQueryObjectui64(query, target);
    }

    @Override
    public int glGetQueryObjectiv(int query, int pname) {
        return GL15.glGetQueryObjecti(query, pname);
    }

    @Override
    public String glGetShaderInfoLog(int shader, int maxSize) {
        return GL20.glGetShaderInfoLog(shader, maxSize);
    }

    @Override
    public void glBindFragDataLocation(final int program, final int colorNumber, final String name) {
        GL30.glBindFragDataLocation(program, colorNumber, name);
    }

    @Override
    public void glBindVertexArray(final int array) {
        GL30.glBindVertexArray(array);
    }

    @Override
    public void glGenVertexArrays(final IntBuffer arrays) {
        checkLimit(arrays);
        GL30.glGenVertexArrays(arrays);
    }

    @Override
    public void glPatchParameter(final int count) {
        GL40.glPatchParameteri(GL40.GL_PATCH_VERTICES, count);
    }

    @Override
    public int glGetProgramResourceIndex(final int program, final int programInterface, final String name) {
        return GL43.glGetProgramResourceIndex(program, programInterface, name);
    }

    @Override
    public void glShaderStorageBlockBinding(final int program, final int storageBlockIndex, final int storageBlockBinding) {
        GL43.glShaderStorageBlockBinding(program, storageBlockIndex, storageBlockBinding);
    }

    @Override
    public void glDeleteVertexArrays(final IntBuffer arrays) {
        checkLimit(arrays);
        ARBVertexArrayObject.glDeleteVertexArrays(arrays);
    }

    @Override
    public int glGetUniformBlockIndex(final int program, final String uniformBlockName) {
        return GL31.glGetUniformBlockIndex(program, uniformBlockName);
    }

    @Override
    public void glBindBufferBase(final int target, final int index, final int buffer) {
        GL30.glBindBufferBase(target, index, buffer);
    }

    @Override
    public void glUniformBlockBinding(final int program, final int uniformBlockIndex, final int uniformBlockBinding) {
        GL31.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
    }
}
