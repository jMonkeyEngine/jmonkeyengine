/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
package com.jme3.shader;

/**
 * Represents a generic variable within a shader program.
 * <p>
 * This abstract class provides the fundamental properties shared by different
 * types of shader variables, such as uniforms or attributes.
 */
public class ShaderVariable {

    public static final int LOC_UNKNOWN = -2;
    public static final int LOC_NOT_DEFINED = -1;

    /**
     * <ul>
     * <li>If {@code LOC_UNKNOWN} (-2): The location is currently unknown.</li>
     * <li>If {@code LOC_NOT_DEFINED} (-1): The variable is not defined in the shader.</li>
     * <li>If {@code >= 0}: The uniform variable is defined and available.</li>
     * </ul>
     */
    protected int location = LOC_UNKNOWN;
    /**
     * Name of the uniform as was declared in the shader.
     * E.g. name = "g_WorldMatrix" if the declaration was
     * "uniform mat4 g_WorldMatrix;".
     */
    protected String name = null;
    /**
     * True if the shader value was changed.
     */
    protected boolean updateNeeded = true;

    /**
     * Sets the location of this shader variable.
     *
     * @param location The integer location of the variable in the shader.
     */
    public void setLocation(int location) {
        this.location = location;
    }

    /**
     * Returns the location of this shader variable.
     *
     * @return The integer location of the variable.
     */
    public int getLocation() {
        return location;
    }

    /**
     * Sets the name of this shader variable as it's defined in the shader code.
     *
     * @param name The string name of the variable.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the name of this shader variable.
     *
     * @return The string name of the variable.
     */
    public String getName() {
        return name;
    }

}
