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

import com.jme3.asset.TextureKey;
import com.jme3.export.*;
import com.jme3.math.*;
import com.jme3.renderer.GL1Renderer;
import com.jme3.renderer.Renderer;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import java.io.IOException;

/**
 * Describes a material parameter. This is used for both defining a name and type
 * as well as a material parameter value.
 *
 * @author Kirill Vainer
 */
public class MatParam implements Savable, Cloneable {

    protected VarType type;
    protected String name;
    protected String prefixedName;
    protected Object value;
    protected FixedFuncBinding ffBinding;

    /**
     * Create a new material parameter. For internal use only.
     */
    public MatParam(VarType type, String name, Object value, FixedFuncBinding ffBinding) {
        this.type = type;
        this.name = name;
        this.prefixedName = "m_" + name;
        this.value = value;
        this.ffBinding = ffBinding;
    }

    /**
     * Serialization only. Do not use.
     */
    public MatParam() {
    }

    /**
     * Returns the fixed function binding.
     *
     * @return the fixed function binding.
     */
    public FixedFuncBinding getFixedFuncBinding() {
        return ffBinding;
    }

    /**
     * Returns the material parameter type.
     *
     * @return the material parameter type.
     */
    public VarType getVarType() {
        return type;
    }

    /**
     * Returns the name of the material parameter.
     * @return the name of the material parameter.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the name with "m_" prefixed to it.
     *
     * @return the name with "m_" prefixed to it
     */
    public String getPrefixedName() {
        return prefixedName;
    }

    /**
     * Used internally
     * @param name
     */
    void setName(String name) {
        this.name = name;
        this.prefixedName = "m_" + name;
    }

    /**
     * Returns the value of this material parameter.
     * <p>
     * Material parameters that are used for material definitions
     * will not have a value, unless there's a default value declared
     * in the definition.
     *
     * @return the value of this material parameter.
     */
    public Object getValue() {
        return value;
    }

    /**
     * Sets the value of this material parameter.
     * <p>
     * It is assumed the value is of the same {@link MatParam#getVarType() type}
     * as this material parameter.
     *
     * @param value the value of this material parameter.
     */
    public void setValue(Object value) {
        this.value = value;
    }

    void apply(Renderer r, Technique technique) {
        TechniqueDef techDef = technique.getDef();
        if (techDef.isUsingShaders()) {
            technique.updateUniformParam(getPrefixedName(), getVarType(), getValue(), true);
        }
        if (ffBinding != null && r instanceof GL1Renderer) {
            ((GL1Renderer) r).setFixedFuncBinding(ffBinding, getValue());
        }
    }

    /**
     * Returns the material parameter value as it would appear in a J3M
     * file. E.g.<br/>
     * <code>
     * MaterialParameters {<br/>
     *     ABC : 1 2 3 4<br/>
     * }<br/>
     * </code>
     * Assuming "ABC" is a Vector4 parameter, then the value
     * "1 2 3 4" would be returned by this method.
     * <br/><br/>
     * @return material parameter value as it would appear in a J3M file.
     */
    public String getValueAsString() {
        switch (type) {
            case Boolean:
            case Float:
            case Int:
                return value.toString();
            case Vector2:
                Vector2f v2 = (Vector2f) value;
                return v2.getX() + " " + v2.getY();
/* 
This may get used at a later point of time
When arrays can be inserted in J3M files

            case Vector2Array:
                Vector2f[] v2Arr = (Vector2f[]) value;
                String v2str = "";
                for (int i = 0; i < v2Arr.length ; i++) {
                    v2str += v2Arr[i].getX() + " " + v2Arr[i].getY() + "\n";
                }
                return v2str;
*/
            case Vector3:
                Vector3f v3 = (Vector3f) value;
                return v3.getX() + " " + v3.getY() + " " + v3.getZ();
/*
            case Vector3Array:
                Vector3f[] v3Arr = (Vector3f[]) value;
                String v3str = "";
                for (int i = 0; i < v3Arr.length ; i++) {
                    v3str += v3Arr[i].getX() + " "
                            + v3Arr[i].getY() + " "
                            + v3Arr[i].getZ() + "\n";
                }
                return v3str;
            case Vector4Array:
                // can be either ColorRGBA, Vector4f or Quaternion
                if (value instanceof Vector4f) {
                    Vector4f[] v4arr = (Vector4f[]) value;
                    String v4str = "";
                    for (int i = 0; i < v4arr.length ; i++) {
                        v4str += v4arr[i].getX() + " "
                                + v4arr[i].getY() + " "
                                + v4arr[i].getZ() + " "
                                + v4arr[i].getW() + "\n";
                    }
                    return v4str;
                } else if (value instanceof ColorRGBA) {
                    ColorRGBA[] colorArr = (ColorRGBA[]) value;
                    String colStr = "";
                    for (int i = 0; i < colorArr.length ; i++) {
                        colStr += colorArr[i].getRed() + " "
                                + colorArr[i].getGreen() + " "
                                + colorArr[i].getBlue() + " "
                                + colorArr[i].getAlpha() + "\n";
                    }
                    return colStr;
                } else if (value instanceof Quaternion) {
                    Quaternion[] quatArr = (Quaternion[]) value;
                    String quatStr = "";
                    for (int i = 0; i < quatArr.length ; i++) {
                        quatStr += quatArr[i].getX() + " "
                                + quatArr[i].getY() + " "
                                + quatArr[i].getZ() + " "
                                + quatArr[i].getW() + "\n";
                    }
                    return quatStr;
                } else {
                    throw new UnsupportedOperationException("Unexpected Vector4Array type: " + value);
                }
*/
            case Vector4:
                // can be either ColorRGBA, Vector4f or Quaternion
                if (value instanceof Vector4f) {
                    Vector4f v4 = (Vector4f) value;
                    return v4.getX() + " " + v4.getY() + " "
                            + v4.getZ() + " " + v4.getW();
                } else if (value instanceof ColorRGBA) {
                    ColorRGBA color = (ColorRGBA) value;
                    return color.getRed() + " " + color.getGreen() + " "
                            + color.getBlue() + " " + color.getAlpha();
                } else if (value instanceof Quaternion) {
                    Quaternion quat = (Quaternion) value;
                    return quat.getX() + " " + quat.getY() + " "
                            + quat.getZ() + " " + quat.getW();
                } else {
                    throw new UnsupportedOperationException("Unexpected Vector4 type: " + value);
                }
            case Texture2D:
            case Texture3D:
            case TextureArray:
            case TextureBuffer:
            case TextureCubeMap:
                Texture texVal = (Texture) value;
                TextureKey texKey = (TextureKey) texVal.getKey();
                if (texKey == null){
                    throw new UnsupportedOperationException("The specified MatParam cannot be represented in J3M");
                }

                String ret = "";
                if (texKey.isFlipY()) {
                    ret += "Flip ";
                }
                if (texVal.getWrap(Texture.WrapAxis.S) == WrapMode.Repeat) {
                    ret += "Repeat ";
                }

                return ret + texKey.getName();
            default:
                return null; // parameter type not supported in J3M
        }
    }

    @Override
    public MatParam clone() {
        try {
            MatParam param = (MatParam) super.clone();
            return param;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(type, "varType", null);
        oc.write(name, "name", null);
        oc.write(ffBinding, "ff_binding", null);
        if (value instanceof Savable) {
            Savable s = (Savable) value;
            oc.write(s, "value_savable", null);
        } else if (value instanceof Float) {
            Float f = (Float) value;
            oc.write(f.floatValue(), "value_float", 0f);
        } else if (value instanceof Integer) {
            Integer i = (Integer) value;
            oc.write(i.intValue(), "value_int", 0);
        } else if (value instanceof Boolean) {
            Boolean b = (Boolean) value;
            oc.write(b.booleanValue(), "value_bool", false);
        }
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        type = ic.readEnum("varType", VarType.class, null);
        name = ic.readString("name", null);
        ffBinding = ic.readEnum("ff_binding", FixedFuncBinding.class, null);
        switch (getVarType()) {
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
    public boolean equals(Object other) {
        if (!(other instanceof MatParam)) {
            return false;
        }

        MatParam otherParam = (MatParam) other;
        return otherParam.type == type
                && otherParam.name.equals(name);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 17 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 17 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return type.name() + " " + name + " : " + getValueAsString();
    }
}
