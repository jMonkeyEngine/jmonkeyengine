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

import com.jme3.renderer.RendererException;
import com.jme3.renderer.opengl.GLFbo;
import org.lwjgl.opengl.EXTFramebufferBlit;
import org.lwjgl.opengl.EXTFramebufferMultisample;
import org.lwjgl.opengl.EXTFramebufferObject;

import java.nio.Buffer;
import java.nio.IntBuffer;
import org.lwjgl.opengl.EXTTextureArray;

/**
 * Implements GLFbo via GL_EXT_framebuffer_object.
 * 
 * @author Kirill Vainer
 */
public class LwjglGLFboEXT implements GLFbo {

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
        EXTFramebufferBlit.glBlitFramebufferEXT(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }
    
    @Override
    public void glRenderbufferStorageMultisampleEXT(int target, int samples, int internalformat, int width, int height) {
        EXTFramebufferMultisample.glRenderbufferStorageMultisampleEXT(target, samples, internalformat, width, height);
    }
    
    @Override
    public void glBindFramebufferEXT(int param1, int param2) {
        EXTFramebufferObject.glBindFramebufferEXT(param1, param2);
    }
    
    @Override
    public void glBindRenderbufferEXT(int param1, int param2) {
        EXTFramebufferObject.glBindRenderbufferEXT(param1, param2);
    }
    
    @Override
    public int glCheckFramebufferStatusEXT(int param1) {
        return EXTFramebufferObject.glCheckFramebufferStatusEXT(param1);
    }
    
    @Override
    public void glDeleteFramebuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        EXTFramebufferObject.glDeleteFramebuffersEXT(param1);
    }
    
    @Override
    public void glDeleteRenderbuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        EXTFramebufferObject.glDeleteRenderbuffersEXT(param1);
    }
    
    @Override
    public void glFramebufferRenderbufferEXT(int param1, int param2, int param3, int param4) {
        EXTFramebufferObject.glFramebufferRenderbufferEXT(param1, param2, param3, param4);
    }
    
    @Override
    public void glFramebufferTexture2DEXT(int param1, int param2, int param3, int param4, int param5) {
        EXTFramebufferObject.glFramebufferTexture2DEXT(param1, param2, param3, param4, param5);
    }
    
    @Override
    public void glGenFramebuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        EXTFramebufferObject.glGenFramebuffersEXT(param1);
    }
    
    @Override
    public void glGenRenderbuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        EXTFramebufferObject.glGenRenderbuffersEXT(param1);
    }
    
    @Override
    public void glGenerateMipmapEXT(int param1) {
        EXTFramebufferObject.glGenerateMipmapEXT(param1);
    }
    
    @Override
    public void glRenderbufferStorageEXT(int param1, int param2, int param3, int param4) {
        EXTFramebufferObject.glRenderbufferStorageEXT(param1, param2, param3, param4);
    }
    
    @Override
    public void glFramebufferTextureLayerEXT(int target, int attachment, int texture, int level, int layer) {
        EXTTextureArray.glFramebufferTextureLayerEXT(target, attachment, texture, level, layer);
    }
}
