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

import com.jme3.math.*;
import com.jme3.util.BufferUtils;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class Uniform extends ShaderVariable {

    private static final Integer ZERO_INT = 0;
    private static final Float ZERO_FLT = Float.valueOf(0);
    private static final FloatBuffer ZERO_BUF = BufferUtils.createFloatBuffer(4*4);

    /**
     * Currently set value of the uniform.
     */
    protected Object value = null;
    
    /**
     * For arrays or matrices, efficient format
     * that can be sent to GL faster.
     */
    protected FloatBuffer multiData = null;

    /**
     * Type of uniform
     */
    protected VarType varType;

    /**
     * Binding to a renderer value, or null if user-defined uniform
     */
    protected UniformBinding binding;

    /**
     * Used to track which uniforms to clear to avoid
     * values leaking from other materials that use that shader.
     */
    protected boolean setByCurrentMaterial = false;

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 31 * hash + (this.value != null ? this.value.hashCode() : 0);
        hash = 31 * hash + (this.varType != null ? this.varType.hashCode() : 0);
        hash = 31 * hash + (this.binding != null ? this.binding.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        final Uniform other = (Uniform) obj;
        if (this.value != other.value && (this.value == null || !this.value.equals(other.value))) {
            return false;
        }
        return this.binding == other.binding && this.varType == other.varType;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("Uniform[name=");
        sb.append(name);
        if (varType != null){
            sb.append(", type=");
            sb.append(varType);
            sb.append(", value=");
            sb.append(value);
        }else{
            sb.append(", value=<not set>");
        }
        sb.append("]");
        return sb.toString();
    }

    public void setBinding(UniformBinding binding){
        this.binding = binding;
    }

    public UniformBinding getBinding(){
        return binding;
    }

    public VarType getVarType() {
        return varType;
    }

    public Object getValue(){
        return value;
    }
    
    public FloatBuffer getMultiData() {
        return multiData;
    }

    public boolean isSetByCurrentMaterial() {
        return setByCurrentMaterial;
    }

    public void clearSetByCurrentMaterial(){
        setByCurrentMaterial = false;
    }

    public void clearValue(){
        updateNeeded = true;

        if (multiData != null){           
            multiData.clear();

            while (multiData.remaining() > 0){
                ZERO_BUF.clear();
                ZERO_BUF.limit( Math.min(multiData.remaining(), 16) );
                multiData.put(ZERO_BUF);
            }

            multiData.clear();

            return;
        }

        if (varType == null) {
            return;
        }
            
        switch (varType){
            case Int:
                this.value = ZERO_INT;
                break;
            case Boolean:
                this.value = Boolean.FALSE;
                break;
            case Float:
                this.value = ZERO_FLT; 
                break;
            case Vector2:
                if (this.value != null) {
                    ((Vector2f) this.value).set(Vector2f.ZERO);
                }
                break;
            case Vector3:
                if (this.value != null) {
                    ((Vector3f) this.value).set(Vector3f.ZERO);
                }
                break;
            case Vector4:
                if (this.value != null) {
                    if (this.value instanceof ColorRGBA) {
                        ((ColorRGBA) this.value).set(ColorRGBA.BlackNoAlpha);
                    } else if (this.value instanceof Vector4f) {
                        ((Vector4f) this.value).set(Vector4f.ZERO);
                    } else {
                        ((Quaternion) this.value).set(Quaternion.ZERO);
                    }
                }
                break;
            default:
                // won't happen because those are either textures
                // or multidata types
        }
    }
    
    public void setValue(VarType type, Object value){
        if (location == LOC_NOT_DEFINED) {
            return;
        }

        if (varType != null && varType != type) {
            throw new IllegalArgumentException("Expected a " + varType.name() + " value!");
        }

        if (value == null) {
            throw new IllegalArgumentException("for uniform " + name + ": value cannot be null");
        }

        setByCurrentMaterial = true;

        switch (type){
            case Matrix3:
                if (value.equals(this.value)) {
                    return;
                }
                Matrix3f m3 = (Matrix3f) value;
                if (multiData == null) {
                    multiData = BufferUtils.createFloatBuffer(9);
                }
                m3.fillFloatBuffer(multiData, true);
                multiData.clear();
                if (this.value == null) {
                    this.value = new Matrix3f(m3);
                } else {
                    ((Matrix3f)this.value).set(m3);
                }
                break;
            case Matrix4:
                if (value.equals(this.value)) {
                    return;
                }
                Matrix4f m4 = (Matrix4f) value;
                if (multiData == null) {
                    multiData = BufferUtils.createFloatBuffer(16);
                }
                m4.fillFloatBuffer(multiData, true);
                multiData.clear();
                if (this.value == null) {
                    this.value = new Matrix4f(m4);
                } else {
                    ((Matrix4f)this.value).copy(m4);
                }
                break;
            case IntArray:
                int[] ia = (int[]) value;
                if (this.value == null) {
                    this.value = BufferUtils.createIntBuffer(ia);
                } else {
                    this.value = BufferUtils.ensureLargeEnough((IntBuffer)this.value, ia.length);
                }
                ((IntBuffer)this.value).clear();
                break;
            case FloatArray:
                float[] fa = (float[]) value;
                if (multiData == null) {
                    multiData = BufferUtils.createFloatBuffer(fa);
                } else {
                    multiData = BufferUtils.ensureLargeEnough(multiData, fa.length);
                }
                multiData.put(fa);
                multiData.clear();
                break;
            case Vector2Array:
                Vector2f[] v2a = (Vector2f[]) value;
                if (multiData == null) {
                    multiData = BufferUtils.createFloatBuffer(v2a);
                } else {
                    multiData = BufferUtils.ensureLargeEnough(multiData, v2a.length * 2);
                }
                for (int i = 0; i < v2a.length; i++) {
                    BufferUtils.setInBuffer(v2a[i], multiData, i);
                }
                multiData.clear();
                break;
            case Vector3Array:
                Vector3f[] v3a = (Vector3f[]) value;
                if (multiData == null) {
                    multiData = BufferUtils.createFloatBuffer(v3a);
                } else {
                    multiData = BufferUtils.ensureLargeEnough(multiData, v3a.length * 3);
                }
                for (int i = 0; i < v3a.length; i++) {
                    BufferUtils.setInBuffer(v3a[i], multiData, i);
                }
                multiData.clear();
                break;
            case Vector4Array:
                Vector4f[] v4a = (Vector4f[]) value;
                if (multiData == null) {
                    multiData = BufferUtils.createFloatBuffer(v4a);
                } else {
                    multiData = BufferUtils.ensureLargeEnough(multiData, v4a.length * 4);
                }
                for (int i = 0; i < v4a.length; i++) {
                    BufferUtils.setInBuffer(v4a[i], multiData, i);
                }
                multiData.clear();
                break;
            case Matrix3Array:
                Matrix3f[] m3a = (Matrix3f[]) value;
                if (multiData == null) {
                    multiData = BufferUtils.createFloatBuffer(m3a.length * 9);
                } else {
                    multiData = BufferUtils.ensureLargeEnough(multiData, m3a.length * 9);
                }
                for (int i = 0; i < m3a.length; i++) {
                    m3a[i].fillFloatBuffer(multiData, true);
                }
                multiData.clear();
                break;
            case Matrix4Array:
                Matrix4f[] m4a = (Matrix4f[]) value;
                if (multiData == null) {
                    multiData = BufferUtils.createFloatBuffer(m4a.length * 16);
                } else {
                    multiData = BufferUtils.ensureLargeEnough(multiData, m4a.length * 16);
                }
                for (int i = 0; i < m4a.length; i++) {
                    m4a[i].fillFloatBuffer(multiData, true);
                }
                multiData.clear();
                break;
            case Vector2:
                if (value.equals(this.value)) {
                    return;
                }
                if (this.value == null) {
                    this.value = new Vector2f((Vector2f) value);
                } else {
                    ((Vector2f) this.value).set((Vector2f) value);
                }
                break;
            case Vector3:
                if (value.equals(this.value)) {
                    return;
                }
                if (this.value == null) {
                    this.value = new Vector3f((Vector3f) value);
                } else {
                    ((Vector3f) this.value).set((Vector3f) value);
                }
                break;
            case Vector4:
                if (value.equals(this.value)) {
                    return;
                }
                if (value instanceof ColorRGBA) {
                    if (this.value == null) {
                        this.value = new ColorRGBA();
                    }
                    ((ColorRGBA) this.value).set((ColorRGBA) value);
                } else if (value instanceof Vector4f) {
                    if (this.value == null) {
                        this.value = new Vector4f();
                    }
                    ((Vector4f) this.value).set((Vector4f) value);
                } else {
                    if (this.value == null) {
                        this.value = new Quaternion();
                    }
                    ((Quaternion) this.value).set((Quaternion) value);
                }
                break;
                // Only use check if equals optimization for primitive values
            case Int:
            case Float:
            case Boolean:
                if (value.equals(this.value)) {
                    return;
                }
                this.value = value;
                break;
            default:
                this.value = value;
                break;
        }

//        if (multiData != null) {
//            this.value = multiData;
//        }

        varType = type;
        updateNeeded = true;
    }

    public void setVector4Length(int length){
        if (location == -1) {
            return;
        }
        
        multiData = BufferUtils.ensureLargeEnough(multiData, length * 4);
        value = multiData;
        varType = VarType.Vector4Array;
        updateNeeded = true;
        setByCurrentMaterial = true;
    }

    public void setVector4InArray(float x, float y, float z, float w, int index){
        if (location == -1) {
            return;
        }

        if (varType != null && varType != VarType.Vector4Array) {
            throw new IllegalArgumentException("Expected a " + varType.name() + " value!");
        }

        multiData.position(index * 4);
        multiData.put(x).put(y).put(z).put(w);
        multiData.rewind();
        updateNeeded = true;
        setByCurrentMaterial = true;
    }
    
    public boolean isUpdateNeeded(){
        return updateNeeded;
    }

    public void clearUpdateNeeded(){
        updateNeeded = false;
    }

    public void reset(){
        setByCurrentMaterial = false;
        location = -2;
        updateNeeded = true;
    }

    public void deleteNativeBuffers() {
        if (value instanceof Buffer) {
            BufferUtils.destroyDirectBuffer((Buffer)value);
            value = null; // ????
        }
    }
}
