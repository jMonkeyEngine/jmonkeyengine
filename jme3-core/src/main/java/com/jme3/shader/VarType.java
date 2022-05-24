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
package com.jme3.shader;
import com.jme3.math.*;
import com.jme3.texture.*;
import com.jme3.shader.BufferObject;
public enum VarType {

    Float("float",float.class,Float.class),
    Vector2("vec2",Vector2f.class),
    Vector3("vec3",Vector3f.class),
    Vector4("vec4",Vector4f.class, ColorRGBA.class),

    IntArray(true,false,"int",int[].class,Integer[].class),
    FloatArray(true,false,"float",float[].class,Float[].class),
    Vector2Array(true,false,"vec2",Vector2f[].class),
    Vector3Array(true,false,"vec3",Vector3f[].class),
    Vector4Array(true,false,"vec4",Vector4f[].class),

    Boolean("bool",Boolean.class,boolean.class),

    Matrix3(true,false,"mat3",Matrix3f.class),
    Matrix4(true,false,"mat4",Matrix4f.class),

    Matrix3Array(true,false,"mat3",Matrix3f[].class),
    Matrix4Array(true,false,"mat4",Matrix4f[].class),
    
    TextureBuffer(false,true,"sampler1D|sampler1DShadow"), 
    Texture2D(false,true,"sampler2D|sampler2DShadow",Texture2D.class,Texture.class),
    Texture3D(false,true,"sampler3D",Texture3D.class,Texture.class),
    TextureArray(false,true,"sampler2DArray|sampler2DArrayShadow",TextureArray.class,Texture.class),
    TextureCubeMap(false,true,"samplerCube",TextureCubeMap.class,Texture.class),
    Int("int",int.class,Integer.class),
    BufferObject(false, false, "custom", BufferObject.class);


    private boolean usesMultiData = false;
    private boolean textureType = false;
    final private String glslType;
    private Class<?> javaTypes[];
    
    VarType(String glslType,Class<?> ...javaTypes){
        this.glslType = glslType;
        if (javaTypes != null) {
            this.javaTypes = javaTypes;
        } else {
            this.javaTypes = new Class<?>[0];
        }
        
    }


    VarType(boolean multiData, boolean textureType, String glslType, Class<?>... javaTypes) {
        usesMultiData = multiData;
        this.textureType = textureType;
        this.glslType = glslType;
        if (javaTypes != null) {
            this.javaTypes = javaTypes;
        } else {
            this.javaTypes = new Class<?>[0];
        }
    }
    
    /**
     * Check if the passed object is of a type mapped to this VarType
     * @param o Object to check
     * @return true if the object type is mapped to this VarType
     */
    public boolean isOfType(Object o){
        for(Class<?> c : javaTypes){
            if(c.isAssignableFrom(o.getClass()))return true;
        }
        return false;
    }
    

    /**
     * Get the java types mapped to this VarType
     * @return an array of classes mapped to this VarType
     */
    public Class<?>[] getJavaType(){
        return javaTypes;
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
