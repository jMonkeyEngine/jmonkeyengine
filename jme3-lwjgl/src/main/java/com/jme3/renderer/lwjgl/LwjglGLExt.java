package com.jme3.renderer.lwjgl;

import com.jme3.renderer.opengl.GLExt;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.opengl.ARBDrawInstanced;
import org.lwjgl.opengl.ARBInstancedArrays;
import org.lwjgl.opengl.ARBTextureMultisample;
import org.lwjgl.opengl.EXTFramebufferBlit;
import org.lwjgl.opengl.EXTFramebufferMultisample;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

public class LwjglGLExt implements GLExt {

    public void glBlitFramebufferEXT(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        EXTFramebufferBlit.glBlitFramebufferEXT(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }

    public void glBufferData(int target, IntBuffer data, int usage) {
        GL15.glBufferData(target, data, usage);
    }

    public void glBufferSubData(int target, long offset, IntBuffer data) {
        GL15.glBufferSubData(target, offset, data);
    }

    public void glDrawArraysInstancedARB(int mode, int first, int count, int primcount) {
        ARBDrawInstanced.glDrawArraysInstancedARB(mode, first, count, primcount);
    }

    public void glDrawBuffers(IntBuffer bufs) {
        GL20.glDrawBuffers(bufs);
    }

    public void glDrawElementsInstancedARB(int mode, int indices_count, int type, long indices_buffer_offset, int primcount) {
        ARBDrawInstanced.glDrawElementsInstancedARB(mode, indices_count, type, indices_buffer_offset, primcount);
    }

    public void glGetMultisample(int pname, int index, FloatBuffer val) {
        ARBTextureMultisample.glGetMultisample(pname, index, val);
    }

    public void glRenderbufferStorageMultisampleEXT(int target, int samples, int internalformat, int width, int height) {
        EXTFramebufferMultisample.glRenderbufferStorageMultisampleEXT(target, samples, internalformat, width, height);
    }

    public void glTexImage2DMultisample(int target, int samples, int internalformat, int width, int height, boolean fixedsamplelocations) {
        ARBTextureMultisample.glTexImage2DMultisample(target, samples, internalformat, width, height, fixedsamplelocations);
    }

    public void glVertexAttribDivisorARB(int index, int divisor) {
        ARBInstancedArrays.glVertexAttribDivisorARB(index, divisor);
    }

    public void glBindFramebufferEXT(int param1, int param2) {
        EXTFramebufferObject.glBindFramebufferEXT(param1, param2);
    }

    public void glBindRenderbufferEXT(int param1, int param2) {
        EXTFramebufferObject.glBindRenderbufferEXT(param1, param2);
    }

    public int glCheckFramebufferStatusEXT(int param1) {
        return EXTFramebufferObject.glCheckFramebufferStatusEXT(param1);
    }

    public void glDeleteFramebuffersEXT(IntBuffer param1) {
        EXTFramebufferObject.glDeleteFramebuffersEXT(param1);
    }

    public void glDeleteRenderbuffersEXT(IntBuffer param1) {
        EXTFramebufferObject.glDeleteRenderbuffersEXT(param1);
    }

    public void glFramebufferRenderbufferEXT(int param1, int param2, int param3, int param4) {
        EXTFramebufferObject.glFramebufferRenderbufferEXT(param1, param2, param3, param4);
    }

    public void glFramebufferTexture2DEXT(int param1, int param2, int param3, int param4, int param5) {
        EXTFramebufferObject.glFramebufferTexture2DEXT(param1, param2, param3, param4, param5);
    }

    public void glGenFramebuffersEXT(IntBuffer param1) {
        EXTFramebufferObject.glGenFramebuffersEXT(param1);
    }

    public void glGenRenderbuffersEXT(IntBuffer param1) {
        EXTFramebufferObject.glGenRenderbuffersEXT(param1);
    }

    public void glGenerateMipmapEXT(int param1) {
        EXTFramebufferObject.glGenerateMipmapEXT(param1);
    }

    public void glRenderbufferStorageEXT(int param1, int param2, int param3, int param4) {
        EXTFramebufferObject.glRenderbufferStorageEXT(param1, param2, param3, param4);
    }
}
