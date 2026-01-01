/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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

import com.jme3.math.Matrix4f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.RendererException;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * A compute shader for general-purpose GPU computing (GPGPU).
 * <p>
 * Compute shaders require OpenGL 4.3 or higher.
 */
public class ComputeShader {

    private final GL4 gl;
    private final int programId;
    /**
     * Creates a new compute shader from GLSL source code.
     */
    public ComputeShader(GL4 gl, String source) {
        this.gl = gl;

        // Create and compile the shader
        int shaderId = gl.glCreateShader(GL4.GL_COMPUTE_SHADER);
        if (shaderId <= 0) {
            throw new RendererException("Failed to create compute shader");
        }

        IntBuffer intBuf = BufferUtils.createIntBuffer(1);
        intBuf.clear();
        intBuf.put(0, source.length());
        gl.glShaderSource(shaderId, new String[]{source}, intBuf);
        gl.glCompileShader(shaderId);

        // Check compilation status
        gl.glGetShader(shaderId, GL.GL_COMPILE_STATUS, intBuf);
        if (intBuf.get(0) != GL.GL_TRUE) {
            gl.glGetShader(shaderId, GL.GL_INFO_LOG_LENGTH, intBuf);
            String infoLog = gl.glGetShaderInfoLog(shaderId, intBuf.get(0));
            gl.glDeleteShader(shaderId);
            throw new RendererException("Compute shader compilation failed: " + infoLog);
        }

        // Create program and link
        programId = gl.glCreateProgram();
        if (programId <= 0) {
            gl.glDeleteShader(shaderId);
            throw new RendererException("Failed to create shader program");
        }

        gl.glAttachShader(programId, shaderId);
        gl.glLinkProgram(programId);

        // Check link status
        gl.glGetProgram(programId, GL.GL_LINK_STATUS, intBuf);
        if (intBuf.get(0) != GL.GL_TRUE) {
            gl.glGetProgram(programId, GL.GL_INFO_LOG_LENGTH, intBuf);
            String infoLog = gl.glGetProgramInfoLog(programId, intBuf.get(0));
            gl.glDeleteShader(shaderId);
            gl.glDeleteProgram(programId);
            throw new RendererException("Compute shader program linking failed: " + infoLog);
        }

        // Shader object can be deleted after linking
        gl.glDeleteShader(shaderId);
    }

    /**
     * Activates this compute shader for use.
     * Must be called before setting uniforms or dispatching.
     */
    public void makeActive() {
        gl.glUseProgram(programId);
    }

    /**
     * Dispatches the compute shader with the specified number of work groups.
     */
    public void dispatch(int numGroupsX, int numGroupsY, int numGroupsZ) {
        gl.glDispatchCompute(numGroupsX, numGroupsY, numGroupsZ);
    }

    public void bindTexture(int bindingPoint, Texture texture) {
        gl.glActiveTexture(GL.GL_TEXTURE0 + bindingPoint);
        int textureId = texture.getImage().getId();
        int target = convertTextureType(texture);
        gl.glBindTexture(target, textureId);
    }

    public void setUniform(int location, int value) {
        gl.glUniform1i(location, value);
    }

    public void setUniform(int location, float value) {
        gl.glUniform1f(location, value);
    }

    public void setUniform(int location, Vector2f value) {
        gl.glUniform2f(location, value.x, value.y);
    }

    public void setUniform(int location, Vector3f value) {
        gl.glUniform3f(location, value.x, value.y, value.z);
    }

    public void setUniform(int location, Vector4f value) {
        gl.glUniform4f(location, value.x, value.y, value.z, value.w);
    }

    public void setUniform(int location, Matrix4f value) {
        FloatBuffer floatBuf16 = BufferUtils.createFloatBuffer(16);
        value.fillFloatBuffer(floatBuf16, true);
        floatBuf16.clear();
        gl.glUniformMatrix4(location, false, floatBuf16);
    }

    public int getUniformLocation(String name) {
        return gl.glGetUniformLocation(programId, name);
    }

    public void bindShaderStorageBuffer(int location, ShaderStorageBufferObject ssbo) {
        gl.glBindBufferBase(GL4.GL_SHADER_STORAGE_BUFFER, location, ssbo.getBufferId());
    }

    /**
     * Deletes this compute shader and releases GPU resources.
     * The shader should not be used after calling this method.
     */
    public void delete() {
        gl.glDeleteProgram(programId);
    }

    private int convertTextureType(Texture texture) {
        switch (texture.getType()) {
            case TwoDimensional:
                return GL.GL_TEXTURE_2D;
            case ThreeDimensional:
                return GL2.GL_TEXTURE_3D;
            case CubeMap:
                return GL.GL_TEXTURE_CUBE_MAP;
            case TwoDimensionalArray:
                return GLExt.GL_TEXTURE_2D_ARRAY_EXT;
            default:
                throw new UnsupportedOperationException("Unsupported texture type: " + texture.getType());
        }
    }
}