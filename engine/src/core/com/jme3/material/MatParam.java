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

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.renderer.GL1Renderer;
import com.jme3.renderer.Renderer;
import com.jme3.shader.Uniform;
import com.jme3.shader.VarType;
import java.io.IOException;

public class MatParam implements Savable, Cloneable {

    protected VarType type;
    protected String name;
    protected Object value;
    protected FixedFuncBinding ffBinding;
//    protected Uniform uniform;

    public MatParam(VarType type, String name, Object value, FixedFuncBinding ffBinding){
        this.type = type;
        this.name = name;
        this.value = value;
        this.ffBinding = ffBinding;
    }

    public MatParam(){
    }

    public FixedFuncBinding getFixedFuncBinding() {
        return ffBinding;
    }

    public VarType getVarType() {
        return type;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name=name;
    }

    public Object getValue(){
        return value;
    }

    public void setValue(Object value){
        this.value = value;
    }

//    public Uniform getUniform() {
//        return uniform;
//    }
//
//    public void setUniform(Uniform uniform) {
//        this.uniform = uniform;
//    }

    @Override
    public MatParam clone(){
        try{
            MatParam param = (MatParam) super.clone();
            return param;
        }catch (CloneNotSupportedException ex){
            throw new AssertionError();
        }
    }

    public void write(JmeExporter ex) throws IOException{
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(type, "varType", null);
        oc.write(name, "name", null);
        oc.write(ffBinding, "ff_binding", null);
        if (value instanceof Savable){
            Savable s = (Savable) value;
            oc.write(s, "value_savable", null);
        }else if (value instanceof Float){
            Float f = (Float) value;
            oc.write(f.floatValue(), "value_float", 0f);
        }else if (value instanceof Integer){
            Integer i = (Integer) value;
            oc.write(i.intValue(), "value_int", 0);
        }else if (value instanceof Boolean){
            Boolean b = (Boolean) value;
            oc.write(b.booleanValue(), "value_bool", false);
        }
    }

    public void read(JmeImporter im) throws IOException{
        InputCapsule ic = im.getCapsule(this);
        type = ic.readEnum("varType", VarType.class, null);
        name = ic.readString("name", null);
        ffBinding = ic.readEnum("ff_binding", FixedFuncBinding.class, null);
        switch (getVarType()){
            case Boolean:
                value = ic.readBoolean("value_bool", false);
                break;
            case Float:
                value = ic.readFloat("value_float", 0f);
                break;
            case Int:
                value = ic.readInt("value_int", 0);
                break;
            default:
                value = ic.readSavable("value_savable", null);
                break;
        }
    }

    @Override
    public boolean equals(Object other){
        if (!(other instanceof MatParam))
            return false;

        MatParam otherParam = (MatParam) other;
        return otherParam.type == type &&
               otherParam.name.equals(name);
    }

    @Override
    public String toString(){
        return type.name()+" "+name;
    }

    public void apply(Renderer r, Technique technique) {
        TechniqueDef techDef = technique.getDef();
        if (techDef.isUsingShaders()) {
            technique.updateUniformParam(getName(), getVarType(), getValue(), true);
        }
        if (ffBinding != null && r instanceof GL1Renderer){
            ((GL1Renderer)r).setFixedFuncBinding(ffBinding, getValue());
        }
    }
}

