package com.jme3.renderer.lwjgl;

import com.jme3.renderer.RendererException;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.opengl.ARBDrawInstanced;
import org.lwjgl.opengl.ARBInstancedArrays;
import org.lwjgl.opengl.ARBSync;
import org.lwjgl.opengl.ARBTextureMultisample;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLSync;

/**
 * Old layer over OpenGL EXT extensions that allowed different versions
 * of LWJGL to be used in the engine.
 *
 * @deprecated use LWJGL3 bindings directly
 */
@Deprecated
public final class LwjglGLExt {

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

    
    public void glBufferData(int target, IntBuffer data, int usage) {
        checkLimit(data);
        GL15.glBufferData(target, data, usage);
    }

    
    public void glBufferSubData(int target, long offset, IntBuffer data) {
        checkLimit(data);
        GL15.glBufferSubData(target, offset, data);
    }

    
    public void glDrawArraysInstancedARB(int mode, int first, int count, int primcount) {
        ARBDrawInstanced.glDrawArraysInstancedARB(mode, first, count, primcount);
    }

    
    public void glDrawBuffers(IntBuffer bufs) {
        checkLimit(bufs);
        GL20.glDrawBuffers(bufs);
    }

    
    public void glDrawElementsInstancedARB(int mode, int indicesCount, int type, long indicesBufferOffset, int primcount) {
        ARBDrawInstanced.glDrawElementsInstancedARB(mode, indicesCount, type, indicesBufferOffset, primcount);
    }

    
    public void glGetMultisample(int pname, int index, FloatBuffer val) {
        checkLimit(val);
        ARBTextureMultisample.glGetMultisample(pname, index, val);
    }

    
    public void glTexImage2DMultisample(int target, int samples, int internalformat, int width, int height, boolean fixedSampleLocations) {
        ARBTextureMultisample.glTexImage2DMultisample(target, samples, internalformat, width, height, fixedSampleLocations);
    }

    
    public void glVertexAttribDivisorARB(int index, int divisor) {
        ARBInstancedArrays.glVertexAttribDivisorARB(index, divisor);
    }

    
    public Object glFenceSync(int condition, int flags) {
        return ARBSync.glFenceSync(condition, flags);
    }
    
    
    public int glClientWaitSync(Object sync, int flags, long timeout) {
        return ARBSync.glClientWaitSync((GLSync) sync, flags, timeout);
    }

    
    public void glDeleteSync(Object sync) {
        ARBSync.glDeleteSync((GLSync) sync);
    }
}
