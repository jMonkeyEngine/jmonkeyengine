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

import org.lwjgl.opengl.EXTFramebufferBlit;
import org.lwjgl.opengl.EXTFramebufferMultisample;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.EXTTextureArray;

import java.nio.IntBuffer;

/**
 * Implements GLFbo via GL_EXT_framebuffer_object.
 *
 * @author Kirill Vainer
 * @deprecated use LWJGL3 bindings directly
 */
@Deprecated
public class LwjglGLFboEXT extends LwjglRender {

    
    public void glBlitFramebufferEXT(final int srcX0, final int srcY0, final int srcX1, final int srcY1,
                                     final int dstX0, final int dstY0, final int dstX1, final int dstY1, final int mask,
                                     final int filter) {
        EXTFramebufferBlit.glBlitFramebufferEXT(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }

    
    public void glRenderbufferStorageMultisampleEXT(final int target, final int samples, final int internalFormat,
                                                    final int width, final int height) {
        EXTFramebufferMultisample.glRenderbufferStorageMultisampleEXT(target, samples, internalFormat, width, height);
    }

    
    public void glBindFramebufferEXT(final int target, final int frameBuffer) {
        EXTFramebufferObject.glBindFramebufferEXT(target, frameBuffer);
    }

    
    public void glBindRenderbufferEXT(final int target, final int renderBuffer) {
        EXTFramebufferObject.glBindRenderbufferEXT(target, renderBuffer);
    }

    
    public int glCheckFramebufferStatusEXT(final int target) {
        return EXTFramebufferObject.glCheckFramebufferStatusEXT(target);
    }

    
    public void glDeleteFramebuffersEXT(final IntBuffer frameBuffers) {
        checkLimit(frameBuffers);
        EXTFramebufferObject.glDeleteFramebuffersEXT(frameBuffers);
    }

    
    public void glDeleteRenderbuffersEXT(final IntBuffer renderBuffers) {
        checkLimit(renderBuffers);
        EXTFramebufferObject.glDeleteRenderbuffersEXT(renderBuffers);
    }

    
    public void glFramebufferRenderbufferEXT(final int target, final int attachment, final int renderBufferTarget,
                                             final int renderBuffer) {
        EXTFramebufferObject.glFramebufferRenderbufferEXT(target, attachment, renderBufferTarget, renderBuffer);
    }

    
    public void glFramebufferTexture2DEXT(final int target, final int attachment, final int texTarget,
                                          final int texture, final int level) {
        EXTFramebufferObject.glFramebufferTexture2DEXT(target, attachment, texTarget, texture, level);
    }

    
    public void glGenFramebuffersEXT(final IntBuffer frameBuffers) {
        checkLimit(frameBuffers);
        EXTFramebufferObject.glGenFramebuffersEXT(frameBuffers);
    }

    
    public void glGenRenderbuffersEXT(final IntBuffer renderBuffers) {
        checkLimit(renderBuffers);
        EXTFramebufferObject.glGenRenderbuffersEXT(renderBuffers);
    }

    
    public void glGenerateMipmapEXT(final int target) {
        EXTFramebufferObject.glGenerateMipmapEXT(target);
    }

    
    public void glRenderbufferStorageEXT(final int target, final int internalFormat, final int width,
                                         final int height) {
        EXTFramebufferObject.glRenderbufferStorageEXT(target, internalFormat, width, height);
    }

    
    public void glFramebufferTextureLayerEXT(final int target, final int attachment, final int texture, final int level,
                                             final int layer) {
        EXTTextureArray.glFramebufferTextureLayerEXT(target, attachment, texture, level, layer);
    }
}
