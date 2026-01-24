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

import com.jme3.renderer.RendererException;
import com.jme3.util.BufferUtils;
import com.jme3.util.NativeObject;

import java.nio.IntBuffer;

/**
 * <p><a target="_blank" href="https://wikis.khronos.org/opengl/Shader_Storage_Buffer_Object">Reference Page</a></p>
 * A Shader Storage Buffer Object (SSBO) for GPU read/write data storage.
 * <p>
 * SSBOs are buffers that can be read from and written to by shaders.
 * SSBOs require OpenGL 4.3 or higher.
 */
public class ShaderStorageBufferObject extends NativeObject {

    private final GL4 gl;

    /**
     * Creates a new SSBO.
     *
     * @param gl the GL4 interface (required for glBindBufferBase)
     */
    public ShaderStorageBufferObject(GL4 gl) {
        super();
        this.gl = gl;
        ensureBufferReady();
    }
    private ShaderStorageBufferObject(ShaderStorageBufferObject source){
        super();
        this.gl = source.gl;
        this.id = source.id;
    }

    private void ensureBufferReady(){
        if(isUpdateNeeded()){
            IntBuffer buf = BufferUtils.createIntBuffer(1);
            gl.glGenBuffers(buf);
            this.id = buf.get(0);
            clearUpdateNeeded();
        }
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
        ensureBufferReady();
        gl.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, id);
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
        if(isUpdateNeeded()){
            //If the SSBO was deleted from e.g. context restart, it probably isn't sensible to read from it.
            //We could create a fresh empty buffer and read from that, but that might result in garbage data.
            throw new RendererException("SSBO was not ready for read");
        }
        gl.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, id);
        gl.glGetBufferSubData(GL4.GL_SHADER_STORAGE_BUFFER, 0, destination);
        gl.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, 0);
    }


    @Override
    public void resetObject() {
        this.id = INVALID_ID;
        setUpdateNeeded();
    }

    @Override
    public void deleteObject(Object rendererObject) {
        if(id != INVALID_ID){
            IntBuffer buf = BufferUtils.createIntBuffer(1);
            buf.put(id);
            buf.flip();
            gl.glDeleteBuffers(buf);
        }
        resetObject();
    }

    @Override
    public NativeObject createDestructableClone() {
        return new ShaderStorageBufferObject(this);
    }

    @Override
    public long getUniqueId() {
        return ((long) OBJTYPE_BO << 32) | (0xffffffffL & (long) id);
    }
}