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
public interface InputCapsule {

    public int getSavableVersion(Class<? extends Savable> clazz);
    
    // byte primitive

    public byte readByte(String name, byte defVal) throws IOException;
    public byte[] readByteArray(String name, byte[] defVal) throws IOException;
    public byte[][] readByteArray2D(String name, byte[][] defVal) throws IOException;

    // int primitive

    public int readInt(String name, int defVal) throws IOException;
    public int[] readIntArray(String name, int[] defVal) throws IOException;
    public int[][] readIntArray2D(String name, int[][] defVal) throws IOException;


    // float primitive

    public float readFloat(String name, float defVal) throws IOException;
    public float[] readFloatArray(String name, float[] defVal) throws IOException;
    public float[][] readFloatArray2D(String name, float[][] defVal) throws IOException;


    // double primitive

    public double readDouble(String name, double defVal) throws IOException;
    public double[] readDoubleArray(String name, double[] defVal) throws IOException;
    public double[][] readDoubleArray2D(String name, double[][] defVal) throws IOException;


    // long primitive

    public long readLong(String name, long defVal) throws IOException;
    public long[] readLongArray(String name, long[] defVal) throws IOException;
    public long[][] readLongArray2D(String name, long[][] defVal) throws IOException;


    // short primitive

    public short readShort(String name, short defVal) throws IOException;
    public short[] readShortArray(String name, short[] defVal) throws IOException;
    public short[][] readShortArray2D(String name, short[][] defVal) throws IOException;


    // boolean primitive

    public boolean readBoolean(String name, boolean defVal) throws IOException;
    public boolean[] readBooleanArray(String name, boolean[] defVal) throws IOException;
    public boolean[][] readBooleanArray2D(String name, boolean[][] defVal) throws IOException;


    // String

    public String readString(String name, String defVal) throws IOException;
    public String[] readStringArray(String name, String[] defVal) throws IOException;
    public String[][] readStringArray2D(String name, String[][] defVal) throws IOException;


    // BitSet

    public BitSet readBitSet(String name, BitSet defVal) throws IOException;


    // BinarySavable

    public Savable readSavable(String name, Savable defVal) throws IOException;
    public Savable[] readSavableArray(String name, Savable[] defVal) throws IOException;
    public Savable[][] readSavableArray2D(String name, Savable[][] defVal) throws IOException;


    // ArrayLists

    public ArrayList readSavableArrayList(String name, ArrayList defVal) throws IOException;
    public ArrayList[] readSavableArrayListArray(String name, ArrayList[] defVal) throws IOException;
    public ArrayList[][] readSavableArrayListArray2D(String name, ArrayList[][] defVal) throws IOException;

    public ArrayList<FloatBuffer> readFloatBufferArrayList(String name, ArrayList<FloatBuffer> defVal) throws IOException;
    public ArrayList<ByteBuffer> readByteBufferArrayList(String name, ArrayList<ByteBuffer> defVal) throws IOException;


    // Maps

    public Map<? extends Savable, ? extends Savable> readSavableMap(String name, Map<? extends Savable, ? extends Savable> defVal) throws IOException;
    public Map<String, ? extends Savable> readStringSavableMap(String name, Map<String, ? extends Savable> defVal) throws IOException;
    public IntMap<? extends Savable> readIntSavableMap(String name, IntMap<? extends Savable> defVal) throws IOException;

    // NIO BUFFERS
    // float buffer

    public FloatBuffer readFloatBuffer(String name, FloatBuffer defVal) throws IOException;


    // int buffer

    public IntBuffer readIntBuffer(String name, IntBuffer defVal) throws IOException;


    // byte buffer

    public ByteBuffer readByteBuffer(String name, ByteBuffer defVal) throws IOException;


    // short buffer

    public ShortBuffer readShortBuffer(String name, ShortBuffer defVal) throws IOException;


    // enums

    public <T extends Enum<T>> T readEnum(String name, Class<T> enumType, T defVal) throws IOException;

}