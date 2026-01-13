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

/**
 * Wrapper for an OpenGL sync object (fence).
 * <p><a target="_blank" href="https://registry.khronos.org/OpenGL-Refpages/gl4/html/glFenceSync.xhtml">See here.</a></p>
 * <p>
 * A fence is a synchronization primitive that can be used to coordinate
 * work between the CPU and GPU. Once inserted into the command stream,
 * the GPU will signal the fence when all preceding commands have completed.
 * <p>
 * This class wraps the native sync handle (either a long address or a GLSync)
 *
 * @see GL4#glFenceSync(int, int)
 * @see GL4#glClientWaitSync(GLFence, int, long)
 * @see GL4#glDeleteSync(GLFence)
 */
public class GLFence {

    private final long handle;
    private Object nativeSync;

    /**
     * Creates a new fence wrapper with the given handle.
     *
     * @param handle the native sync object handle (pointer)
     */
    public GLFence(long handle) {
        this.handle = handle;
    }

    /**
     * Creates a new fence wrapper with the given handle and native sync object.
     *
     * @param handle the native sync object handle (pointer)
     * @param nativeSync the backend-specific sync object (e.g., LWJGL2's GLSync)
     */
    public GLFence(long handle, Object nativeSync) {
        this.handle = handle;
        this.nativeSync = nativeSync;
    }

    /**
     * Returns the native sync object handle.
     *
     * @return the sync handle
     */
    public long getHandle() {
        return handle;
    }

    /**
     * Returns the backend-specific native sync object, if set.
     * <p>
     * This is used by LWJGL2 that require their own GLSync
     * object type rather than a raw pointer.
     *
     * @return the native sync object, or null if not set
     */
    public Object getNativeSync() {
        return nativeSync;
    }
}