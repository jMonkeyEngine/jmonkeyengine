package com.jme3.renderer.jogl;

import com.jme3.renderer.RendererException;
import com.jme3.renderer.opengl.GLExt;
import com.jogamp.opengl.GLContext;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class JoglGLExt implements GLExt {

	private static int getLimitBytes(IntBuffer buffer) {
        checkLimit(buffer);
        return buffer.limit() * 4;
    }
	
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
        GLContext.getCurrentGL().glBufferData(target, getLimitBytes(data), data, usage);
    }

    @Override
    public void glBufferSubData(int target, long offset, IntBuffer data) {
        checkLimit(data);
        GLContext.getCurrentGL().glBufferSubData(target, getLimitBytes(data), offset, data);
    }

    @Override
    public void glDrawArraysInstancedARB(int mode, int first, int count, int primcount) {
        GLContext.getCurrentGL().getGL2ES3().glDrawArraysInstanced(mode, first, count, primcount);
    }

    @Override
    public void glDrawBuffers(IntBuffer bufs) {
        checkLimit(bufs);
        GLContext.getCurrentGL().getGL2ES2().glDrawBuffers(bufs.limit(), bufs);
    }

    @Override
    public void glDrawElementsInstancedARB(int mode, int indices_count, int type, long indices_buffer_offset, int primcount) {
        GLContext.getCurrentGL().getGL2ES3().glDrawElementsInstanced(mode, indices_count, type, indices_buffer_offset, primcount);
    }

    @Override
    public void glGetMultisample(int pname, int index, FloatBuffer val) {
        checkLimit(val);
        GLContext.getCurrentGL().getGL2ES2().glGetMultisamplefv(pname, index, val);
    }

    @Override
    public void glTexImage2DMultisample(int target, int samples, int internalformat, int width, int height, boolean fixedsamplelocations) {
        GLContext.getCurrentGL().getGL2ES2().glTexImage2DMultisample(target, samples, internalformat, width, height, fixedsamplelocations);
    }

    @Override
    public void glVertexAttribDivisorARB(int index, int divisor) {
        GLContext.getCurrentGL().getGL2ES3().glVertexAttribDivisor(index, divisor);
    }

    @Override
    public Object glFenceSync(int condition, int flags) {
        return GLContext.getCurrentGL().getGL3ES3().glFenceSync(condition, flags);
    }
    
    @Override
    public int glClientWaitSync(Object sync, int flags, long timeout) {
        return GLContext.getCurrentGL().getGL3ES3().glClientWaitSync(((Long) sync).longValue(), flags, timeout);
    }

    @Override
    public void glDeleteSync(Object sync) {
        GLContext.getCurrentGL().getGL3ES3().glDeleteSync(((Long) sync).longValue());
    }
}
