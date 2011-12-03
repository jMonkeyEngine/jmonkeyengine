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

package com.jme3.shader;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.*;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.nio.FloatBuffer;

public class Uniform extends ShaderVariable {

    private static final Integer ZERO_INT = Integer.valueOf(0);
    private static final Float ZERO_FLT = Float.valueOf(0);
    private static final FloatBuffer ZERO_BUF = BufferUtils.createFloatBuffer(4*4);

    /**
     * Currently set value of the uniform.
     */
    protected Object value = null;
    protected FloatBuffer multiData = null;

    /**
     * Type of uniform
     */
    protected VarType varType;

    /**
     * Binding to a renderer value, or null if user-defined uniform
     */
    protected UniformBinding binding;

    protected boolean setByCurrentMaterial = false;
//    protected Object lastChanger = null;

    @Override
    public void write(JmeExporter ex) throws IOException{
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(varType, "varType", null);
        oc.write(binding, "binding", null);
        switch (varType){
            case Boolean:
                oc.write( ((Boolean)value).booleanValue(), "valueBoolean", false );
                break;
            case Float:
                oc.write( ((Float)value).floatValue(), "valueFloat", 0);
                break;
            case FloatArray:
                oc.write( (FloatBuffer)value, "valueFloatArray", null);
                break;
            case Int:
                oc.write( ((Integer)value).intValue(), "valueInt", 0);
                break;
            case Matrix3:
                oc.write( (Matrix3f)value, "valueMatrix3", null);
                break;
            case Matrix3Array:
            case Matrix4Array:
            case Vector2Array:
                throw new UnsupportedOperationException("Come again?");
            case Matrix4:
                oc.write( (Matrix4f)value, "valueMatrix4", null);
                break;
            case Vector2:
                oc.write( (Vector2f)value, "valueVector2", null);
                break;
            case Vector3:
                oc.write( (Vector3f)value, "valueVector3", null);
                break;
            case Vector3Array:
                oc.write( (FloatBuffer)value, "valueVector3Array", null);
                break;
            case Vector4:
                oc.write( (ColorRGBA)value, "valueVector4", null);
                break;
            case Vector4Array:
                oc.write( (FloatBuffer)value, "valueVector4Array", null);
                break;
        }
    }

    @Override
    public void read(JmeImporter im) throws IOException{
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        varType = ic.readEnum("varType", VarType.class, null);
        binding = ic.readEnum("binding", UniformBinding.class, null);
        switch (varType){
            case Boolean:
                value = ic.readBoolean("valueBoolean", false);
                break;
            case Float:
                value = ic.readFloat("valueFloat", 0);
                break;
            case FloatArray:
                value = ic.readFloatBuffer("valueFloatArray", null);
                break;
            case Int:
                value = ic.readInt("valueInt", 0);
                break;
            case Matrix3:
                multiData = ic.readFloatBuffer("valueMatrix3", null);
                value = multiData;
                break;
            case Matrix4:
                multiData = ic.readFloatBuffer("valueMatrix4", null);
                value = multiData;
                break;
            case Vector2:
                value = ic.readSavable("valueVector2", null);
                break;
            case Vector3:
                value = ic.readSavable("valueVector3", null);
                break;
            case Vector3Array:
                value = ic.readFloatBuffer("valueVector3Array", null);
                break;
            case Vector4:
                value = ic.readSavable("valueVector4", null);
                break;
            case Vector4Array:
                value = ic.readFloatBuffer("valueVector4Array", null);
                break;
        }
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        if (name != null){
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

    public boolean isSetByCurrentMaterial() {
        return setByCurrentMaterial;
    }

    public void clearSetByCurrentMaterial(){
        setByCurrentMaterial = false;
    }

//    public void setLastChanger(Object lastChanger){
//        this.lastChanger = lastChanger;
//    }
//
//    public Object getLastChanger(){
//        return lastChanger;
//    }

    public void clearValue(){
        updateNeeded = true;

        if (multiData != null){
            ZERO_BUF.clear();
            multiData.clear();

            while (multiData.remaining() > 0){
                ZERO_BUF.limit( Math.min(multiData.remaining(), 16) );
                multiData.put(ZERO_BUF);
            }

            multiData.clear();

            return;
        }

        if (varType == null)
            return;

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
                this.value = Vector2f.ZERO;
                break;
            case Vector3:
                this.value = Vector3f.ZERO;
                break;
            case Vector4:
                if (this.value instanceof ColorRGBA){
                    this.value = ColorRGBA.BlackNoAlpha;
                }else{
                    this.value = Quaternion.ZERO;
                }
                break;
            default:
                break; // won't happen because those are either textures
                       // or multidata types
        }
    }

    public void setValue(VarType type, Object value){
        if (location == -1)
            return;

        if (varType != null && varType != type)
            throw new IllegalArgumentException("Expected a "+varType.name()+" value!");

        if (value == null)
            throw new NullPointerException();

        setByCurrentMaterial = true;

        switch (type){
            case Matrix3:
                Matrix3f m3 = (Matrix3f) value;
                if (multiData == null)
                    multiData = BufferUtils.createFloatBuffer(9);
                
                m3.fillFloatBuffer(multiData, true);
                multiData.clear();
                break;
            case Matrix4:
                Matrix4f m4 = (Matrix4f) value;
                if (multiData == null)
                    multiData = BufferUtils.createFloatBuffer(16);
                
                m4.fillFloatBuffer(multiData, true);
                multiData.clear();
                break;
            case FloatArray:
                float[] fa = (float[]) value;
                if (multiData == null){
                    multiData = BufferUtils.createFloatBuffer(fa);
                }else{
                    multiData = BufferUtils.ensureLargeEnough(multiData, fa.length);
                }
                
                multiData.put(fa);
                multiData.clear();
                break;
            case Vector2Array:
                Vector2f[] v2a = (Vector2f[]) value;
                if (multiData == null){
                    multiData = BufferUtils.createFloatBuffer(v2a);
                } else {
                    multiData = BufferUtils.ensureLargeEnough(multiData, v2a.length * 2);
                }

                for (int i = 0; i < v2a.length; i++)
                    BufferUtils.setInBuffer(v2a[i], multiData, i);
                
                multiData.clear();
                break;
            case Vector3Array:
                Vector3f[] v3a = (Vector3f[]) value;
                if (multiData == null){
                    multiData = BufferUtils.createFloatBuffer(v3a);
                } else{
                    multiData = BufferUtils.ensureLargeEnough(multiData, v3a.length * 3);
                }
                
                for (int i = 0; i < v3a.length; i++)
                    BufferUtils.setInBuffer(v3a[i], multiData, i);

                multiData.clear();
                break;
            case Vector4Array:
                Quaternion[] v4a = (Quaternion[]) value;
                if (multiData == null){
                    multiData = BufferUtils.createFloatBuffer(v4a);
                } else {
                    multiData = BufferUtils.ensureLargeEnough(multiData, v4a.length * 4);
                }
                
                for (int i = 0; i < v4a.length; i++)
                    BufferUtils.setInBuffer(v4a[i], multiData, i);

                multiData.clear();
                break;
            case Matrix3Array:
                Matrix3f[] m3a = (Matrix3f[]) value;

                if (multiData == null)
                    multiData = BufferUtils.createFloatBuffer(m3a.length * 9);
                else{
                    multiData = BufferUtils.ensureLargeEnough(multiData, m3a.length * 9);
                }

                for (int i = 0; i < m3a.length; i++)
                    m3a[i].fillFloatBuffer(multiData, true);
                
                multiData.clear();
                break;
            case Matrix4Array:
                Matrix4f[] m4a = (Matrix4f[]) value;

                if (multiData == null)
                    multiData = BufferUtils.createFloatBuffer(m4a.length * 16);
                else{
                    multiData = BufferUtils.ensureLargeEnough(multiData, m4a.length * 16);
                }

                for (int i = 0; i < m4a.length; i++)
                    m4a[i].fillFloatBuffer(multiData, true);
                
                multiData.clear();
                break;
            // Only use check if equals optimization for primitive values
            case Int:
            case Float:
            case Boolean:
                if (this.value != null && this.value.equals(value))
                    return;

                this.value = value;
                break;
            default:
                this.value = value;
                break;
        }

        if (multiData != null)
            this.value = multiData;
        
        varType = type;
        updateNeeded = true;
    }

    public void setVector4Length(int length){
        if (location == -1)
            return;

        FloatBuffer fb = (FloatBuffer) value;
        if (fb == null || fb.capacity() < length){
            value = BufferUtils.createFloatBuffer(length * 4);
        }

        varType = VarType.Vector4Array;
        updateNeeded = true;
        setByCurrentMaterial = true;
    }

    public void setVector4InArray(float x, float y, float z, float w, int index){
        if (location == -1)
            return;

        if (varType != null && varType != VarType.Vector4Array)
            throw new IllegalArgumentException("Expected a "+varType.name()+" value!");

        FloatBuffer fb = (FloatBuffer) value;
        fb.position(index * 4);
        fb.put(x).put(y).put(z).put(w);
        fb.rewind();
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

}
