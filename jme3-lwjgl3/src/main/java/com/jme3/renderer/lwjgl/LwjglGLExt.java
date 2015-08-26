package com.jme3.renderer.lwjgl;

import com.jme3.renderer.RendererException;
import com.jme3.renderer.opengl.GLExt;
import org.lwjgl.opengl.*;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class LwjglGLExt implements GLExt {

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

    @Override
    public void glBufferData(int target, IntBuffer data, int usage) {
        checkLimit(data);
        GL15.glBufferData(target, data, usage);
    }

    @Override
    public void glBufferSubData(int target, long offset, IntBuffer data) {
        checkLimit(data);
        GL15.glBufferSubData(target, offset, data);
    }

    @Override
    public void glDrawArraysInstancedARB(int mode, int first, int count, int primcount) {
        ARBDrawInstanced.glDrawArraysInstancedARB(mode, first, count, primcount);
    }

    @Override
    public void glDrawBuffers(IntBuffer bufs) {
        checkLimit(bufs);
        GL20.glDrawBuffers(bufs);
    }

    @Override
    public void glDrawElementsInstancedARB(int mode, int indices_count, int type, long indices_buffer_offset, int primcount) {
        ARBDrawInstanced.glDrawElementsInstancedARB(mode, indices_count, type, indices_buffer_offset, primcount);
    }

    @Override
    public void glGetMultisample(int pname, int index, FloatBuffer val) {
        checkLimit(val);
        ARBTextureMultisample.glGetMultisamplefv(pname, index, val);
    }

    @Override
    public void glTexImage2DMultisample(int target, int samples, int internalformat, int width, int height, boolean fixedsamplelocations) {
        ARBTextureMultisample.glTexImage2DMultisample(target, samples, internalformat, width, height, fixedsamplelocations);
    }

    @Override
    public void glVertexAttribDivisorARB(int index, int divisor) {
        ARBInstancedArrays.glVertexAttribDivisorARB(index, divisor);
    }

    @Override
    public Object glFenceSync(int condition, int flags) {
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
}
