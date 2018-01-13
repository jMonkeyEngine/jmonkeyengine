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
 * GL functions only available on vanilla desktop OpenGL 3.0+.
 *
 * @author Kirill Vainer
 */
public interface GL3 extends GL2 {

    static final int GL_DEPTH_STENCIL_ATTACHMENT = 0x821A;
    static final int GL_GEOMETRY_SHADER = 0x8DD9;
    static final int GL_NUM_EXTENSIONS = 0x821D;
    static final int GL_R8 = 0x8229;
    static final int GL_R16F = 0x822D;
    static final int GL_R32F = 0x822E;
    static final int GL_RG16F = 0x822F;
    static final int GL_RG32F = 0x8230;
    static final int GL_RG = 0x8227;
    static final int GL_RG8 = 0x822B;
    static final int GL_TEXTURE_SWIZZLE_A = 0x8E45;
    static final int GL_TEXTURE_SWIZZLE_B = 0x8E44;
    static final int GL_TEXTURE_SWIZZLE_G = 0x8E43;
    static final int GL_TEXTURE_SWIZZLE_R = 0x8E42;
    static final int GL_R8I = 33329;
    static final int GL_R8UI = 33330;
    static final int GL_R16I = 33331;
    static final int GL_R16UI = 33332;
    static final int GL_R32I = 33333;
    static final int GL_R32UI = 33334;
    static final int GL_RG8I = 33335;
    static final int GL_RG8UI = 33336;
    static final int GL_RG16I = 33337;
    static final int GL_RG16UI = 33338;
    static final int GL_RG32I = 33339;
    static final int GL_RG32UI = 33340;
    static final int GL_RGBA32UI = 36208;
    static final int GL_RGB32UI = 36209;
    static final int GL_RGBA16UI = 36214;
    static final int GL_RGB16UI = 36215;
    static final int GL_RGBA8UI = 36220;
    static final int GL_RGB8UI = 36221;
    static final int GL_RGBA32I = 36226;
    static final int GL_RGB32I = 36227;
    static final int GL_RGBA16I = 36232;
    static final int GL_RGB16I = 36233;
    static final int GL_RGBA8I = 36238;
    static final int GL_RGB8I = 36239;
    static final int GL_RED_INTEGER = 36244;
    static final int GL_RG_INTEGER = 33320;
    static final int GL_RGB_INTEGER = 36248;
    static final int GL_RGBA_INTEGER = 36249;

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glBindFragDataLocation">Reference Page</a></p>
     * <p>
     * Binds a user-defined varying out variable to a fragment shader color number.
     *
     * @param program     the name of the program containing varying out variable whose binding to modify.
     * @param colorNumber the color number to bind the user-defined varying out variable to.
     * @param name        the name of the user-defined varying out variable whose binding to modify.
     */
    void glBindFragDataLocation(int program, int colorNumber, String name); /// GL3+

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glBindVertexArray">Reference Page</a></p>
     * <p>
     * Binds a vertex array object
     *
     * @param array the name of the vertex array to bind.
     */
    void glBindVertexArray(int array); /// GL3+

    /**
     * Deletes vertex array objects.
     *
     * @param arrays an array containing the n names of the objects to be deleted.
     */
    void glDeleteVertexArrays(IntBuffer arrays); /// GL3+

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glGenVertexArrays">Reference Page</a></p>
     *
     * Generates vertex array object names.
     *
     * @param arrays a buffer in which the generated vertex array object names are stored.
     */
    void glGenVertexArrays(IntBuffer arrays); /// GL3+

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glGetStringi">Reference Page</a></p>
     * <p>
     * Queries indexed string state.
     *
     * @param name  the indexed state to query. One of:<br><table><tr><td>{@link GL#GL_EXTENSIONS EXTENSIONS}</td><td>{@link GL2#GL_SHADING_LANGUAGE_VERSION SHADING_LANGUAGE_VERSION}</td></tr></table>
     * @param index the index of the particular element being queried.
     */
    String glGetString(int name, int index); /// GL3+
}
