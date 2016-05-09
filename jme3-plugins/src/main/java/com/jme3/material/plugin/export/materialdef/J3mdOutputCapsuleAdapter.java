/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.material.plugin.export.materialdef;

import com.jme3.asset.TextureKey;
import com.jme3.export.*;
import com.jme3.material.*;
import com.jme3.math.*;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.IntMap;

import java.io.*;
import java.nio.*;
import java.util.*;

/**
 *
 * @author tsr
 */
public abstract class J3mdOutputCapsuleAdapter implements OutputCapsule {

    protected final J3mdExporter exporter;

    public J3mdOutputCapsuleAdapter(J3mdExporter exporter) {
        this.exporter = exporter;
    }

    public void writeToStream(OutputStreamWriter out) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    protected void writeParameter(OutputStreamWriter out, String name, String value) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void clear() {

    }

    protected void putParameter(String name, String value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void write(boolean value, String name, boolean defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void writeStringSavableMap(Map<String, ? extends Savable> map, String name, Map<String, ? extends Savable> defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    @Override
    public void write(Enum value, String name, Enum defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void write(float value, String name, float defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void write(float[] value, String name, float[] defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void write(float[][] value, String name, float[][] defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(double value, String name, double defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(double[] value, String name, double[] defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(double[][] value, String name, double[][] defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(long value, String name, long defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(long[] value, String name, long[] defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(long[][] value, String name, long[][] defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(short value, String name, short defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(short[] value, String name, short[] defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(short[][] value, String name, short[][] defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(boolean[] value, String name, boolean[] defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(boolean[][] value, String name, boolean[][] defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(String value, String name, String defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(String[] value, String name, String[] defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(String[][] value, String name, String[][] defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(BitSet value, String name, BitSet defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(Savable object, String name, Savable defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(Savable[] objects, String name, Savable[] defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(Savable[][] objects, String name, Savable[][] defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeSavableArrayList(ArrayList array, String name, ArrayList defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeSavableArrayListArray(ArrayList[] array, String name, ArrayList[] defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeSavableArrayListArray2D(ArrayList[][] array, String name, ArrayList[][] defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeFloatBufferArrayList(ArrayList<FloatBuffer> array, String name, ArrayList<FloatBuffer> defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeByteBufferArrayList(ArrayList<ByteBuffer> array, String name, ArrayList<ByteBuffer> defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeSavableMap(Map<? extends Savable, ? extends Savable> map, String name, Map<? extends Savable, ? extends Savable> defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeIntSavableMap(IntMap<? extends Savable> map, String name, IntMap<? extends Savable> defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(FloatBuffer value, String name, FloatBuffer defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(IntBuffer value, String name, IntBuffer defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(ByteBuffer value, String name, ByteBuffer defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(ShortBuffer value, String name, ShortBuffer defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(byte value, String name, byte defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(byte[] value, String name, byte[] defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(byte[][] value, String name, byte[][] defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(int value, String name, int defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(int[] value, String name, int[] defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(int[][] value, String name, int[][] defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
