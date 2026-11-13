/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.material;

import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.shader.VarType;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MatParamTest {

    @Test
    public void toStringUsesValueWhenJ3mFormatIsUnavailable() {
        Matrix4f value = new Matrix4f();
        MatParam parameter = new MatParam(VarType.Matrix4, "Transform", value);
        Matrix3f normalMatrix = new Matrix3f();
        MatParam matrix3Parameter = new MatParam(VarType.Matrix3, "NormalMatrix", normalMatrix);

        assertNull(parameter.getValueAsString());
        assertEquals("Matrix4 Transform : " + value, parameter.toString());
        assertNull(matrix3Parameter.getValueAsString());
        assertEquals("Matrix3 NormalMatrix : " + normalMatrix, matrix3Parameter.toString());
    }

    @Test
    public void toStringShowsPrimitiveArrayContentsWhenJ3mFormatIsUnavailable() {
        int[] indices = {1, 3, 5};
        float[] weights = {0.25f, 0.75f};
        MatParam intParameter = new MatParam(VarType.IntArray, "Indices", indices);
        MatParam floatParameter = new MatParam(VarType.FloatArray, "Weights", weights);

        assertEquals("IntArray Indices : " + Arrays.toString(indices), intParameter.toString());
        assertEquals("FloatArray Weights : " + Arrays.toString(weights), floatParameter.toString());
    }

    @Test
    public void toStringShowsObjectArrayContentsWhenJ3mFormatIsUnavailable() {
        Vector3f[] value = {new Vector3f(1f, 2f, 3f)};
        MatParam parameter = new MatParam(VarType.Vector3Array, "Directions", value);

        assertEquals("Vector3Array Directions : " + Arrays.deepToString(value), parameter.toString());
    }

    @Test
    public void toStringPreservesJ3mFormattingForSupportedValues() {
        MatParam parameter = new MatParam(VarType.Vector3, "Direction", new Vector3f(1f, 2f, 3f));

        assertEquals("Vector3 Direction : 1.0 2.0 3.0", parameter.toString());
    }

    @Test
    public void toStringOmitsValueWhenParameterValueIsNull() {
        MatParam parameter = new MatParam(VarType.Matrix4, "Transform", null);

        assertEquals("Matrix4 Transform", parameter.toString());
    }
}
