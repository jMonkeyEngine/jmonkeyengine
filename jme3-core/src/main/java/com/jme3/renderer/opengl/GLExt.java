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

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * GL functions provided by extensions.
 * 
 * Always must check against a renderer capability prior to using those.
 * 
 * @author Kirill Vainer
 */
public interface GLExt {

        public static final int GL_ALREADY_SIGNALED = 0x911A;
	public static final int GL_COMPRESSED_RGB8_ETC2 = 0x9274;
	public static final int GL_COMPRESSED_RGBA_S3TC_DXT1_EXT = 0x83F1;
	public static final int GL_COMPRESSED_RGBA_S3TC_DXT3_EXT = 0x83F2;
	public static final int GL_COMPRESSED_RGBA_S3TC_DXT5_EXT = 0x83F3;
	public static final int GL_COMPRESSED_RGB_S3TC_DXT1_EXT = 0x83F0;
	public static final int GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT1_EXT = 0x8C4D;
	public static final int GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT3_EXT = 0x8C4E;
	public static final int GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT5_EXT = 0x8C4F;
	public static final int GL_COMPRESSED_SRGB_S3TC_DXT1_EXT = 0x8C4C;
        public static final int GL_CONDITION_SATISFIED = 0x911C;
        public static final int GL_DEPTH_COMPONENT32F = 0x8CAC;
	public static final int GL_DEPTH24_STENCIL8_EXT = 0x88F0;
	public static final int GL_DEPTH_STENCIL_EXT = 0x84F9;
        public static final int GL_ETC1_RGB8_OES = 0x8D64;
	public static final int GL_FRAMEBUFFER_SRGB_CAPABLE_EXT = 0x8DBA;
	public static final int GL_FRAMEBUFFER_SRGB_EXT = 0x8DB9;
	public static final int GL_HALF_FLOAT_ARB = 0x140B;
	public static final int GL_LUMINANCE16F_ARB = 0x881E;
	public static final int GL_LUMINANCE32F_ARB = 0x8818;
	public static final int GL_LUMINANCE_ALPHA16F_ARB = 0x881F;
	public static final int GL_MAX_COLOR_TEXTURE_SAMPLES = 0x910E;
	public static final int GL_MAX_DEPTH_TEXTURE_SAMPLES = 0x910F;
        public static final int GL_MAX_DRAW_BUFFERS_ARB = 0x8824;
	public static final int GL_MAX_SAMPLES_EXT = 0x8D57;
        public static final int GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT = 0x84FF;
	public static final int GL_MULTISAMPLE_ARB = 0x809D;
        public static final int GL_NUM_PROGRAM_BINARY_FORMATS = 0x87FE;
        public static final int GL_PIXEL_PACK_BUFFER_ARB = 0x88EB;
        public static final int GL_PIXEL_UNPACK_BUFFER_ARB = 0x88EC;
	public static final int GL_R11F_G11F_B10F_EXT = 0x8C3A;
        public static final int GL_RGBA8 = 0x8058;
	public static final int GL_RGB16F_ARB = 0x881B;
	public static final int GL_RGB32F_ARB = 0x8815;
	public static final int GL_RGB9_E5_EXT = 0x8C3D;
	public static final int GL_RGBA16F_ARB = 0x881A;
	public static final int GL_RGBA32F_ARB = 0x8814;
	public static final int GL_SAMPLES_ARB = 0x80A9;
	public static final int GL_SAMPLE_ALPHA_TO_COVERAGE_ARB = 0x809E;
	public static final int GL_SAMPLE_BUFFERS_ARB = 0x80A8;
	public static final int GL_SAMPLE_POSITION = 0x8E50;
	public static final int GL_SLUMINANCE8_ALPHA8_EXT = 0x8C45;
	public static final int GL_SLUMINANCE8_EXT = 0x8C47;
	public static final int GL_SRGB8_ALPHA8_EXT = 0x8C43;
	public static final int GL_SRGB8_EXT = 0x8C41;
        public static final int GL_SYNC_FLUSH_COMMANDS_BIT = 0x1;
        public static final int GL_SYNC_GPU_COMMANDS_COMPLETE = 0x9117;
	public static final int GL_TEXTURE_2D_ARRAY_EXT = 0x8C1A;
	public static final int GL_TEXTURE_2D_MULTISAMPLE = 0x9100;
	public static final int GL_TEXTURE_2D_MULTISAMPLE_ARRAY = 0x9102;
        public static final int GL_TEXTURE_CUBE_MAP_SEAMLESS = 0x884F;
	public static final int GL_TEXTURE_MAX_ANISOTROPY_EXT = 0x84FE;
        public static final int GL_TIMEOUT_EXPIRED = 0x911B;
	public static final int GL_UNSIGNED_INT_10F_11F_11F_REV_EXT = 0x8C3B;
	public static final int GL_UNSIGNED_INT_24_8_EXT = 0x84FA;
	public static final int GL_UNSIGNED_INT_5_9_9_9_REV_EXT = 0x8C3E;
        public static final int GL_WAIT_FAILED = 0x911D;

        public void glBufferData(int target, IntBuffer data, int usage);
        public void glBufferSubData(int target, long offset, IntBuffer data);
        public int glClientWaitSync(Object sync, int flags, long timeout);
        public void glDeleteSync(Object sync);
	public void glDrawArraysInstancedARB(int mode, int first, int count, int primcount);
        public void glDrawBuffers(IntBuffer bufs);
	public void glDrawElementsInstancedARB(int mode, int indices_count, int type, long indices_buffer_offset, int primcount);
        public Object glFenceSync(int condition, int flags);
	public void glGetMultisample(int pname, int index, FloatBuffer val);
	public void glTexImage2DMultisample(int target, int samples, int internalformat, int width, int height, boolean fixedsamplelocations);
	public void glVertexAttribDivisorARB(int index, int divisor);
}
