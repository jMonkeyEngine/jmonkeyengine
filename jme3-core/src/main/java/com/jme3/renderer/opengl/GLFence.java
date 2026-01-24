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

import com.jme3.renderer.Renderer;
import com.jme3.util.NativeObject;

import java.util.concurrent.atomic.AtomicInteger;

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
public class GLFence extends NativeObject {
    private final static AtomicInteger nextUniqueId = new AtomicInteger(1);
    private Object nativeSync;


    /**
     * Most NativeObject implementations use the int handle GL returns as the NativeObjectId.
     * However, fence IDs are actually longs.
     * (This probably won't cause overflow issues; you're not likely to have 2 billion fences usefully around at once.)
     */
    private final long fenceId;

    /**
     * Creates a new fence wrapper with the given handle and native sync object.
     *
     * @param fenceId the native sync object handle (pointer)
     * @param nativeSync the backend-specific sync object (e.g., LWJGL2's GLSync) or null
     */
    public GLFence(long fenceId, Object nativeSync) {
        super();
        this.fenceId = fenceId;
        this.id = nextUniqueId.getAndIncrement();
        this.nativeSync = nativeSync;
        clearUpdateNeeded();
    }
    private GLFence(GLFence source) {
        super();
        this.fenceId = source.fenceId;
        this.nativeSync = source.nativeSync;
        this.id = source.id;
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
    public long getFenceId() {
        return fenceId;
    }

    @Override
    public void resetObject() {
        this.nativeSync = null;
        this.id = INVALID_ID;
        setUpdateNeeded();
    }

    @Override
    public void deleteObject(Object rendererObject) {
        ((Renderer)rendererObject).deleteFence(this);
    }

    @Override
    public NativeObject createDestructableClone() {
        return new GLFence(this);
    }

    @Override
    public long getUniqueId() {
        return ((long) OBJTYPE_FENCE << 32) | (0xffffffffL & (long) id);
    }
}