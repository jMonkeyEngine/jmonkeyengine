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

import com.jme3.renderer.opengl.GLExt;
import org.lwjgl.opengl.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * The LWJGL implementation og {@link GLExt}.
 */
public class LwjglGLExt extends LwjglRender implements GLExt {

    @Override
    public void glBufferData(final int target, final IntBuffer data, final int usage) {
        checkLimit(data);
        GL15.glBufferData(target, data, usage);
    }

    @Override
    public void glBufferSubData(final int target, final long offset, final IntBuffer data) {
        checkLimit(data);
        GL15.glBufferSubData(target, offset, data);
    }

    @Override
    public void glDrawArraysInstancedARB(final int mode, final int first, final int count, final int primCount) {
        ARBDrawInstanced.glDrawArraysInstancedARB(mode, first, count, primCount);
    }

    @Override
    public void glDrawBuffers(final IntBuffer bufs) {
        checkLimit(bufs);
        GL20.glDrawBuffers(bufs);
    }

    @Override
    public void glDrawElementsInstancedARB(final int mode, final int indicesCount, final int type,
                                           final long indicesBufferOffset, final int primCount) {
        ARBDrawInstanced.glDrawElementsInstancedARB(mode, indicesCount, type, indicesBufferOffset, primCount);
    }

    @Override
    public void glGetMultisample(final int pname, final int index, final FloatBuffer val) {
        checkLimit(val);
        ARBTextureMultisample.glGetMultisamplefv(pname, index, val);
    }

    @Override
    public void glTexImage2DMultisample(final int target, final int samples, final int internalFormat, final int width,
                                        final int height, final boolean fixedSampleLocations) {
        ARBTextureMultisample.glTexImage2DMultisample(target, samples, internalFormat, width, height, fixedSampleLocations);
    }

    @Override
    public void glVertexAttribDivisorARB(final int index, final int divisor) {
        ARBInstancedArrays.glVertexAttribDivisorARB(index, divisor);
    }

    @Override
    public Object glFenceSync(final int condition, final int flags) {
        return ARBSync.glFenceSync(condition, flags);
    }
    
    @Override
    public int glClientWaitSync(final Object sync, final int flags, final long timeout) {
        return ARBSync.glClientWaitSync((Long) sync, flags, timeout);
    }

    @Override
    public void glDeleteSync(final Object sync) {
        ARBSync.glDeleteSync((Long) sync);
    }

    @Override
    public void glPushDebugGroup(int source, int id, String message) {
        KHRDebug.glPushDebugGroup(source, id, message);
    }

    @Override
    public void glPopDebugGroup() {
        KHRDebug.glPopDebugGroup();
    }

    @Override
    public void glObjectLabel(int identifier, int id, String label) {
        assert label != null;
        KHRDebug.glObjectLabel(identifier, id, label);
    }
}
