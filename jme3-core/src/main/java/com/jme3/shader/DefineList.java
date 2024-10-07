/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 * The new define list.
 *
 * @author Kirill Vainer
 */
public final class DefineList {

    private final BitSet isSet;
    private final int[] values;

    public DefineList(int numValues) {
        if (numValues < 0) {
            throw new IllegalArgumentException("numValues must be >= 0");
        }
        values = new int[numValues];
        isSet = new BitSet(numValues);
    }

    private DefineList(DefineList original) {
        this.isSet = (BitSet) original.isSet.clone();
        this.values = new int[original.values.length];
        System.arraycopy(original.values, 0, values, 0, values.length);
    }

    private void rangeCheck(int id) {
        assert 0 <= id && id < values.length;
    }

    public boolean isSet(int id) {
        rangeCheck(id);
        return isSet.get(id);
    }

    public void unset(int id) {
        rangeCheck(id);
        isSet.clear(id);
        values[id] = 0;
    }

    public void set(int id, int val) {
        rangeCheck(id);
        isSet.set(id, true);
        values[id] = val;
    }

    public void set(int id, float val) {
        set(id, Float.floatToIntBits(val));
    }

    public void set(int id, boolean val) {
        if (val) {
            set(id, 1);
        } else {
            // Because #ifdef usage is very common in shaders, unset the define
            // instead of setting it to 0 for booleans.
            unset(id);
        }
    }

    public void set(int id, VarType type, Object value) {
        if (value != null) {
            switch (type) {
                case Int:
                    set(id, (Integer) value);
                    break;
                case Float:
                    set(id, (Float) value);
                    break;
                case Boolean:
                    set(id, ((Boolean) value));
                    break;
                default:
                    set(id, 1);
                    break;
            }
        } else {
            unset(id);
        }
    }

    public void setAll(DefineList other) {
        for (int i = 0; i < other.values.length; i++) {
            if (other.isSet(i)) {
                set(i, other.getInt(i));
            }
        }
    }

    public void clear() {
        isSet.clear();
        Arrays.fill(values, 0);
    }

    public boolean getBoolean(int id) {
        return values[id] != 0;
    }

    public float getFloat(int id) {
        return Float.intBitsToFloat(values[id]);
    }

    public int getInt(int id) {
        return values[id];
    }

    @Override
    public int hashCode() {
        return isSet.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || object.getClass() != getClass()) {
            return false;
        }
        DefineList otherDefineList = (DefineList) object;
        if (values.length != otherDefineList.values.length) {
            return false;
        }
        if (!isSet.equals(otherDefineList.isSet)) {
            return false;
        }
        for (int i = 0; i < values.length; i++) {
            if (values[i] != otherDefineList.values[i]) {
                return false;
            }
        }
        return true;
    }

    public DefineList deepClone() {
        return new DefineList(this);
    }

    public void generateSource(StringBuilder sb, List<String> defineNames, List<VarType> defineTypes) {
        for (int i = 0; i < values.length; i++) {
            if (!isSet(i)) {
                continue;
            }

            sb.append("#define ").append(defineNames.get(i)).append(' ');

            if (defineTypes != null && defineTypes.get(i) == VarType.Float) {
                float val = Float.intBitsToFloat(values[i]);
                if (Float.isInfinite(val) || Float.isNaN(val)) {
                    throw new IllegalArgumentException(
                            "GLSL does not support NaN "
                            + "or Infinite float literals");
                }
                sb.append(val);
            } else {
                sb.append(values[i]);
            }

            sb.append('\n');
        }
    }

    public String generateSource(List<String> defineNames, List<VarType> defineTypes) {
        StringBuilder sb = new StringBuilder();
        generateSource(sb, defineNames, defineTypes);
        return sb.toString();
    }
}
