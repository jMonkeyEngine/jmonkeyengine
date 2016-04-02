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
package com.jme3.shader;

public enum VarType {

    Float("float"),
    Vector2("vec2"),
    Vector3("vec3"),
    Vector4("vec4"),

    IntArray(true,false,"int"),
    FloatArray(true,false,"float"),
    Vector2Array(true,false,"vec2"),
    Vector3Array(true,false,"vec3"),
    Vector4Array(true,false,"vec4"),

    Boolean("bool"),

    Matrix3(true,false,"mat3"),
    Matrix4(true,false,"mat4"),

    Matrix3Array(true,false,"mat3"),
    Matrix4Array(true,false,"mat4"),
    
    TextureBuffer(false,true,"sampler1D|sampler1DShadow"),
    Texture2D(false,true,"sampler2D|sampler2DShadow"),
    Texture3D(false,true,"sampler3D"),
    TextureArray(false,true,"sampler2DArray|sampler2DArrayShadow"),
    TextureCubeMap(false,true,"samplerCube"),
    Int("int");

    private boolean usesMultiData = false;
    private boolean textureType = false;
    private String glslType;

    
    VarType(String glslType){
        this.glslType = glslType;
    }

    VarType(boolean multiData, boolean textureType,String glslType){
        usesMultiData = multiData;
        this.textureType = textureType;
        this.glslType = glslType;
    }

    public boolean isTextureType() {
        return textureType;
    }

    public boolean usesMultiData() {
        return usesMultiData;
    }

    public String getGlslType() {
        return glslType;
    }    

}
