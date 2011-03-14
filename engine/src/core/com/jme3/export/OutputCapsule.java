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

package com.jme3.export;

import com.jme3.util.IntMap;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Map;

/**
 * @author Joshua Slack
 */
public interface OutputCapsule {

    // byte primitive

    public void write(byte value, String name, byte defVal) throws IOException;
    public void write(byte[] value, String name, byte[] defVal) throws IOException;
    public void write(byte[][] value, String name, byte[][] defVal) throws IOException;


    // int primitive

    public void write(int value, String name, int defVal) throws IOException;
    public void write(int[] value, String name, int[] defVal) throws IOException;
    public void write(int[][] value, String name, int[][] defVal) throws IOException;


    // float primitive

    public void write(float value, String name, float defVal) throws IOException;
    public void write(float[] value, String name, float[] defVal) throws IOException;
    public void write(float[][] value, String name, float[][] defVal) throws IOException;


    // double primitive

    public void write(double value, String name, double defVal) throws IOException;
    public void write(double[] value, String name, double[] defVal) throws IOException;
    public void write(double[][] value, String name, double[][] defVal) throws IOException;


    // long primitive

    public void write(long value, String name, long defVal) throws IOException;
    public void write(long[] value, String name, long[] defVal) throws IOException;
    public void write(long[][] value, String name, long[][] defVal) throws IOException;


    // short primitive

    public void write(short value, String name, short defVal) throws IOException;
    public void write(short[] value, String name, short[] defVal) throws IOException;
    public void write(short[][] value, String name, short[][] defVal) throws IOException;


    // boolean primitive

    public void write(boolean value, String name, boolean defVal) throws IOException;
    public void write(boolean[] value, String name, boolean[] defVal) throws IOException;
    public void write(boolean[][] value, String name, boolean[][] defVal) throws IOException;


    // String

    public void write(String value, String name, String defVal) throws IOException;
    public void write(String[] value, String name, String[] defVal) throws IOException;
    public void write(String[][] value, String name, String[][] defVal) throws IOException;


    // BitSet

    public void write(BitSet value, String name, BitSet defVal) throws IOException;


    // BinarySavable

    public void write(Savable object, String name, Savable defVal) throws IOException;
    public void write(Savable[] objects, String name, Savable[] defVal) throws IOException;
    public void write(Savable[][] objects, String name, Savable[][] defVal) throws IOException;


    // ArrayLists

    public void writeSavableArrayList(ArrayList array, String name, ArrayList defVal) throws IOException;
    public void writeSavableArrayListArray(ArrayList[] array, String name, ArrayList[] defVal) throws IOException;
    public void writeSavableArrayListArray2D(ArrayList[][] array, String name, ArrayList[][] defVal) throws IOException;

    public void writeFloatBufferArrayList(ArrayList<FloatBuffer> array, String name, ArrayList<FloatBuffer> defVal) throws IOException;
    public void writeByteBufferArrayList(ArrayList<ByteBuffer> array, String name, ArrayList<ByteBuffer> defVal) throws IOException;


    // Maps

    public void writeSavableMap(Map<? extends Savable, ? extends Savable> map, String name, Map<? extends Savable, ? extends Savable> defVal) throws IOException;
    public void writeStringSavableMap(Map<String, ? extends Savable> map, String name, Map<String, ? extends Savable> defVal) throws IOException;
    public void writeIntSavableMap(IntMap<? extends Savable> map, String name, IntMap<? extends Savable> defVal) throws IOException;

    // NIO BUFFERS
    // float buffer

    public void write(FloatBuffer value, String name, FloatBuffer defVal) throws IOException;


    // int buffer

    public void write(IntBuffer value, String name, IntBuffer defVal) throws IOException;


    // byte buffer

    public void write(ByteBuffer value, String name, ByteBuffer defVal) throws IOException;


    // short buffer

    public void write(ShortBuffer value, String name, ShortBuffer defVal) throws IOException;


    // enums

    public void write(Enum value, String name, Enum defVal) throws IOException;
}