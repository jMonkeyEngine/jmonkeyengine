/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
 * <p>
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
    public static final int GL_HALF_FLOAT_OES = 0x8D61;
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
    
    // OpenGL 4.2 texture compression, we now check these through the extension
    public static final int GL_COMPRESSED_RGB_BPTC_SIGNED_FLOAT = 0x8E8E;
    public static final int GL_COMPRESSED_RGB_BPTC_UNSIGNED_FLOAT = 0x8E8F;
    public static final int GL_COMPRESSED_RGBA_BPTC_UNORM = 0x8E8C;
    public static final int GL_COMPRESSED_SRGB_ALPHA_BPTC_UNORM = 0x8E8D;

    public static final int GL_DEBUG_SOURCE_API = 0x8246;
    public static final int GL_DEBUG_SOURCE_WINDOW_SYSTEM = 0x8247;
    public static final int GL_DEBUG_SOURCE_SHADER_COMPILER = 0x8248;
    public static final int GL_DEBUG_SOURCE_THIRD_PARTY = 0x8249;
    public static final int GL_DEBUG_SOURCE_APPLICATION = 0x824A;
    public static final int GL_DEBUG_SOURCE_OTHER = 0x824B;

    public static final int GL_BUFFER = 0x82E0;
    public static final int GL_SHADER = 0x82E1;
    public static final int GL_PROGRAM = 0x82E2;
    public static final int GL_QUERY = 0x82E3;
    public static final int GL_PROGRAM_PIPELINE = 0x82E4;
    public static final int GL_SAMPLER = 0x82E6;
    public static final int GL_DISPLAY_LIST = 0x82E7;

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glBufferData">Reference Page</a></p>
     *
     * Creates and initializes a buffer object's data store.
     *
     * <p>{@code usage} is a hint to the GL implementation as to how a buffer object's data store will be accessed. This enables the GL implementation to make
     * more intelligent decisions that may significantly impact buffer object performance. It does not, however, constrain the actual usage of the data store.
     * {@code usage} can be broken down into two parts: first, the frequency of access (modification and usage), and second, the nature of that access. The
     * frequency of access may be one of these:</p>
     *
     * <ul>
     * <li><em>STREAM</em> - The data store contents will be modified once and used at most a few times.</li>
     * <li><em>STATIC</em> - The data store contents will be modified once and used many times.</li>
     * <li><em>DYNAMIC</em> - The data store contents will be modified repeatedly and used many times.</li>
     * </ul>
     *
     * <p>The nature of access may be one of these:</p>
     *
     * <ul>
     * <li><em>DRAW</em> - The data store contents are modified by the application, and used as the source for GL drawing and image specification commands.</li>
     * <li><em>READ</em> - The data store contents are modified by reading data from the GL, and used to return that data when queried by the application.</li>
     * <li><em>COPY</em> - The data store contents are modified by reading data from the GL, and used as the source for GL drawing and image specification commands.</li>
     * </ul>
     *
     * @param target the target buffer object.
     * @param data   a pointer to data that will be copied into the data store for initialization, or {@code NULL} if no data is to be copied.
     * @param usage  the expected usage pattern of the data store.
     */
    public void glBufferData(int target, IntBuffer data, int usage);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glBufferSubData">Reference Page</a></p>
     * <p>
     * Updates a subset of a buffer object's data store.
     *
     * @param target the target buffer object.
     * @param offset the offset into the buffer object's data store where data replacement will begin, measured in bytes.
     * @param data   a pointer to the new data that will be copied into the data store.
     */
    public void glBufferSubData(int target, long offset, IntBuffer data);

    /**
     * Causes the client to block and wait for a sync object to become signaled. If {@code sync} is signaled when {@code glClientWaitSync} is called,
     * {@code glClientWaitSync} returns immediately, otherwise it will block and wait for up to timeout nanoseconds for {@code sync} to become signaled.
     *
     * @param sync    the sync object whose status to wait on.
     * @param flags   a bitfield controlling the command flushing behavior.
     * @param timeout the timeout, specified in nanoseconds, for which the implementation should wait for {@code sync} to become signaled.
     * @return the status is one of ALREADY_SIGNALED, TIMEOUT_EXPIRED, CONDITION_SATISFIED or WAIT_FAILED.
     */
    public int glClientWaitSync(Object sync, int flags, long timeout);

    /**
     * Deletes a sync object.
     *
     * @param sync the sync object to be deleted.
     */
    public void glDeleteSync(Object sync);

    /**
     * Draw multiple instances of a range of elements.
     *
     * @param mode      the kind of primitives to render.
     * @param first     the starting index in the enabled arrays.
     * @param count     the number of indices to be rendered.
     * @param primCount the number of instances of the specified range of indices to be rendered.
     */
    public void glDrawArraysInstancedARB(int mode, int first, int count, int primCount);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glDrawBuffers">Reference Page</a></p>
     * <p>
     * Specifies a list of color buffers to be drawn into.
     *
     * @param bufs an array of symbolic constants specifying the buffers into which fragment colors or data values will be written.
     */
    public void glDrawBuffers(IntBuffer bufs);

    /**
     * Draws multiple instances of a set of elements.
     *
     * @param mode                the kind of primitives to render.
     * @param indicesCount        the number of elements to be rendered.
     * @param type                the type of the values in {@code indices}.
     * @param indicesBufferOffset a pointer to the location where the indices are stored.
     * @param primCount           the number of instances of the specified range of indices to be rendered.
     */
    public void glDrawElementsInstancedARB(int mode, int indicesCount, int type, long indicesBufferOffset, int primCount);

    /**
     * Creates a new sync object and inserts it into the GL command stream.
     *
     * @param condition the condition that must be met to set the sync object's state to signaled.
     * @param flags     a bitwise combination of flags controlling the behavior of the sync object. No flags are presently defined for this operation and {@code flags} must be zero.
     * @return          a new instance
     */
    public Object glFenceSync(int condition, int flags);

    /**
     * Retrieves the location of a sample.
     *
     * @param pname the sample parameter name.
     * @param index the index of the sample whose position to query.
     * @param val   an array to receive the position of the sample.
     */
    public void glGetMultisample(int pname, int index, FloatBuffer val);

    /**
     * Establishes the data storage, format, dimensions, and number of samples of a 2D multisample texture's image.
     *
     * @param target               the target of the operation.
     * @param samples              the number of samples in the multisample texture's image
     * @param internalFormat       the internal format to be used to store the multisample texture's image. {@code internalformat} must specify a color-renderable, depth-renderable,
     *                             or stencil-renderable format.
     * @param width                the width of the multisample texture's image, in texels
     * @param height               the height of the multisample texture's image, in texels
     * @param fixedSampleLocations whether the image will use identical sample locations and the same number of samples for all texels in the image, and the sample locations will not
     *                             depend on the internal format or size of the image
     */
    public void glTexImage2DMultisample(int target, int samples, int internalFormat, int width, int height,
                                        boolean fixedSampleLocations);

    /**
     * Modifies the rate at which generic vertex attributes advance when rendering multiple instances of primitives in a single draw call. If {@code divisor}
     * is zero, the attribute at slot {@code index} advances once per vertex. If {@code divisor} is non-zero, the attribute advances once per {@code divisor}
     * instances of the set(s) of vertices being rendered. An attribute is referred to as {@code instanced} if its {@code divisor} value is non-zero.
     *
     * @param index   the attribute index.
     * @param divisor the divisor value.
     */
    public void glVertexAttribDivisorARB(int index, int divisor);

    public default void glPushDebugGroup(int source, int id, String message) {
    }

    public default void glPopDebugGroup() {
    }

    public default void glObjectLabel(int identifier, int id, String label){
    }
}
