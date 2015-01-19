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
 * 
 * Available by default in OpenGL ES 2, but on desktop GL 2 
 * an extension is required.
 * 
 * @author Kirill Vainer
 */
public interface GLFbo {
    
    public static final int GL_COLOR_ATTACHMENT0_EXT = 0x8CE0;
    public static final int GL_COLOR_ATTACHMENT15_EXT = 0x8CEF;
    public static final int GL_DEPTH_ATTACHMENT_EXT = 0x8D00;
    public static final int GL_DRAW_FRAMEBUFFER_BINDING_EXT = 0x8CA6;
    public static final int GL_DRAW_FRAMEBUFFER_EXT = 0x8CA9;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME_EXT = 0x8CD1;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE_EXT = 0x8CD0;
    public static final int GL_FRAMEBUFFER_COMPLETE_EXT = 0x8CD5;
    public static final int GL_FRAMEBUFFER_EXT = 0x8D40;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT = 0x8CD6;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT = 0x8CD9;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT = 0x8CDB;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT = 0x8CDA;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT = 0x8CD7;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE_EXT = 0x8D56;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT = 0x8CDC;
    public static final int GL_FRAMEBUFFER_UNSUPPORTED_EXT = 0x8CDD;
    public static final int GL_INVALID_FRAMEBUFFER_OPERATION_EXT = 0x506;
    public static final int GL_MAX_COLOR_ATTACHMENTS_EXT = 0x8CDF;
    public static final int GL_MAX_RENDERBUFFER_SIZE_EXT = 0x84E8;
    public static final int GL_READ_FRAMEBUFFER_BINDING_EXT = 0x8CAA;
    public static final int GL_READ_FRAMEBUFFER_EXT = 0x8CA8;
    public static final int GL_RENDERBUFFER_EXT = 0x8D41;
    
    public void glBindFramebufferEXT(int param1, int param2);
    public void glBindRenderbufferEXT(int param1, int param2);
    public int glCheckFramebufferStatusEXT(int param1);
    public void glDeleteFramebuffersEXT(IntBuffer param1);
    public void glDeleteRenderbuffersEXT(IntBuffer param1);
    public void glFramebufferRenderbufferEXT(int param1, int param2, int param3, int param4);
    public void glFramebufferTexture2DEXT(int param1, int param2, int param3, int param4, int param5);
    public void glGenFramebuffersEXT(IntBuffer param1);
    public void glGenRenderbuffersEXT(IntBuffer param1);
    public void glGenerateMipmapEXT(int param1);
    public void glRenderbufferStorageEXT(int param1, int param2, int param3, int param4);
    
}
