package com.jme3.renderer.jogl;

import com.jme3.renderer.RendererException;
import com.jme3.renderer.opengl.GLFbo;
import com.jogamp.opengl.GLContext;

import java.nio.Buffer;
import java.nio.IntBuffer;

/**
 * Implements GLFbo
 * 
 * @author Kirill Vainer
 */
public class JoglGLFbo implements GLFbo {

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
        GLContext.getCurrentGL().getGL2ES3().glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }
    
    @Override
    public void glRenderbufferStorageMultisampleEXT(int target, int samples, int internalformat, int width, int height) {
        GLContext.getCurrentGL().glRenderbufferStorageMultisample(target, samples, internalformat, width, height);
    }
    
    @Override
    public void glBindFramebufferEXT(int param1, int param2) {
        GLContext.getCurrentGL().glBindFramebuffer(param1, param2);
    }
    
    @Override
    public void glBindRenderbufferEXT(int param1, int param2) {
        GLContext.getCurrentGL().glBindRenderbuffer(param1, param2);
    }
    
    @Override
    public int glCheckFramebufferStatusEXT(int param1) {
        return GLContext.getCurrentGL().glCheckFramebufferStatus(param1);
    }
    
    @Override
    public void glDeleteFramebuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        GLContext.getCurrentGL().glDeleteFramebuffers(param1.limit(), param1);
    }
    
    @Override
    public void glDeleteRenderbuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        GLContext.getCurrentGL().glDeleteRenderbuffers(param1.limit(), param1);
    }
    
    @Override
    public void glFramebufferRenderbufferEXT(int param1, int param2, int param3, int param4) {
        GLContext.getCurrentGL().glFramebufferRenderbuffer(param1, param2, param3, param4);
    }
    
    @Override
    public void glFramebufferTexture2DEXT(int param1, int param2, int param3, int param4, int param5) {
        GLContext.getCurrentGL().glFramebufferTexture2D(param1, param2, param3, param4, param5);
    }
    
    @Override
    public void glGenFramebuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        GLContext.getCurrentGL().glGenFramebuffers(param1.limit(), param1);
    }
    
    @Override
    public void glGenRenderbuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        GLContext.getCurrentGL().glGenRenderbuffers(param1.limit(), param1);
    }
    
    @Override
    public void glGenerateMipmapEXT(int param1) {
        GLContext.getCurrentGL().glGenerateMipmap(param1);
    }
    
    @Override
    public void glRenderbufferStorageEXT(int param1, int param2, int param3, int param4) {
        GLContext.getCurrentGL().glRenderbufferStorage(param1, param2, param3, param4);
    }
    
    @Override
    public void glFramebufferTextureLayerEXT(int param1, int param2, int param3, int param4, int param5) {
        GLContext.getCurrentGL().getGL3().glFramebufferTextureLayer(param1, param2, param3, param4, param5);
    }
}
