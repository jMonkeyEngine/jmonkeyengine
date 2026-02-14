/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
package com.jme3.scene.plugins.gltf;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.jme3.util.BufferUtils;

/**
 * A package-private class to perform dequantization of buffers.
 * 
 * This handled buffers that contain (unsigned) byte or short values and that are "normalized", i.e. supposed
 * to be interpreted as float values.
 * 
 * (NOTE: Some of these methods are taken from a non-published state of JglTF, but published by the original
 * author, as part of JMonkeyEngine)
 */
class BufferQuantization {

    /**
     * Dequantize the given buffer into a float buffer, treating each element of the input as a signed byte.
     * 
     * @param byteBuffer
     *            The input buffer
     * @return The result
     */
    static FloatBuffer dequantizeByteBuffer(ByteBuffer byteBuffer) {
        FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(byteBuffer.capacity());
        for (int i = 0; i < byteBuffer.capacity(); i++) {
            byte c = byteBuffer.get(i);
            float f = dequantizeByte(c);
            floatBuffer.put(i, f);
        }
        return floatBuffer;
    }

    /**
     * Dequantize the given buffer into a float buffer, treating each element of the input as an unsigned
     * byte.
     * 
     * @param byteBuffer
     *            The input buffer
     * @return The result
     */
    static FloatBuffer dequantizeUnsignedByteBuffer(ByteBuffer byteBuffer) {
        FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(byteBuffer.capacity());
        for (int i = 0; i < byteBuffer.capacity(); i++) {
            byte c = byteBuffer.get(i);
            float f = dequantizeUnsignedByte(c);
            floatBuffer.put(i, f);
        }
        return floatBuffer;
    }

    /**
     * Dequantize the given buffer into a float buffer, treating each element of the input as a signed short.
     * 
     * @param shortBuffer
     *            The input buffer
     * @return The result
     */
    static FloatBuffer dequantizeShortBuffer(ShortBuffer shortBuffer) {
        FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(shortBuffer.capacity());
        for (int i = 0; i < shortBuffer.capacity(); i++) {
            short c = shortBuffer.get(i);
            float f = dequantizeShort(c);
            floatBuffer.put(i, f);
        }
        return floatBuffer;
    }

    /**
     * Dequantize the given buffer into a float buffer, treating each element of the input as an unsigned
     * short.
     * 
     * @param shortBuffer
     *            The input buffer
     * @return The result
     */
    static FloatBuffer dequantizeUnsignedShortBuffer(ShortBuffer shortBuffer) {
        FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(shortBuffer.capacity());
        for (int i = 0; i < shortBuffer.capacity(); i++) {
            short c = shortBuffer.get(i);
            float f = dequantizeUnsignedShort(c);
            floatBuffer.put(i, f);
        }
        return floatBuffer;
    }

    /**
     * Dequantize the given signed byte into a floating point value
     * 
     * @param c
     *            The input
     * @return The result
     */
    private static float dequantizeByte(byte c) {
        float f = Math.max(c / 127.0f, -1.0f);
        return f;
    }

    /**
     * Dequantize the given unsigned byte into a floating point value
     * 
     * @param c
     *            The input
     * @return The result
     */
    private static float dequantizeUnsignedByte(byte c) {
        int i = Byte.toUnsignedInt(c);
        float f = i / 255.0f;
        return f;
    }

    /**
     * Dequantize the given signed short into a floating point value
     * 
     * @param c
     *            The input
     * @return The result
     */
    private static float dequantizeShort(short c) {
        float f = Math.max(c / 32767.0f, -1.0f);
        return f;
    }

    /**
     * 
     * Dequantize the given unsigned byte into a floating point value
     * 
     * @param c
     *            The input
     * @return The result
     */
    private static float dequantizeUnsignedShort(short c) {
        int i = Short.toUnsignedInt(c);
        float f = i / 65535.0f;
        return f;
    }

    /**
     * Private constructor to prevent instantiation
     */
    private BufferQuantization() {
        // Private constructor to prevent instantiation
    }

}
