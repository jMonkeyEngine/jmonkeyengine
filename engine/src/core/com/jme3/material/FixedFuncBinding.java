/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

package com.jme3.material;

/**
 * Fixed function binding is used to specify a binding for a {@link MatParam}
 * in case that shaders are not supported on the system.
 * 
 * @author Kirill Vainer
 */
public enum FixedFuncBinding {
    /**
     * Specifies the material ambient color.
     * Same as GL_AMBIENT for OpenGL.
     */
    MaterialAmbient,
    
    /**
     * Specifies the material diffuse color.
     * Same as GL_DIFFUSE for OpenGL.
     */
    MaterialDiffuse,
    
    /**
     * Specifies the material specular color.
     * Same as GL_SPECULAR for OpenGL
     */
    MaterialSpecular,
    
    /**
     * Specifies the color of the object.
     * <p>
     * Used only for non-lit materials.
     */
    Color,
    
    /**
     * Specifies the material shininess value.
     * 
     * Same as GL_SHININESS for OpenGL.
     */
    MaterialShininess,
    
    /**
     * Use vertex color as an additional diffuse color, if lighting is enabled.
     * If lighting is disabled, vertex color is modulated with
     * {@link #Color material color}.
     */
    UseVertexColor
}
