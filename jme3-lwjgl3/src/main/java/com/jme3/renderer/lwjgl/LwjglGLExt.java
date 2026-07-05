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
 * The LWJGL implementation of {@link GLExt}.
 *
 * <p>Each method prefers the equivalent core OpenGL function (available since
 * the GL version that promoted the feature to core) and falls back to the
 * corresponding ARB extension only when the core entry point is unavailable.
 * This avoids JVM crashes on macOS Core Profile drivers that strip legacy ARB
 * extension pointers even when the underlying functionality is exposed through
 * the core profile.
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
        // glDrawArraysInstanced is core since OpenGL 3.1
        if (GL.getCapabilities().OpenGL31) {
            GL31.glDrawArraysInstanced(mode, first, count, primCount);
        } else {
            ARBDrawInstanced.glDrawArraysInstancedARB(mode, first, count, primCount);
        }
    }

    @Override
    public void glDrawBuffers(final IntBuffer bufs) {
        checkLimit(bufs);
        GL20.glDrawBuffers(bufs);
    }

    @Override
    public void glDrawElementsInstancedARB(final int mode, final int indicesCount, final int type,
                                           final long indicesBufferOffset, final int primCount) {
        // glDrawElementsInstanced is core since OpenGL 3.1
        if (GL.getCapabilities().OpenGL31) {
            GL31.glDrawElementsInstanced(mode, indicesCount, type, indicesBufferOffset, primCount);
        } else {
            ARBDrawInstanced.glDrawElementsInstancedARB(mode, indicesCount, type, indicesBufferOffset, primCount);
        }
    }

    @Override
    public void glGetMultisample(final int pname, final int index, final FloatBuffer val) {
        checkLimit(val);
        // glGetMultisamplefv is core since OpenGL 3.2
        if (GL.getCapabilities().OpenGL32) {
            GL32.glGetMultisamplefv(pname, index, val);
        } else {
            ARBTextureMultisample.glGetMultisamplefv(pname, index, val);
        }
    }

    @Override
    public void glTexImage2DMultisample(final int target, final int samples, final int internalFormat, final int width,
                                        final int height, final boolean fixedSampleLocations) {
        // glTexImage2DMultisample is core since OpenGL 3.2
        if (GL.getCapabilities().OpenGL32) {
            GL32.glTexImage2DMultisample(target, samples, internalFormat, width, height, fixedSampleLocations);
        } else {
            ARBTextureMultisample.glTexImage2DMultisample(target, samples, internalFormat, width, height, fixedSampleLocations);
        }
    }

    @Override
    public void glVertexAttribDivisorARB(final int index, final int divisor) {
        // glVertexAttribDivisor is core since OpenGL 3.3
        if (GL.getCapabilities().OpenGL33) {
            GL33.glVertexAttribDivisor(index, divisor);
        } else {
            ARBInstancedArrays.glVertexAttribDivisorARB(index, divisor);
        }
    }

    @Override
    public int glGetUniformBlockIndex(final int program, final String uniformBlockName) {
        // glGetUniformBlockIndex is core since OpenGL 3.1
        if (GL.getCapabilities().OpenGL31) {
            return GL31.glGetUniformBlockIndex(program, uniformBlockName);
        } else {
            return ARBUniformBufferObject.glGetUniformBlockIndex(program, uniformBlockName);
        }
    }

    @Override
    public void glBindBufferBase(final int target, final int index, final int buffer) {
        // glBindBufferBase is core since OpenGL 3.0
        if (GL.getCapabilities().OpenGL30) {
            GL30.glBindBufferBase(target, index, buffer);
        } else {
            ARBUniformBufferObject.glBindBufferBase(target, index, buffer);
        }
    }

    @Override
    public void glUniformBlockBinding(final int program, final int uniformBlockIndex, final int uniformBlockBinding) {
        // glUniformBlockBinding is core since OpenGL 3.1
        if (GL.getCapabilities().OpenGL31) {
            GL31.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
        } else {
            ARBUniformBufferObject.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
        }
    }

    @Override
    public int glGetProgramResourceIndex(final int program, final int programInterface, final String name) {
        // glGetProgramResourceIndex is core since OpenGL 4.3
        if (GL.getCapabilities().OpenGL43) {
            return GL43.glGetProgramResourceIndex(program, programInterface, name);
        } else {
            return ARBProgramInterfaceQuery.glGetProgramResourceIndex(program, programInterface, name);
        }
    }

    @Override
    public void glShaderStorageBlockBinding(final int program, final int storageBlockIndex, final int storageBlockBinding) {
        // glShaderStorageBlockBinding is core since OpenGL 4.3
        if (GL.getCapabilities().OpenGL43) {
            GL43.glShaderStorageBlockBinding(program, storageBlockIndex, storageBlockBinding);
        } else {
            ARBShaderStorageBufferObject.glShaderStorageBlockBinding(program, storageBlockIndex, storageBlockBinding);
        }
    }

    @Override
    public Object glFenceSync(final int condition, final int flags) {
        // glFenceSync is core since OpenGL 3.2
        if (GL.getCapabilities().OpenGL32) {
            return GL32.glFenceSync(condition, flags);
        } else {
            return ARBSync.glFenceSync(condition, flags);
        }
    }

    @Override
    public int glClientWaitSync(final Object sync, final int flags, final long timeout) {
        // glClientWaitSync is core since OpenGL 3.2
        if (GL.getCapabilities().OpenGL32) {
            return GL32.glClientWaitSync((Long) sync, flags, timeout);
        } else {
            return ARBSync.glClientWaitSync((Long) sync, flags, timeout);
        }
    }

    @Override
    public void glDeleteSync(final Object sync) {
        // glDeleteSync is core since OpenGL 3.2
        if (GL.getCapabilities().OpenGL32) {
            GL32.glDeleteSync((Long) sync);
        } else {
            ARBSync.glDeleteSync((Long) sync);
        }
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
