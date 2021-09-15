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

/**
 * GL functions only available on vanilla desktop OpenGL 4.0.
 * 
 * @author Kirill Vainer
 */
public interface GL4 extends GL3 {

    public static final int GL_TESS_CONTROL_SHADER = 0x8E88;
    public static final int GL_TESS_EVALUATION_SHADER = 0x8E87;
    public static final int GL_PATCHES = 0xE;

    /**
     * Accepted by the {@code target} parameter of BindBufferBase and BindBufferRange.
     */
    public static final int GL_ATOMIC_COUNTER_BUFFER = 0x92C0;

    /**
     * Accepted by the {@code target} parameters of BindBuffer, BufferData, BufferSubData, MapBuffer, UnmapBuffer, GetBufferSubData, and GetBufferPointerv.
     */
    public static final int GL_SHADER_STORAGE_BUFFER = 0x90D2;
    public static final int GL_SHADER_STORAGE_BLOCK = 0x92E6;

    /**
     *  Accepted by the &lt;pname&gt; parameter of GetIntegerv, GetBooleanv,
     *  GetInteger64v, GetFloatv, and GetDoublev:
     */
    public static final int GL_MAX_VERTEX_SHADER_STORAGE_BLOCKS = 0x90D6;
    public static final int GL_MAX_GEOMETRY_SHADER_STORAGE_BLOCKS = 0x90D7;
    public static final int GL_MAX_TESS_CONTROL_SHADER_STORAGE_BLOCKS = 0x90D8;
    public static final int GL_MAX_TESS_EVALUATION_SHADER_STORAGE_BLOCKS = 0x90D9;
    public static final int GL_MAX_FRAGMENT_SHADER_STORAGE_BLOCKS = 0x90DA;
    public static final int GL_MAX_COMPUTE_SHADER_STORAGE_BLOCKS = 0x90DB;
    public static final int GL_MAX_COMBINED_SHADER_STORAGE_BLOCKS = 0x90DC;
    public static final int GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS = 0x90DD;
    public static final int GL_MAX_SHADER_STORAGE_BLOCK_SIZE = 0x90DE;
    public static final int GL_SHADER_STORAGE_BUFFER_OFFSET_ALIGNMENT = 0x90DF;

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glPatchParameteri">Reference Page</a></p>
     * <p>
     * Specifies the integer value of the specified parameter for patch primitives.
     *
     * @param count the new value for the parameter given by {@code pname}
     */
    public void glPatchParameter(int count);

    /**
     * Returns the unsigned integer index assigned to a resource named name in the interface type programInterface of
     * program object program.
     *
     * @param program          the name of a program object whose resources to query.
     * @param programInterface a token identifying the interface within program containing the resource named name.
     * @param name             the name of the resource to query the index of.
     * @return the index of a named resource within a program.
     */
    public int glGetProgramResourceIndex(int program, int programInterface, String name);

    /**
     * Changes the active shader storage block with an assigned index of storageBlockIndex in program object program.
     * storageBlockIndex must be an active shader storage block index in program. storageBlockBinding must be less
     * than the value of {@code #GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS}. If successful, glShaderStorageBlockBinding specifies
     * that program will use the data store of the buffer object bound to the binding point storageBlockBinding to
     * read and write the values of the buffer variables in the shader storage block identified by storageBlockIndex.
     *
     * @param program             the name of a program object whose resources to query.
     * @param storageBlockIndex   The index storage block within the program.
     * @param storageBlockBinding The index storage block binding to associate with the specified storage block.
     */
    public void glShaderStorageBlockBinding(int program, int storageBlockIndex, int storageBlockBinding);
}
