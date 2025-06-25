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

import com.jme3.material.Material.BindUnits;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.util.BufferUtils;
import com.jme3.util.TempVars;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

/**
 * Represents a uniform variable in a shader program.
 * <p>
 * A uniform is a way to pass data from the CPU to the GPU. This class manages
 * the value of the uniform, its type, and its binding to renderer values.
 */
public class Uniform extends ShaderVariable {

    private static final Integer ZERO_INT = 0;
    private static final Float ZERO_FLT = 0.0f;
    // Pre-allocated buffer for clearing multiData to zeros efficiently.
    private static final FloatBuffer ZERO_BUF = BufferUtils.createFloatBuffer(16); //Max 4x4 matrix elements

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
        return Objects.hash(value, varType, binding);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Uniform)) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        final Uniform other = (Uniform) obj;
        return Objects.equals(this.value, other.value) &&
                this.binding == other.binding &&
                this.varType == other.varType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Uniform[name=");
        sb.append(name);
        if (varType != null) {
            sb.append(", type=");
            sb.append(varType);
            sb.append(", value=");
            sb.append(value);
        } else {
            sb.append(", value=<not set>");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Sets the {@link UniformBinding} for this uniform.
     *
     * @param binding The UniformBinding to set.
     */
    public void setBinding(UniformBinding binding) {
        this.binding = binding;
    }

    /**
     * Returns the {@link UniformBinding} associated with this uniform.
     *
     * @return The UniformBinding, or null if it's a user-defined uniform.
     */
    public UniformBinding getBinding() {
        return binding;
    }

    /**
     * Returns the {@link VarType} of this uniform.
     *
     * @return The variable type of the uniform.
     */
    public VarType getVarType() {
        return varType;
    }

    /**
     * Returns the currently set value of the uniform.
     *
     * @return The value of the uniform.
     */
    public Object getValue() {
        return value;
    }

    /**
     * Returns the {@link FloatBuffer} containing the multi-data for array or matrix uniforms.
     *
     * @return The FloatBuffer containing the uniform data, or null if not applicable.
     */
    public FloatBuffer getMultiData() {
        return multiData;
    }

    /**
     * Checks if this uniform's value was set by the current material.
     *
     * @return true if set by the current material, false otherwise.
     */
    public boolean isSetByCurrentMaterial() {
        return setByCurrentMaterial;
    }

    /**
     * Clears the {@code setByCurrentMaterial} flag, indicating that the uniform
     * was not set by the current material. This is typically called when
     * a material is unbound.
     */
    public void clearSetByCurrentMaterial() {
        setByCurrentMaterial = false;
    }

    /**
     * Clears the current value of the uniform, resetting it to a default
     * "zero" or identity state based on its {@link VarType}.
     */
    public void clearValue() {
        updateNeeded = true;

        if (multiData != null) {
            multiData.clear();

            while (multiData.remaining() > 0) {
                ZERO_BUF.clear();
                ZERO_BUF.limit(Math.min(multiData.remaining(), 16));
                multiData.put(ZERO_BUF);
            }

            multiData.clear();

            return;
        }

        if (varType == null) {
            return;
        }

        switch (varType) {
            case Int:
                value = ZERO_INT;
                break;
            case Boolean:
                value = Boolean.FALSE;
                break;
            case Float:
                value = ZERO_FLT;
                break;
            case Vector2:
                if (value != null) {
                    ((Vector2f) value).set(Vector2f.ZERO);
                }
                break;
            case Vector3:
                if (value != null) {
                    ((Vector3f) value).set(Vector3f.ZERO);
                }
                break;
            case Vector4:
                if (value != null) {
                    if (value instanceof ColorRGBA) {
                        ((ColorRGBA) value).set(ColorRGBA.BlackNoAlpha);
                    } else if (value instanceof Vector4f) {
                        ((Vector4f) value).set(Vector4f.ZERO);
                    } else {
                        ((Quaternion) value).set(Quaternion.ZERO);
                    }
                }
                break;
            default:
                // won't happen because those are either textures
                // or multidata types
        }
    }

    /**
     * Sets the value of the uniform.
     * The type of the value must match the uniform's expected {@link VarType}.
     *
     * @param type The {@link VarType} of the value being set.
     * @param uValue The new value for the uniform. Cannot be null.
     */
    public void setValue(VarType type, Object uValue) {
        assert !(uValue instanceof BindUnits);
        if (location == LOC_NOT_DEFINED) {
            return;
        }

        if (varType != null && varType != type) {
            throw new IllegalArgumentException("Expected a " + varType.name() + " value!");
        }

        if (uValue == null) {
            throw new IllegalArgumentException("for uniform " + name + ": value cannot be null");
        }

        setByCurrentMaterial = true;

        switch (type) {
            case Matrix3:
                if (uValue.equals(this.value)) {
                    return;
                }
                Matrix3f m3 = (Matrix3f) uValue;
                if (multiData == null) {
                    multiData = BufferUtils.createFloatBuffer(9);
                }
                m3.fillFloatBuffer(multiData, true);
                multiData.clear();
                if (this.value == null) {
                    this.value = new Matrix3f(m3);
                } else {
                    ((Matrix3f) this.value).set(m3);
                }
                break;
            case Matrix4:
                if (uValue.equals(this.value)) {
                    return;
                }
                Matrix4f m4 = (Matrix4f) uValue;
                if (multiData == null) {
                    multiData = BufferUtils.createFloatBuffer(16);
                }
                m4.fillFloatBuffer(multiData, true);
                multiData.clear();
                if (this.value == null) {
                    this.value = new Matrix4f(m4);
                } else {
                    ((Matrix4f) this.value).copy(m4);
                }
                break;
            case IntArray:
                int[] ia = (int[]) uValue;
                if (this.value == null) {
                    this.value = BufferUtils.createIntBuffer(ia);
                } else {
                    this.value = BufferUtils.ensureLargeEnough((IntBuffer) this.value, ia.length);
                    ((IntBuffer) this.value).put(ia);
                }
                ((IntBuffer) this.value).clear();
                break;
            case FloatArray:
                float[] fa = (float[]) uValue;
                if (multiData == null) {
                    multiData = BufferUtils.createFloatBuffer(fa);
                } else {
                    multiData = BufferUtils.ensureLargeEnough(multiData, fa.length);
                    multiData.put(fa);
                }
                multiData.clear();
                break;
            case Vector2Array:
                Vector2f[] v2a = (Vector2f[]) uValue;
                if (multiData == null) {
                    multiData = BufferUtils.createFloatBuffer(v2a);
                } else {
                    multiData = BufferUtils.ensureLargeEnough(multiData, v2a.length * 2);
                    for (int i = 0; i < v2a.length; i++) {
                        BufferUtils.setInBuffer(v2a[i], multiData, i);
                    }
                }
                multiData.clear();
                break;
            case Vector3Array:
                Vector3f[] v3a = (Vector3f[]) uValue;
                if (multiData == null) {
                    multiData = BufferUtils.createFloatBuffer(v3a);
                } else {
                    multiData = BufferUtils.ensureLargeEnough(multiData, v3a.length * 3);
                    for (int i = 0; i < v3a.length; i++) {
                        BufferUtils.setInBuffer(v3a[i], multiData, i);
                    }
                }
                multiData.clear();
                break;
            case Vector4Array:
                Vector4f[] v4a = (Vector4f[]) uValue;
                if (multiData == null) {
                    multiData = BufferUtils.createFloatBuffer(v4a);
                } else {
                    multiData = BufferUtils.ensureLargeEnough(multiData, v4a.length * 4);
                    for (int i = 0; i < v4a.length; i++) {
                        BufferUtils.setInBuffer(v4a[i], multiData, i);
                    }
                }
                multiData.clear();
                break;
            case Matrix3Array:
                Matrix3f[] m3a = (Matrix3f[]) uValue;
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
                Matrix4f[] m4a = (Matrix4f[]) uValue;
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
                if (uValue.equals(this.value)) {
                    return;
                }
                if (this.value == null) {
                    this.value = new Vector2f((Vector2f) uValue);
                } else {
                    ((Vector2f) this.value).set((Vector2f) uValue);
                }
                break;
            case Vector3:
                if (uValue.equals(this.value)) {
                    return;
                }
                if (this.value == null) {
                    this.value = new Vector3f((Vector3f) uValue);
                } else {
                    ((Vector3f) this.value).set((Vector3f) uValue);
                }
                break;
            case Vector4:
                if (uValue.equals(this.value)) {
                    return;
                }

                TempVars vars = TempVars.get();
                Vector4f vec4 = vars.vect4f1;
                //handle the null case
                if (this.value == null) {
                    try {
                        this.value = uValue.getClass().getDeclaredConstructor().newInstance();
                    } catch (ReflectiveOperationException | IllegalArgumentException | SecurityException e) {
                        vars.release();
                        throw new IllegalArgumentException("Cannot instantiate param of class " + uValue.getClass().getCanonicalName(), e);
                    }
                }
                //feed the pivot vec 4 with the correct value
                if (uValue instanceof ColorRGBA) {
                    ColorRGBA c = (ColorRGBA) uValue;
                    vec4.set(c.r, c.g, c.b, c.a);
                } else if (uValue instanceof Vector4f) {
                    vec4.set((Vector4f) uValue);
                } else {
                    Quaternion q = (Quaternion) uValue;
                    vec4.set(q.getX(), q.getY(), q.getZ(), q.getW());
                }

                //feed this.value with the collected values.
                if (this.value instanceof ColorRGBA) {
                    ((ColorRGBA) this.value).set(vec4.x, vec4.y, vec4.z, vec4.w);
                } else if (this.value instanceof Vector4f) {
                    ((Vector4f) this.value).set(vec4);
                } else {
                    ((Quaternion) this.value).set(vec4.x, vec4.y, vec4.z, vec4.w);
                }
                vars.release();
                break;
            // Only use check if equals optimization for primitive values
            case Int:
            case Float:
            case Boolean:
                if (uValue.equals(this.value)) {
                    return;
                }
                this.value = uValue;
                break;
            default:
                this.value = uValue;
                break;
        }

//        if (multiData != null) {
//            this.value = multiData;
//        }

        varType = type;
        updateNeeded = true;
    }

    /**
     * Pre-allocates or resizes the {@link FloatBuffer multiData} for a {@code Vector4Array}
     * uniform to a specified length.
     *
     * @param length The desired number of Vector4 elements in the array.
     */
    public void setVector4Length(int length) {
        if (location == -1) {
            return;
        }

        multiData = BufferUtils.ensureLargeEnough(multiData, length * 4);
        value = multiData;
        varType = VarType.Vector4Array;
        updateNeeded = true;
        setByCurrentMaterial = true;
    }

    /**
     * Sets the components of a specific {@code Vector4} element within a {@code Vector4Array}
     * uniform's {@link FloatBuffer multiData}.
     * This method requires the uniform to be of type {@code Vector4Array}.
     *
     * @param x The x-component of the Vector4.
     * @param y The y-component of the Vector4.
     * @param z The z-component of the Vector4.
     * @param w The w-component of the Vector4.
     * @param index The index of the Vector4 element to set within the array.
     * @throws IllegalArgumentException if the uniform is not of type {@code Vector4Array}.
     */
    public void setVector4InArray(float x, float y, float z, float w, int index) {
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

    /**
     * Checks if the uniform's value needs to be updated on the GPU.
     *
     * @return true if an update is needed, false otherwise.
     */
    public boolean isUpdateNeeded() {
        return updateNeeded;
    }

    /**
     * Clears the {@code updateNeeded} flag, indicating that the uniform's value
     * has been successfully sent to the GPU.
     */
    public void clearUpdateNeeded() {
        updateNeeded = false;
    }

    /**
     * Resets the uniform's state. This includes clearing the
     * {@code setByCurrentMaterial} flag, setting its location to undefined,
     * and marking it as needing an update. This is typically used when a shader
     * is unbound or reloaded.
     */
    public void reset() {
        setByCurrentMaterial = false;
        location = -2;
        updateNeeded = true;
    }

    /**
     * Deletes any native direct buffers associated with this uniform's value.
     * This is crucial for releasing native memory resources when the uniform
     * is no longer needed.
     */
    public void deleteNativeBuffers() {
        if (value instanceof Buffer) {
            BufferUtils.destroyDirectBuffer((Buffer) value);
            // It's important to nullify the reference after destroying the buffer
            // to allow the Java garbage collector to reclaim the Uniform object
            // itself if no other references exist.
            value = null;
        }
    }
}
