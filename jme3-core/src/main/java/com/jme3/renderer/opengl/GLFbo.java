/*
 * Copyright (c) 2009-2014 jMonkeyEngine
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
package com.jme3.renderer.opengl;

import java.nio.IntBuffer;

/**
 * Framebuffer object functions.
 * <p>
 * Available by default in OpenGL ES 2, but on desktop GL 2
 * an extension is required.
 *
 * @author Kirill Vainer
 */
public interface GLFbo {

    int GL_COLOR_ATTACHMENT0_EXT = 0x8CE0;
    int GL_COLOR_ATTACHMENT1_EXT = 0x8CE1;
    int GL_COLOR_ATTACHMENT2_EXT = 0x8CE2;
    int GL_COLOR_ATTACHMENT3_EXT = 0x8CE3;
    int GL_COLOR_ATTACHMENT4_EXT = 0x8CE4;
    int GL_COLOR_ATTACHMENT5_EXT = 0x8CE5;
    int GL_COLOR_ATTACHMENT6_EXT = 0x8CE6;
    int GL_COLOR_ATTACHMENT7_EXT = 0x8CE7;
    int GL_COLOR_ATTACHMENT8_EXT = 0x8CE8;
    int GL_COLOR_ATTACHMENT9_EXT = 0x8CE9;
    int GL_COLOR_ATTACHMENT10_EXT = 0x8CEA;
    int GL_COLOR_ATTACHMENT11_EXT = 0x8CEB;
    int GL_COLOR_ATTACHMENT12_EXT = 0x8CEC;
    int GL_COLOR_ATTACHMENT13_EXT = 0x8CED;
    int GL_COLOR_ATTACHMENT14_EXT = 0x8CEE;
    int GL_COLOR_ATTACHMENT15_EXT = 0x8CEF;
    int GL_DEPTH_ATTACHMENT_EXT = 0x8D00;
    int GL_DRAW_FRAMEBUFFER_BINDING_EXT = 0x8CA6;
    int GL_DRAW_FRAMEBUFFER_EXT = 0x8CA9;
    int GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME_EXT = 0x8CD1;
    int GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE_EXT = 0x8CD0;
    int GL_FRAMEBUFFER_COMPLETE_EXT = 0x8CD5;
    int GL_FRAMEBUFFER_EXT = 0x8D40;
    int GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT = 0x8CD6;
    int GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT = 0x8CD9;
    int GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT = 0x8CDB;
    int GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT = 0x8CDA;
    int GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT = 0x8CD7;
    int GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE_EXT = 0x8D56;
    int GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT = 0x8CDC;
    int GL_FRAMEBUFFER_UNSUPPORTED_EXT = 0x8CDD;
    int GL_INVALID_FRAMEBUFFER_OPERATION_EXT = 0x506;
    int GL_MAX_COLOR_ATTACHMENTS_EXT = 0x8CDF;
    int GL_MAX_RENDERBUFFER_SIZE_EXT = 0x84E8;
    int GL_READ_FRAMEBUFFER_BINDING_EXT = 0x8CAA;
    int GL_READ_FRAMEBUFFER_EXT = 0x8CA8;
    int GL_RENDERBUFFER_EXT = 0x8D41;

    void glBindFramebufferEXT(int target, int frameBuffer);

    void glBindRenderbufferEXT(int target, int renderBuffer);

    void glBlitFramebufferEXT(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1,
                              int mask, int filter);

    int glCheckFramebufferStatusEXT(int target);

    void glDeleteFramebuffersEXT(IntBuffer frameBuffers);

    void glDeleteRenderbuffersEXT(IntBuffer renderBuffers);

    void glFramebufferRenderbufferEXT(int target, int attachment, int renderBufferTarget, int renderBuffer);

    void glFramebufferTexture2DEXT(int target, int attachment, int texTarget, int texture, int level);

    void glFramebufferTextureLayerEXT(int target, int attachment, int texture, int level, int layer);

    void glGenFramebuffersEXT(IntBuffer frameBuffers);

    void glGenRenderbuffersEXT(IntBuffer renderBuffers);

    void glGenerateMipmapEXT(int target);

    void glRenderbufferStorageEXT(int target, int internalFormat, int width, int height);

    void glRenderbufferStorageMultisampleEXT(int target, int samples, int internalFormat, int width, int height);
}
