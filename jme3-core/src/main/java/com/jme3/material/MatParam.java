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
package com.jme3.material;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import com.jme3.asset.TextureKey;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

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
    protected boolean typeCheck = true;

    /**
     * Create a new material parameter. For internal use only.
     *
     * @param type  the type of the parameter
     * @param name  the desired parameter name
     * @param value the desired parameter value (alias created)
     */
    public MatParam(VarType type, String name, Object value) {
        this.type = type;
        this.name = name;
        this.prefixedName = "m_" + name;
        this.value = value;
    }

    /**
     * Serialization only. Do not use.
     */
    protected MatParam() {
    }

    /**
     * Checks if type checking is enabled for this material parameter.
     *
     * @return true if type checking is enabled, false otherwise.
     */
    public boolean isTypeCheckEnabled() {
        return typeCheck;
    }

    /**
     * Enable type check for this param.
     * When type check is enabled a RuntimeException is thrown if 
     * an object of the wrong type is passed to setValue.
     * 
     * @param typeCheck (default = true)
     */
    public void setTypeCheckEnabled(boolean typeCheck) {
        this.typeCheck = typeCheck;
    }

    /**
     * Returns the material parameter type.
     *
     * @return The {@link VarType} of this material parameter.
     */
    public VarType getVarType() {
        return type;
    }

    /**
     * Returns the name of the material parameter.
     * 
     * @return The name of the material parameter.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the name of the material parameter prefixed with "m_".
     * This prefixed name is commonly used for internal uniform naming
     * in shaders (e.g., "m_Color" for a parameter named "Color").
     *
     * @return The prefixed name of the material parameter.
     */
    public String getPrefixedName() {
        return prefixedName;
    }

    /**
     * Internal use only.
     *
     * @param name The name for the parameter. Must not be null.
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
     * It is generally assumed that the value's type
     * corresponds to the {@link MatParam#getVarType() type} of this parameter.
     *
     * @param value The new value for this material parameter. Can be null.
     * @throws RuntimeException if type checking is enabled and the provided
     * value's type is incompatible with the parameter's
     * defined variable type.
     */
    public void setValue(Object value) {
        if (isTypeCheckEnabled()) {
            if (value != null) {
                validateValueType(value);
            }
        }
        this.value = value;
    }

    /**
     * Validates if the provided value is compatible with the parameter's type.
     *
     * @param value The value to validate.
     * @throws RuntimeException if the value's type is not compatible.
     */
    private void validateValueType(Object value) {
        if (type.getJavaType().length != 0) {
            boolean valid = false;
            for (Class<?> javaType : type.getJavaType()) {
                if (javaType.isAssignableFrom(value.getClass())) {
                    valid = true;
                    break;
                }
            }
            if (!valid) {
                throw new RuntimeException("Trying to assign a value of type " + value.getClass()
                        + " to " + this.getName()
                        + " of type " + type.name()
                        + ". Valid types are " + Arrays.deepToString(type.getJavaType()));
            }
        }
    }

    /**
     * Returns the material parameter value as it would appear in a J3M
     * file. E.g.
     * <pre>
     * MaterialParameters {
     *     ABC : 1 2 3 4
     * }
     * </pre>
     * Assuming "ABC" is a Vector4 parameter, then the value
     * "1 2 3 4" would be returned by this method.
     *
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
                return v2.x + " " + v2.y;
            case Vector3:
                Vector3f v3 = (Vector3f) value;
                return v3.x + " " + v3.y + " " + v3.z;
            case Vector4:
                // can be either ColorRGBA, Vector4f or Quaternion
                if (value instanceof Vector4f) {
                    Vector4f v4 = (Vector4f) value;
                    return v4.x + " " + v4.y + " " + v4.z + " " + v4.w;
                } else if (value instanceof ColorRGBA) {
                    ColorRGBA color = (ColorRGBA) value;
                    return color.r + " " + color.g + " " + color.b + " " + color.a;
                } else if (value instanceof Quaternion) {
                    Quaternion q = (Quaternion) value;
                    return q.getX() + " " + q.getY() + " " + q.getZ() + " " + q.getW();
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
                if (texKey == null) {
                    return texVal + ":returned null key";
                }

                String ret = "";
                if (texKey.isFlipY()) {
                    ret += "Flip ";
                }

                // Wrap mode
                ret += getWrapMode(texVal, Texture.WrapAxis.S);
                ret += getWrapMode(texVal, Texture.WrapAxis.T);
                ret += getWrapMode(texVal, Texture.WrapAxis.R);

                // Min and Mag filter
                Texture.MinFilter def = Texture.MinFilter.BilinearNoMipMaps;
                if (texVal.getImage().hasMipmaps() || texKey.isGenerateMips()) {
                    def = Texture.MinFilter.Trilinear;
                }
                if (texVal.getMinFilter() != def) {
                    ret += "Min" + texVal.getMinFilter().name() + " ";
                }

                if (texVal.getMagFilter() != Texture.MagFilter.Bilinear) {
                    ret += "Mag" + texVal.getMagFilter().name() + " ";
                }

                return ret + "\"" + texKey.getName() + "\"";
            default:
                return null; // parameter type not supported in J3M
        }
    }

    private String getWrapMode(Texture texVal, Texture.WrapAxis axis) {
        try {
            WrapMode mode = texVal.getWrap(axis);
            if (mode != WrapMode.EdgeClamp) {
                return "Wrap" + mode.name() + "_" + axis.name() + " ";
            }
        } catch (IllegalArgumentException ex) {
            // this axis doesn't exist on the texture
        }

        return "";
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

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(type, "varType", null);
        oc.write(name, "name", null);

        if (value == null) {
            return;
        }

        if (value instanceof Savable) {
            oc.write((Savable) value, "value_savable", null);
        } else if (value instanceof Float) {
            oc.write((Float) value, "value_float", 0f);
        } else if (value instanceof Integer) {
            oc.write((Integer) value, "value_int", 0);
        } else if (value instanceof Boolean) {
            oc.write((Boolean) value, "value_bool", false);
        } else if (value.getClass().isArray() && value instanceof Savable[]) {
            oc.write((Savable[]) value, "value_savable_array", null);
        }

        oc.write(typeCheck, "typeCheck", true);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        type = ic.readEnum("varType", VarType.class, null);
        name = ic.readString("name", null);
        prefixedName = "m_" + name;

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
            case Vector2Array:
                Savable[] vec2Array = ic.readSavableArray("value_savable_array", null);
                if (vec2Array != null) {
                    value = new Vector2f[vec2Array.length];
                    System.arraycopy(vec2Array, 0, value, 0, vec2Array.length);
                }
                break;
            case Vector3Array:
                Savable[] vec3Array = ic.readSavableArray("value_savable_array", null);
                if (vec3Array != null) {
                    value = new Vector3f[vec3Array.length];
                    System.arraycopy(vec3Array, 0, value, 0, vec3Array.length);
                }
                break;
            case Vector4Array:
                Savable[] vec4Array = ic.readSavableArray("value_savable_array", null);
                if (vec4Array != null) {
                    value = new Vector4f[vec4Array.length];
                    System.arraycopy(vec4Array, 0, value, 0, vec4Array.length);
                }
                break;
            case Matrix3Array:
                Savable[] mat3Array = ic.readSavableArray("value_savable_array", null);
                if (mat3Array != null) {
                    value = new Matrix3f[mat3Array.length];
                    System.arraycopy(mat3Array, 0, value, 0, mat3Array.length);
                }
                break;
            case Matrix4Array:
                Savable[] mat4Array = ic.readSavableArray("value_savable_array", null);
                if (mat4Array != null) {
                    value = new Matrix4f[mat4Array.length];
                    System.arraycopy(mat4Array, 0, value, 0, mat4Array.length);
                }
                break;
            default:
                value = ic.readSavable("value_savable", null);
                break;
        }

        typeCheck = ic.readBoolean("typeCheck", true);
    }

    @Override
    public boolean equals(Object obj) {
        // Optimization for same instance
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final MatParam other = (MatParam) obj;
        if (this.type != other.type) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.value, other.value)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, value);
    }

    @Override
    public String toString() {
        if (value != null) {
            String sValue = getValueAsString();
            return type.name() + " " + name + " : " + ((sValue != null) ? sValue : value);
        }
        return type.name() + " " + name;
    }
}
