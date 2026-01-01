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

import com.jme3.util.BufferUtils;

import java.nio.IntBuffer;

/**
 * <p><a target="_blank" href="https://wikis.khronos.org/opengl/Shader_Storage_Buffer_Object">Reference Page</a></p>
 * A Shader Storage Buffer Object (SSBO) for GPU read/write data storage.
 * <p>
 * SSBOs are buffers that can be read from and written to by shaders.
 * SSBOs require OpenGL 4.3 or higher.
 */
public class ShaderStorageBufferObject {

    private final GL4 gl;
    private final int bufferId;

    /**
     * Creates a new SSBO.
     *
     * @param gl the GL4 interface (required for glBindBufferBase)
     */
    public ShaderStorageBufferObject(GL4 gl) {
        this.gl = gl;
        IntBuffer buf = BufferUtils.createIntBuffer(1);
        gl.glGenBuffers(buf);
        this.bufferId = buf.get(0);
    }

    /**
     * Initializes the buffer with integer data.
     * @param data the initial data to upload
     */
    public void initialize(int[] data) {
        IntBuffer buffer = BufferUtils.createIntBuffer(data.length);
        buffer.put(data);
        buffer.flip();
        initialize(buffer);
    }

    /**
     * Initializes the buffer with an IntBuffer.
     *
     * @param data the initial data to upload
     */
    public void initialize(IntBuffer data) {
        gl.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, bufferId);
        gl.glBufferData(GL4.GL_SHADER_STORAGE_BUFFER, data, GL.GL_DYNAMIC_COPY);
    }

    /**
     * Reads integer data from the buffer.
     *
     * @param count the number of integers to read
     * @return an array containing the buffer data
     */
    public int[] read(int count) {
        int[] result = new int[count];
        read(result);
        return result;
    }

    /**
     * Reads integer data from the buffer into an existing array.
     *
     * @param destination the array to read into
     */
    public void read(int[] destination) {
        IntBuffer buffer = BufferUtils.createIntBuffer(destination.length);
        read(buffer);
        buffer.get(destination);
    }

    /**
     * Reads integer data from the buffer into an IntBuffer.
     *
     * @param destination the buffer to read into
     */
    public void read(IntBuffer destination) {
        gl.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, bufferId);
        gl.glGetBufferSubData(GL4.GL_SHADER_STORAGE_BUFFER, 0, destination);
        gl.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, 0);
    }

    /**
     * Deletes this buffer and releases GPU resources.
     * The buffer should not be used after calling this method.
     */
    public void delete() {
        IntBuffer buf = BufferUtils.createIntBuffer(1);
        buf.put(bufferId);
        buf.flip();
        gl.glDeleteBuffers(buf);
    }

    public int getBufferId() {
        return bufferId;
    }
}