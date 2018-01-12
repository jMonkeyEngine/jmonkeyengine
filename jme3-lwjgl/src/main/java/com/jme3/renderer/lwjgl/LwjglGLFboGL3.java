package com.jme3.renderer.lwjgl;

import com.jme3.renderer.RendererException;
import com.jme3.renderer.opengl.GLFbo;
import java.nio.Buffer;
import java.nio.IntBuffer;
import org.lwjgl.opengl.GL30;

/**
 * Implements GLFbo via OpenGL3+.
 * 
 * @author Kirill Vainer
 */
public final class LwjglGLFboGL3 implements GLFbo {

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
    public void glBlitFramebufferEXT(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        GL30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }
    
    @Override
    public void glRenderbufferStorageMultisampleEXT(int target, int samples, int internalformat, int width, int height) {
        GL30.glRenderbufferStorageMultisample(target, samples, internalformat, width, height);
    }
    
    @Override
    public void glBindFramebufferEXT(int param1, int param2) {
        GL30.glBindFramebuffer(param1, param2);
    }
    
    @Override
    public void glBindRenderbufferEXT(int param1, int param2) {
        GL30.glBindRenderbuffer(param1, param2);
    }
    
    @Override
    public int glCheckFramebufferStatusEXT(int param1) {
        return GL30.glCheckFramebufferStatus(param1);
    }
    
    @Override
    public void glDeleteFramebuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        GL30.glDeleteFramebuffers(param1);
    }
    
    @Override
    public void glDeleteRenderbuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        GL30.glDeleteRenderbuffers(param1);
    }
    
    @Override
    public void glFramebufferRenderbufferEXT(int param1, int param2, int param3, int param4) {
        GL30.glFramebufferRenderbuffer(param1, param2, param3, param4);
    }
    
    @Override
    public void glFramebufferTexture2DEXT(int param1, int param2, int param3, int param4, int param5) {
        GL30.glFramebufferTexture2D(param1, param2, param3, param4, param5);
    }
    
    @Override
    public void glGenFramebuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        GL30.glGenFramebuffers(param1);
    }
    
    @Override
    public void glGenRenderbuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        GL30.glGenRenderbuffers(param1);
    }
    
    @Override
    public void glGenerateMipmapEXT(int param1) {
        GL30.glGenerateMipmap(param1);
    }
    
    @Override
    public void glRenderbufferStorageEXT(int param1, int param2, int param3, int param4) {
        GL30.glRenderbufferStorage(param1, param2, param3, param4);
    }
    
    @Override
    public void glFramebufferTextureLayerEXT(int param1, int param2, int param3, int param4, int param5) {
        GL30.glFramebufferTextureLayer(param1, param2, param3, param4, param5);
    }
}
