/*
 * Copyright (c) 2009-2024 jMonkeyEngine
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
package com.jme3.shader;

import java.lang.ref.WeakReference;

import com.jme3.shader.bufferobject.BufferObject;

/**
 * Implementation of shader's buffer block.
 *
 * @author JavaSaBr, Riccardo Balbo
 */
public class ShaderBufferBlock extends ShaderVariable {
    
    public static enum BufferType {
        UniformBufferObject, ShaderStorageBufferObject
    }

    /**
     * Current used buffer object.
     */
    protected BufferObject bufferObject;
    protected WeakReference<BufferObject> bufferObjectRef;
    protected BufferType type;

    /**
     * Set the new buffer object.
     *
     * @param bufferObject
     *            the new buffer object.
     */
    public void setBufferObject(BufferType type, BufferObject bufferObject) {
        if (bufferObject == null) {
            throw new IllegalArgumentException("for storage block " + name + ": storageData cannot be null");
        }
        if (bufferObject == this.bufferObject && type == this.type) return;
        this.bufferObject = bufferObject;
        this.bufferObjectRef = new WeakReference<BufferObject>(bufferObject);
        this.type = type;
        updateNeeded = true;
    }

    public BufferType getType() {
        return type;
    }

    /**
     * Return true if need to update this storage block.
     *
     * @return true if need to update this storage block.
     */
    public boolean isUpdateNeeded(){
        return updateNeeded;
    }

    /**
     * Clear the flag {@link #isUpdateNeeded()}.
     */
    public void clearUpdateNeeded(){
        updateNeeded = false;
    }

    /**
     * Reset this storage block.
     */
    public void reset() {
        location = -1;
        updateNeeded = true;
    }

    /**
     * Get the current storage data.
     *
     * @return the current storage data.
     */
    public BufferObject getBufferObject() {
        return bufferObject;
    }

    public WeakReference<BufferObject> getBufferObjectRef() {
        return bufferObjectRef;
    }

    public void setBufferObjectRef(WeakReference<BufferObject> bufferObjectRef) {
        this.bufferObjectRef = bufferObjectRef;
    }

}
