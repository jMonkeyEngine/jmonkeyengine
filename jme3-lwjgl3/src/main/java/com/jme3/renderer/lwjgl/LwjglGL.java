/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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

import com.jme3.renderer.RendererException;
import com.jme3.renderer.opengl.GL;
import com.jme3.renderer.opengl.GL2;
import com.jme3.renderer.opengl.GL3;
import com.jme3.renderer.opengl.GL4;
import com.jme3.system.NativeLibraryLoader;
import com.jme3.system.Platform;
import org.lwjgl.opengl.*;

import java.nio.*;

public class LwjglGL implements GL, GL2, GL3, GL4 {

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
    
    public void resetStats() {
    }
    
    public void glActiveTexture(int param1) {
        GL13.glActiveTexture(param1);
    }

    public void glAlphaFunc(int param1, float param2) {
        GL11.glAlphaFunc(param1, param2);
    }

    public void glAttachShader(int param1, int param2) {
        GL20.glAttachShader(param1, param2);
    }

    @Override
    public void glBeginQuery(int target, int query) {
        GL15.glBeginQuery(target, query);
    }

    public void glBindBuffer(int param1, int param2) {
        GL15.glBindBuffer(param1, param2);
    }

    public void glBindTexture(int param1, int param2) {
        GL11.glBindTexture(param1, param2);
    }

    public void glBlendEquationSeparate(int colorMode, int alphaMode){
        GL20.glBlendEquationSeparate(colorMode,alphaMode);
    }

    public void glBlendFunc(int param1, int param2) {
        GL11.glBlendFunc(param1, param2);
    }
    
    public void glBlendFuncSeparate(int param1, int param2, int param3, int param4) {
        GL14.glBlendFuncSeparate(param1, param2, param3, param4);
    }

    public void glBufferData(int param1, long param2, int param3) {
        GL15.glBufferData(param1, param2, param3);
    }
    
    public void glBufferData(int param1, FloatBuffer param2, int param3) {
        checkLimit(param2);
        GL15.glBufferData(param1, param2, param3);
    }

    public void glBufferData(int param1, ShortBuffer param2, int param3) {
        checkLimit(param2);
        GL15.glBufferData(param1, param2, param3);
    }

    public void glBufferData(int param1, ByteBuffer param2, int param3) {
        checkLimit(param2);
        GL15.glBufferData(param1, param2, param3);
    }

    public void glBufferSubData(int param1, long param2, FloatBuffer param3) {
        checkLimit(param3);
        GL15.glBufferSubData(param1, param2, param3);
    }

    public void glBufferSubData(int param1, long param2, ShortBuffer param3) {
        checkLimit(param3);
        GL15.glBufferSubData(param1, param2, param3);
    }

    public void glBufferSubData(int param1, long param2, ByteBuffer param3) {
        checkLimit(param3);
        GL15.glBufferSubData(param1, param2, param3);
    }

    public void glClear(int param1) {
        GL11.glClear(param1);
    }

    public void glClearColor(float param1, float param2, float param3, float param4) {
        GL11.glClearColor(param1, param2, param3, param4);
    }

    public void glColorMask(boolean param1, boolean param2, boolean param3, boolean param4) {
        GL11.glColorMask(param1, param2, param3, param4);
    }

    public void glCompileShader(int param1) {
        GL20.glCompileShader(param1);
    }

    public void glCompressedTexImage2D(int param1, int param2, int param3, int param4, int param5, int param6, ByteBuffer param7) {
        checkLimit(param7);
        GL13.glCompressedTexImage2D(param1, param2, param3, param4, param5, param6, param7);
    }

    public void glCompressedTexImage3D(int param1, int param2, int param3, int param4, int param5, int param6, int param7, ByteBuffer param8) {
        checkLimit(param8);
        GL13.glCompressedTexImage3D(param1, param2, param3, param4, param5, param6, param7, param8);
    }

    public void glCompressedTexSubImage2D(int param1, int param2, int param3, int param4, int param5, int param6, int param7, ByteBuffer param8) {
        checkLimit(param8);
        GL13.glCompressedTexSubImage2D(param1, param2, param3, param4, param5, param6, param7, param8);
    }

    public void glCompressedTexSubImage3D(int param1, int param2, int param3, int param4, int param5, int param6, int param7, int param8, int param9, ByteBuffer param10) {
        checkLimit(param10);
        GL13.glCompressedTexSubImage3D(param1, param2, param3, param4, param5, param6, param7, param8, param9, param10);
    }

    public int glCreateProgram() {
        return GL20.glCreateProgram();
    }

    public int glCreateShader(int param1) {
        return GL20.glCreateShader(param1);
    }

    public void glCullFace(int param1) {
        GL11.glCullFace(param1);
    }

    public void glDeleteBuffers(IntBuffer param1) {
        checkLimit(param1);
        GL15.glDeleteBuffers(param1);
    }

    public void glDeleteProgram(int param1) {
        GL20.glDeleteProgram(param1);
    }

    public void glDeleteShader(int param1) {
        GL20.glDeleteShader(param1);
    }

    public void glDeleteTextures(IntBuffer param1) {
        checkLimit(param1);
        GL11.glDeleteTextures(param1);
    }

    public void glDepthFunc(int param1) {
        GL11.glDepthFunc(param1);
    }

    public void glDepthMask(boolean param1) {
        GL11.glDepthMask(param1);
    }

    public void glDepthRange(double param1, double param2) {
        GL11.glDepthRange(param1, param2);
    }

    public void glDetachShader(int param1, int param2) {
        GL20.glDetachShader(param1, param2);
    }

    public void glDisable(int param1) {
        GL11.glDisable(param1);
    }

    public void glDisableVertexAttribArray(int param1) {
        GL20.glDisableVertexAttribArray(param1);
    }

    public void glDrawArrays(int param1, int param2, int param3) {
        GL11.glDrawArrays(param1, param2, param3);
    }

    public void glDrawBuffer(int param1) {
        GL11.glDrawBuffer(param1);
    }
    
    public void glDrawRangeElements(int param1, int param2, int param3, int param4, int param5, long param6) {
        GL12.glDrawRangeElements(param1, param2, param3, param4, param5, param6);
    }

    public void glEnable(int param1) {
        GL11.glEnable(param1);
    }

    public void glEnableVertexAttribArray(int param1) {
        GL20.glEnableVertexAttribArray(param1);
    }

    @Override
    public void glEndQuery(int target) {
        GL15.glEndQuery(target);
    }

    public void glGenBuffers(IntBuffer param1) {
        checkLimit(param1);
        GL15.glGenBuffers(param1);
    }

    public void glGenTextures(IntBuffer param1) {
        checkLimit(param1);
        GL11.glGenTextures(param1);
    }

    @Override
    public void glGenQueries(int num, IntBuffer ids) {
        GL15.glGenQueries(ids);
    }

    public void glGetBoolean(int param1, ByteBuffer param2) {
        checkLimit(param2);
        GL11.glGetBooleanv(param1, param2);
    }
    
    public void glGetBufferSubData(int target, long offset, ByteBuffer data) {
        checkLimit(data);
        GL15.glGetBufferSubData(target, offset, data);
    }

    public int glGetError() {
        return GL11.glGetError();
    }
    
    public void glGetInteger(int param1, IntBuffer param2) {
        checkLimit(param2);
        GL11.glGetIntegerv(param1, param2);
    }

    public void glGetProgram(int param1, int param2, IntBuffer param3) {
        checkLimit(param3);
        GL20.glGetProgramiv(param1, param2, param3);
    }

    public void glGetShader(int param1, int param2, IntBuffer param3) {
        checkLimit(param3);
        GL20.glGetShaderiv(param1, param2, param3);
    }

    public String glGetString(int param1) {
        return GL11.glGetString(param1);
    }
    
    public String glGetString(int param1, int param2) {
        return GL30.glGetStringi(param1, param2);
    }

    public boolean glIsEnabled(int param1) {
        return GL11.glIsEnabled(param1);
    }

    public void glLineWidth(float param1) {
        GL11.glLineWidth(param1);
    }

    public void glLinkProgram(int param1) {
        GL20.glLinkProgram(param1);
    }

    public void glPixelStorei(int param1, int param2) {
        GL11.glPixelStorei(param1, param2);
    }

    public void glPointSize(float param1) {
        GL11.glPointSize(param1);
    }

    public void glPolygonMode(int param1, int param2) {
        GL11.glPolygonMode(param1, param2);
    }

    public void glPolygonOffset(float param1, float param2) {
        GL11.glPolygonOffset(param1, param2);
    }

    public void glReadBuffer(int param1) {
        GL11.glReadBuffer(param1);
    }

    public void glReadPixels(int param1, int param2, int param3, int param4, int param5, int param6, ByteBuffer param7) {
        checkLimit(param7);
        GL11.glReadPixels(param1, param2, param3, param4, param5, param6, param7);
    }
    
    public void glReadPixels(int param1, int param2, int param3, int param4, int param5, int param6, long param7) {
        GL11.glReadPixels(param1, param2, param3, param4, param5, param6, param7);
    }

    public void glScissor(int param1, int param2, int param3, int param4) {
        GL11.glScissor(param1, param2, param3, param4);
    }

    public void glStencilFuncSeparate(int param1, int param2, int param3, int param4) {
        GL20.glStencilFuncSeparate(param1, param2, param3, param4);
    }

    public void glStencilOpSeparate(int param1, int param2, int param3, int param4) {
        GL20.glStencilOpSeparate(param1, param2, param3, param4);
    }

    public void glTexImage2D(int param1, int param2, int param3, int param4, int param5, int param6, int param7, int param8, ByteBuffer param9) {
        checkLimit(param9);
        GL11.glTexImage2D(param1, param2, param3, param4, param5, param6, param7, param8, param9);
    }

    public void glTexImage3D(int param1, int param2, int param3, int param4, int param5, int param6, int param7, int param8, int param9, ByteBuffer param10) {
        checkLimit(param10);
        GL12.glTexImage3D(param1, param2, param3, param4, param5, param6, param7, param8, param9, param10);
    }

    public void glTexParameterf(int param1, int param2, float param3) {
        GL11.glTexParameterf(param1, param2, param3);
    }

    public void glTexParameteri(int param1, int param2, int param3) {
        GL11.glTexParameteri(param1, param2, param3);
    }

    public void glTexSubImage2D(int param1, int param2, int param3, int param4, int param5, int param6, int param7, int param8, ByteBuffer param9) {
        checkLimit(param9);
        GL11.glTexSubImage2D(param1, param2, param3, param4, param5, param6, param7, param8, param9);
    }

    public void glTexSubImage3D(int param1, int param2, int param3, int param4, int param5, int param6, int param7, int param8, int param9, int param10, ByteBuffer param11) {
        checkLimit(param11);
        GL12.glTexSubImage3D(param1, param2, param3, param4, param5, param6, param7, param8, param9, param10, param11);
    }

    public void glUniform1(int param1, FloatBuffer param2) {
        checkLimit(param2);
        GL20.glUniform1fv(param1, param2);
    }

    public void glUniform1(int param1, IntBuffer param2) {
        checkLimit(param2);
        GL20.glUniform1iv(param1, param2);
    }

    public void glUniform1f(int param1, float param2) {
        GL20.glUniform1f(param1, param2);
    }

    public void glUniform1i(int param1, int param2) {
        GL20.glUniform1i(param1, param2);
    }

    public void glUniform2(int param1, IntBuffer param2) {
        checkLimit(param2);
        GL20.glUniform2iv(param1, param2);
    }

    public void glUniform2(int param1, FloatBuffer param2) {
        checkLimit(param2);
        GL20.glUniform2fv(param1, param2);
    }

    public void glUniform2f(int param1, float param2, float param3) {
        GL20.glUniform2f(param1, param2, param3);
    }

    public void glUniform3(int param1, IntBuffer param2) {
        checkLimit(param2);
        GL20.glUniform3iv(param1, param2);
    }

    public void glUniform3(int param1, FloatBuffer param2) {
        checkLimit(param2);
        GL20.glUniform3fv(param1, param2);
    }

    public void glUniform3f(int param1, float param2, float param3, float param4) {
        GL20.glUniform3f(param1, param2, param3, param4);
    }

    public void glUniform4(int param1, FloatBuffer param2) {
        checkLimit(param2);
        GL20.glUniform4fv(param1, param2);
    }

    public void glUniform4(int param1, IntBuffer param2) {
        checkLimit(param2);
        GL20.glUniform4iv(param1, param2);
    }

    public void glUniform4f(int param1, float param2, float param3, float param4, float param5) {
        GL20.glUniform4f(param1, param2, param3, param4, param5);
    }

    public void glUniformMatrix3(int param1, boolean param2, FloatBuffer param3) {
        checkLimit(param3);
        GL20.glUniformMatrix3fv(param1, param2, param3);
    }

    public void glUniformMatrix4(int param1, boolean param2, FloatBuffer param3) {
        checkLimit(param3);
        GL20.glUniformMatrix4fv(param1, param2, param3);
    }

    public void glUseProgram(int param1) {
        GL20.glUseProgram(param1);
    }

    public void glVertexAttribPointer(int param1, int param2, int param3, boolean param4, int param5, long param6) {
        GL20.glVertexAttribPointer(param1, param2, param3, param4, param5, param6);
    }

    public void glViewport(int param1, int param2, int param3, int param4) {
        GL11.glViewport(param1, param2, param3, param4);
    }

    public int glGetAttribLocation(int param1, String param2) {
        // NOTE: LWJGL requires null-terminated strings
        return GL20.glGetAttribLocation(param1, param2 + "\0");
    }

    public int glGetUniformLocation(int param1, String param2) {
        // NOTE: LWJGL requires null-terminated strings
        return GL20.glGetUniformLocation(param1, param2 + "\0");
    }

    public void glShaderSource(int param1, String[] param2, IntBuffer param3) {
        checkLimit(param3);
        GL20.glShaderSource(param1, param2);
    }

    public String glGetProgramInfoLog(int program, int maxSize) {
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

    public String glGetShaderInfoLog(int shader, int maxSize) {
        return GL20.glGetShaderInfoLog(shader, maxSize);
    }

    @Override
    public void glBindFragDataLocation(int param1, int param2, String param3) {
        GL30.glBindFragDataLocation(param1, param2, param3);
    }

    @Override
    public void glBindVertexArray(int param1) {
        GL30.glBindVertexArray(param1);
    }

    @Override
    public void glGenVertexArrays(IntBuffer param1) {
        checkLimit(param1);
        GL30.glGenVertexArrays(param1);
    }

    @Override
    public void glPatchParameter(int count) {
        GL40.glPatchParameteri(GL40.GL_PATCH_VERTICES,count);
    }
    
    @Override
    public void glDeleteVertexArrays(IntBuffer arrays) {
        checkLimit(arrays);
        ARBVertexArrayObject.glDeleteVertexArrays(arrays);
    }

    @Override
    public void glFramebufferTextureLayer(int param1, int param2, int param3, int param4, int param5) {
        GL30.glFramebufferTextureLayer(param1, param2, param3, param4, param5);
    }
}
