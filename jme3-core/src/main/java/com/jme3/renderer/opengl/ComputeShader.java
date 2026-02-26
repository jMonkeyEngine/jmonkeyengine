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
import com.jme3.util.BufferUtils;
import com.jme3.util.NativeObject;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * A compute shader for general-purpose GPU computing (GPGPU).
 * <p>
 * Compute shaders require OpenGL 4.3 or higher.
 */
public class ComputeShader extends NativeObject {

    private final GL4 gl;
    // The source need not be stored, but it helps with debugging.
    private final String source;

    /**
     * Creates a new compute shader from GLSL source code.
     */
    public ComputeShader(GL4 gl, String source) {
        super();
        this.gl = gl;
        this.source = source;
        // Load this up front to surface any problems at init time.
        createComputeShader();
    }

    /**
     * Creates a new compute shader from GLSL source code and a set of defines.
     *
     * @param defines An array of string pairs. The first element of the pair
     *                is the macro name; the second element, the definition.
     */
    public ComputeShader(GL4 gl, String source, String[][] defines) {
        super();
        this.gl = gl;
        this.source = addDefines(source, defines);
        // Load this up front to surface any problems at init time.
        createComputeShader();
    }

    private ComputeShader(ComputeShader source) {
        super();
        this.gl = source.gl;
        this.id = source.id;
        this.source = null;
    }

    private String addDefines(String source, String[][] defines) {
        // The #version pragma must appear before anything else. Insert the
        // defines after it.
        String[] sourceLines = (String[])source.split("\\r?\\n", 2);
        StringBuilder builder = new StringBuilder();
        builder.append(sourceLines[0] + "\n");
        for (String[] pair : defines) {
            builder.append("#define " + pair[0] + " " + pair[1] + "\n");
        }
        builder.append(sourceLines[1] + "\n");
        return builder.toString();
    }

    private void createComputeShader() {
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
        id = gl.glCreateProgram();
        if (id <= 0) {
            gl.glDeleteShader(shaderId);
            throw new RendererException("Failed to create shader program");
        }

        gl.glAttachShader(id, shaderId);
        gl.glLinkProgram(id);

        // Check link status
        gl.glGetProgram(id, GL.GL_LINK_STATUS, intBuf);
        if (intBuf.get(0) != GL.GL_TRUE) {
            gl.glGetProgram(id, GL.GL_INFO_LOG_LENGTH, intBuf);
            String infoLog = gl.glGetProgramInfoLog(id, intBuf.get(0));
            gl.glDeleteShader(shaderId);
            gl.glDeleteProgram(id);
            throw new RendererException("Compute shader program linking failed: " + infoLog);
        }

        // Shader object can be deleted after linking
        gl.glDeleteShader(shaderId);

        clearUpdateNeeded();
    }

    /**
     * Activates this compute shader for use.
     * Must be called before setting uniforms or dispatching.
     */
    public void makeActive() {
        if(isUpdateNeeded()){
            createComputeShader();
        }
        gl.glUseProgram(id);
    }

    /**
     * Dispatches the compute shader with the specified number of work groups.
     */
    public void dispatch(int numGroupsX, int numGroupsY, int numGroupsZ) {
        gl.glDispatchCompute(numGroupsX, numGroupsY, numGroupsZ);
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
        return gl.glGetUniformLocation(id, name);
    }

    public void bindShaderStorageBuffer(int location, ShaderStorageBufferObject ssbo) {
        gl.glBindBufferBase(GL4.GL_SHADER_STORAGE_BUFFER, location, ssbo.getId());
    }

    @Override
    public void resetObject() {
        id = INVALID_ID;
        setUpdateNeeded();
    }

    @Override
    public void deleteObject(Object rendererObject) {
        if(id != INVALID_ID){
            gl.glDeleteProgram(id);
        }
        resetObject();
    }

    @Override
    public NativeObject createDestructableClone() {
        return new ComputeShader(this);
    }

    @Override
    public long getUniqueId() {
        //Note this is the same type of ID as a regular shader.
        return ((long)OBJTYPE_SHADER << 32) | (0xffffffffL & (long)id);
    }
}