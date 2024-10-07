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
import org.junit.Test;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class UniformTest {

    @Test
    public void testSetValue_IntArray() {
        Uniform uniform = new Uniform();

        // Set value for the first time
        int[] intArray1 = new int[] {1, 2, 4, 8};
        uniform.setValue(VarType.IntArray, intArray1);

        assertTrue(uniform.getValue() instanceof IntBuffer);
        verifyIntBufferContent((IntBuffer) uniform.getValue(), intArray1);

        // Overriding the previous value
        int[] intArray2 = new int[] {3, 5, 7, 11, 13};
        uniform.setValue(VarType.IntArray, intArray2);

        assertTrue(uniform.getValue() instanceof IntBuffer);
        verifyIntBufferContent((IntBuffer) uniform.getValue(), intArray2);
    }

    private void verifyIntBufferContent(IntBuffer intBuffer, int[] intArray) {
        assertEquals(0, intBuffer.position());
        assertEquals(intArray.length, intBuffer.capacity());
        assertEquals(intArray.length, intBuffer.limit());

        for (int i = 0; i < intArray.length; i++) {
            assertEquals(intArray[i], intBuffer.get(i));
        }
    }


    @Test
    public void testSetValue_FloatArray() {
        Uniform uniform = new Uniform();

        // Set value for the first time
        float[] floatArray1 = new float[] {1.1f, 2.2f, 4.4f, 8.8f};
        uniform.setValue(VarType.FloatArray, floatArray1);

        verifyFloatBufferContent(uniform.getMultiData(), floatArray1);

        // Overriding the previous value
        float[] floatArray2 = new float[] {3.3f, 5.5f, 7.7f, 11.11f, 13.13f};
        uniform.setValue(VarType.FloatArray, floatArray2);

        verifyFloatBufferContent(uniform.getMultiData(), floatArray2);
    }


    @Test
    public void testSetValue_Vector2Array() {
        Uniform uniform = new Uniform();

        // Set value for the first time
        float[] expectedData1 = new float[] {
                1.1f, 2.2f,
                3.3f, 4.4f
        };
        Vector2f[] vector2Array1 = new Vector2f[] {
                new Vector2f(expectedData1[0], expectedData1[1]),
                new Vector2f(expectedData1[2], expectedData1[3])
        };
        uniform.setValue(VarType.Vector2Array, vector2Array1);

        verifyFloatBufferContent(uniform.getMultiData(), expectedData1);

        // Overriding the previous value
        float[] expectedData2 = new float[] {
                1.2f, 2.3f,
                3.4f, 4.5f,
                5.6f, 6.7f
        };
        Vector2f[] vector2Array2 = new Vector2f[] {
                new Vector2f(expectedData2[0], expectedData2[1]),
                new Vector2f(expectedData2[2], expectedData2[3]),
                new Vector2f(expectedData2[4], expectedData2[5])
        };
        uniform.setValue(VarType.Vector2Array, vector2Array2);

        verifyFloatBufferContent(uniform.getMultiData(), expectedData2);
    }


    @Test
    public void testSetValue_Vector3Array() {
        Uniform uniform = new Uniform();

        // Set value for the first time
        float[] expectedData1 = new float[] {
                1.1f, 2.2f, 3.3f,
                4.4f, 5.5f, 6.6f
        };
        Vector3f[] vector3Array1 = new Vector3f[] {
                new Vector3f(expectedData1[0], expectedData1[1], expectedData1[2]),
                new Vector3f(expectedData1[3], expectedData1[4], expectedData1[5])
        };
        uniform.setValue(VarType.Vector3Array, vector3Array1);

        verifyFloatBufferContent(uniform.getMultiData(), expectedData1);

        // Overriding the previous value
        float[] expectedData2 = new float[] {
                1.2f, 2.3f, 3.4f,
                4.5f, 5.6f, 6.7f,
                7.8f, 8.9f, 9.1f
        };
        Vector3f[] vector3Array2 = new Vector3f[] {
                new Vector3f(expectedData2[0], expectedData2[1], expectedData2[2]),
                new Vector3f(expectedData2[3], expectedData2[4], expectedData2[5]),
                new Vector3f(expectedData2[6], expectedData2[7], expectedData2[8])
        };
        uniform.setValue(VarType.Vector3Array, vector3Array2);

        verifyFloatBufferContent(uniform.getMultiData(), expectedData2);
    }


    @Test
    public void testSetValue_Vector4Array() {
        Uniform uniform = new Uniform();

        // Set value for the first time
        float[] expectedData1 = new float[] {
                1.1f, 2.2f, 3.3f, 4.4f,
                5.5f, 6.6f, 7.7f, 8.8f
        };
        Vector4f[] vector4Array1 = new Vector4f[] {
                new Vector4f(expectedData1[0], expectedData1[1], expectedData1[2], expectedData1[3]),
                new Vector4f(expectedData1[4], expectedData1[5], expectedData1[6], expectedData1[7])
        };
        uniform.setValue(VarType.Vector4Array, vector4Array1);

        verifyFloatBufferContent(uniform.getMultiData(), expectedData1);

        // Overriding the previous value
        float[] expectedData2 = new float[] {
                1.2f, 2.3f, 3.4f, 4.5f,
                5.6f, 6.7f, 7.8f, 8.9f,
                9.10f, 10.11f, 11.12f, 12.13f
        };
        Vector4f[] vector4Array2 = new Vector4f[] {
                new Vector4f(expectedData2[0], expectedData2[1], expectedData2[2], expectedData2[3]),
                new Vector4f(expectedData2[4], expectedData2[5], expectedData2[6], expectedData2[7]),
                new Vector4f(expectedData2[8], expectedData2[9], expectedData2[10], expectedData2[11])
        };
        uniform.setValue(VarType.Vector4Array, vector4Array2);

        verifyFloatBufferContent(uniform.getMultiData(), expectedData2);
    }


    @Test
    public void testSetValue_Matrix3Array() {
        Uniform uniform = new Uniform();

        // Set value for the first time
        float[] expectedData1 = new float[] {
                1.1f, 2.2f, 3.3f,
                4.4f, 5.5f, 6.6f,
                7.7f, 8.8f, 9.9f,

                10.10f, 11.11f, 12.12f,
                13.13f, 14.14f, 15.15f,
                16.16f, 17.17f, 18.18f
        };
        Matrix3f[] matrix3Array1 = new Matrix3f[] {
                new Matrix3f(
                        expectedData1[0], expectedData1[3], expectedData1[6],
                        expectedData1[1], expectedData1[4], expectedData1[7],
                        expectedData1[2], expectedData1[5], expectedData1[8]
                ),
                new Matrix3f(
                        expectedData1[9], expectedData1[12], expectedData1[15],
                        expectedData1[10], expectedData1[13], expectedData1[16],
                        expectedData1[11], expectedData1[14], expectedData1[17]
                )
        };
        uniform.setValue(VarType.Matrix3Array, matrix3Array1);

        verifyFloatBufferContent(uniform.getMultiData(), expectedData1);

        // Overriding the previous value
        float[] expectedData2 = new float[] {
                1.2f, 2.3f, 3.4f,
                4.5f, 5.6f, 6.7f,
                7.8f, 8.9f, 9.1f,

                10.11f, 11.12f, 12.13f,
                13.14f, 14.15f, 15.16f,
                16.17f, 17.18f, 18.19f,

                19.20f, 20.21f, 21.22f,
                22.23f, 23.24f, 24.25f,
                25.26f, 26.27f, 27.28f
        };
        Matrix3f[] matrix3Array2 = new Matrix3f[] {
                new Matrix3f(
                        expectedData2[0], expectedData2[3], expectedData2[6],
                        expectedData2[1], expectedData2[4], expectedData2[7],
                        expectedData2[2], expectedData2[5], expectedData2[8]
                ),
                new Matrix3f(
                        expectedData2[9], expectedData2[12], expectedData2[15],
                        expectedData2[10], expectedData2[13], expectedData2[16],
                        expectedData2[11], expectedData2[14], expectedData2[17]
                ),
                new Matrix3f(
                        expectedData2[18], expectedData2[21], expectedData2[24],
                        expectedData2[19], expectedData2[22], expectedData2[25],
                        expectedData2[20], expectedData2[23], expectedData2[26]
                )
        };
        uniform.setValue(VarType.Matrix3Array, matrix3Array2);

        verifyFloatBufferContent(uniform.getMultiData(), expectedData2);
    }


    @Test
    public void testSetValue_Matrix4Array() {
        Uniform uniform = new Uniform();

        // Set value for the first time
        float[] expectedData1 = new float[] {
                1.1f, 2.2f, 3.3f, 4.4f,
                5.5f, 6.6f, 7.7f, 8.8f,
                9.9f, 10.10f, 11.11f, 12.12f,
                13.13f, 14.14f, 15.15f, 16.16f,

                17.17f, 18.18f, 19.19f, 20.20f,
                21.21f, 22.22f, 23.23f, 24.24f,
                25.25f, 26.26f, 27.27f, 28.28f,
                29.29f, 30.30f, 31.31f, 32.32f
        };
        Matrix4f[] matrix4Array1 = new Matrix4f[] {
                new Matrix4f(Arrays.copyOfRange(expectedData1, 0, 16)),
                new Matrix4f(Arrays.copyOfRange(expectedData1, 16, 32))
        };
        uniform.setValue(VarType.Matrix4Array, matrix4Array1);

        verifyFloatBufferContent(uniform.getMultiData(), expectedData1);

        // Overriding the previous value
        float[] expectedData2 = new float[] {
                1.2f, 2.3f, 3.4f, 4.5f,
                5.6f, 6.7f, 7.8f, 8.9f,
                9.1f, 10.11f, 11.12f, 12.13f,
                13.14f, 14.15f, 15.16f, 16.17f,

                17.18f, 18.19f, 19.20f, 20.21f,
                21.22f, 22.23f, 23.24f, 24.25f,
                25.26f, 26.27f, 27.28f, 28.29f,
                29.30f, 30.31f, 31.32f, 32.33f,

                33.34f, 34.35f, 35.36f, 36.37f,
                37.38f, 38.39f, 39.40f, 40.41f,
                41.42f, 42.43f, 43.44f, 44.45f,
                45.46f, 46.47f, 47.48f, 48.49f
        };
        Matrix4f[] matrix4Array2 = new Matrix4f[] {
                new Matrix4f(Arrays.copyOfRange(expectedData2, 0, 16)),
                new Matrix4f(Arrays.copyOfRange(expectedData2, 16, 32)),
                new Matrix4f(Arrays.copyOfRange(expectedData2, 32, 48))
        };
        uniform.setValue(VarType.Matrix4Array, matrix4Array2);

        verifyFloatBufferContent(uniform.getMultiData(), expectedData2);
    }

    private void verifyFloatBufferContent(FloatBuffer floatBuffer, float[] floatArray) {
        assertEquals(0, floatBuffer.position());
        assertEquals(floatArray.length, floatBuffer.capacity());
        assertEquals(floatArray.length, floatBuffer.limit());

        for (int i = 0; i < floatArray.length; i++) {
            assertEquals(floatArray[i], floatBuffer.get(i), 0f);
        }
    }

}
