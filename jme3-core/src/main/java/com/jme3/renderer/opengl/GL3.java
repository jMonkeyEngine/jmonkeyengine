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

import java.nio.IntBuffer;

/**
 * GL functions only available on vanilla desktop OpenGL 3.0+.
 *
 * @author Kirill Vainer
 */
public interface GL3 extends GL2 {

    public static final int GL_DEPTH_STENCIL_ATTACHMENT = 0x821A;
    public static final int GL_GEOMETRY_SHADER = 0x8DD9;
    public static final int GL_NUM_EXTENSIONS = 0x821D;
    public static final int GL_R8 = 0x8229;
    public static final int GL_R16F = 0x822D;
    public static final int GL_R32F = 0x822E;
    public static final int GL_RG16F = 0x822F;
    public static final int GL_RG32F = 0x8230;
    public static final int GL_RG = 0x8227;
    public static final int GL_RG8 = 0x822B;
    public static final int GL_TEXTURE_SWIZZLE_A = 0x8E45;
    public static final int GL_TEXTURE_SWIZZLE_B = 0x8E44;
    public static final int GL_TEXTURE_SWIZZLE_G = 0x8E43;
    public static final int GL_TEXTURE_SWIZZLE_R = 0x8E42;
    public static final int GL_COMPRESSED_RED_RGTC1 = 0x8DBB;
    public static final int GL_COMPRESSED_SIGNED_RED_RGTC1 = 0x8DBC;
    public static final int GL_COMPRESSED_RG_RGTC2 = 0x8DBD;
    public static final int GL_COMPRESSED_SIGNED_RG_RGTC2 = 0x8DBE;
    public static final int GL_R8I = 33329;
    public static final int GL_R8UI = 33330;
    public static final int GL_R16I = 33331;
    public static final int GL_R16UI = 33332;
    public static final int GL_R32I = 33333;
    public static final int GL_R32UI = 33334;
    public static final int GL_RG8I = 33335;
    public static final int GL_RG8UI = 33336;
    public static final int GL_RG16I = 33337;
    public static final int GL_RG16UI = 33338;
    public static final int GL_RG32I = 33339;
    public static final int GL_RG32UI = 33340;
    public static final int GL_RGBA32UI = 36208;
    public static final int GL_RGB32UI = 36209;
    public static final int GL_RGBA16UI = 36214;
    public static final int GL_RGB16UI = 36215;
    public static final int GL_RGBA8UI = 36220;
    public static final int GL_RGB8UI = 36221;
    public static final int GL_RGBA32I = 36226;
    public static final int GL_RGB32I = 36227;
    public static final int GL_RGBA16I = 36232;
    public static final int GL_RGB16I = 36233;
    public static final int GL_RGBA8I = 36238;
    public static final int GL_RGB8I = 36239;
    public static final int GL_RED_INTEGER = 36244;
    public static final int GL_RG_INTEGER = 33320;
    public static final int GL_RGB_INTEGER = 36248;
    public static final int GL_RGBA_INTEGER = 36249;

    public static final int GL_UNIFORM_OFFSET = 0x8A3B;

    /**
     * Accepted by the {@code target} parameters of BindBuffer, BufferData, BufferSubData, MapBuffer, UnmapBuffer, GetBufferSubData, and GetBufferPointerv.
     */
    public static final int GL_UNIFORM_BUFFER = 0x8A11;

    /**
     * Accepted by the {@code pname} parameter of GetActiveUniformBlockiv.
     */
    public static final int GL_UNIFORM_BLOCK_BINDING = 0x8A3F;
    public static final int GL_UNIFORM_BLOCK_DATA_SIZE = 0x8A40;
    public static final int GL_UNIFORM_BLOCK_NAME_LENGTH = 0x8A41;
    public static final int GL_UNIFORM_BLOCK_ACTIVE_UNIFORMS = 0x8A42;
    public static final int GL_UNIFORM_BLOCK_ACTIVE_UNIFORM_INDICES = 0x8A43;
    public static final int GL_UNIFORM_BLOCK_REFERENCED_BY_VERTEX_SHADER = 0x8A44;
    public static final int GL_UNIFORM_BLOCK_REFERENCED_BY_GEOMETRY_SHADER = 0x8A45;
    public static final int GL_UNIFORM_BLOCK_REFERENCED_BY_FRAGMENT_SHADER = 0x8A46;

    /**
     *  Accepted by the &lt;pname&gt; parameter of GetBooleanv, GetIntegerv,
     *  GetFloatv, and GetDoublev:
     */
    public static final int GL_MAX_VERTEX_UNIFORM_BLOCKS = 0x8A2B;
    public static final int GL_MAX_GEOMETRY_UNIFORM_BLOCKS = 0x8A2C;
    public static final int GL_MAX_FRAGMENT_UNIFORM_BLOCKS = 0x8A2D;
    public static final int GL_MAX_COMBINED_UNIFORM_BLOCKS = 0x8A2E;
    public static final int GL_MAX_UNIFORM_BUFFER_BINDINGS = 0x8A2F;
    public static final int GL_MAX_UNIFORM_BLOCK_SIZE = 0x8A30;
    public static final int GL_MAX_COMBINED_VERTEX_UNIFORM_COMPONENTS = 0x8A31;
    public static final int GL_MAX_COMBINED_GEOMETRY_UNIFORM_COMPONENTS = 0x8A32;
    public static final int GL_MAX_COMBINED_FRAGMENT_UNIFORM_COMPONENTS = 0x8A33;
    public static final int GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT = 0x8A34;

    /**
     * Accepted by the {@code target} parameters of BindBuffer, BufferData, BufferSubData, MapBuffer, UnmapBuffer, GetBufferSubData, GetBufferPointerv,
     * BindBufferRange, BindBufferOffset and BindBufferBase.
     */
    public static final int GL_TRANSFORM_FEEDBACK_BUFFER = 0x8C8E;


    public static final int GL_FRAMEBUFFER = 0x8D40;
    public static final int GL_READ_FRAMEBUFFER = 0x8CA8;
    public static final int GL_DRAW_FRAMEBUFFER = 0x8CA9;

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glBindFragDataLocation">Reference Page</a></p>
     * <p>
     * Binds a user-defined varying out variable to a fragment shader color number.
     *
     * @param program     the name of the program containing varying out variable whose binding to modify.
     * @param colorNumber the color number to bind the user-defined varying out variable to.
     * @param name        the name of the user-defined varying out variable whose binding to modify.
     */
    public void glBindFragDataLocation(int program, int colorNumber, String name); /// GL3+

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glBindVertexArray">Reference Page</a></p>
     * <p>
     * Binds a vertex array object
     *
     * @param array the name of the vertex array to bind.
     */
    public void glBindVertexArray(int array); /// GL3+

    /**
     * Deletes vertex array objects.
     *
     * @param arrays an array containing the n names of the objects to be deleted.
     */
    public void glDeleteVertexArrays(IntBuffer arrays); /// GL3+

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glGenVertexArrays">Reference Page</a></p>
     *
     * Generates vertex array object names.
     *
     * @param arrays a buffer in which the generated vertex array object names are stored.
     */
    public void glGenVertexArrays(IntBuffer arrays); /// GL3+

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glGetStringi">Reference Page</a></p>
     *
     * Queries indexed string state.
     *
     * @param name  the indexed state to query. One of:
     *  {@link GL#GL_EXTENSIONS EXTENSIONS}
     *  {@link GL2#GL_SHADING_LANGUAGE_VERSION SHADING_LANGUAGE_VERSION}
     * @param index the index of the particular element being queried.
     * @return the value of the string state
     */
    public String glGetString(int name, int index); /// GL3+


    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glGetUniformBlockIndex">Reference Page</a></p>
     *
     * Retrieves the index of a named uniform block.
     *
     * @param program          the name of a program containing the uniform block.
     * @param uniformBlockName an array of characters to containing the name of the uniform block whose index to retrieve.
     * @return the block index.
     */
    public int glGetUniformBlockIndex(int program, String uniformBlockName);

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glBindBufferBase">Reference Page</a></p>
     *
     * Binds a buffer object to an indexed buffer target.
     *
     * @param target the target of the bind operation. One of:
     *  {@link #GL_TRANSFORM_FEEDBACK_BUFFER TRANSFORM_FEEDBACK_BUFFER}
     *  {@link #GL_UNIFORM_BUFFER UNIFORM_BUFFER}
     *  {@link GL4#GL_ATOMIC_COUNTER_BUFFER ATOMIC_COUNTER_BUFFER}
     *  {@link GL4#GL_SHADER_STORAGE_BUFFER SHADER_STORAGE_BUFFER}
     * @param index  the index of the binding point within the array specified by {@code target}
     * @param buffer a buffer object to bind to the specified binding point
     */
    public void glBindBufferBase(int target, int index, int buffer);

    /**
     * Binding points for active uniform blocks are assigned using glUniformBlockBinding. Each of a program's active
     * uniform blocks has a corresponding uniform buffer binding point. program is the name of a program object for
     * which the command glLinkProgram has been issued in the past.
     * <p>
     * If successful, glUniformBlockBinding specifies that program will use the data store of the buffer object bound
     * to the binding point uniformBlockBinding to extract the values of the uniforms in the uniform block identified
     * by uniformBlockIndex.
     * <p>
     * When a program object is linked or re-linked, the uniform buffer object binding point assigned to each of its
     * active uniform blocks is reset to zero.
     *
     * @param program             The name of a program object containing the active uniform block whose binding to
     *                            assign.
     * @param uniformBlockIndex   The index of the active uniform block within program whose binding to assign.
     * @param uniformBlockBinding Specifies the binding point to which to bind the uniform block with index
     *                            uniformBlockIndex within program.
     */
    public void glUniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding);
}
