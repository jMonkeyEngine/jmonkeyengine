/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.material.plugin.export.material;

import com.jme3.asset.TextureKey;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.material.MatParam;
import com.jme3.material.MatParamTexture;
import com.jme3.math.*;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.IntMap;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author tsr
 */
public class J3MOutputCapsule implements OutputCapsule {

    private final HashMap<String, String> parameters;
    protected final J3MExporter exporter;

    public J3MOutputCapsule(J3MExporter exporter) {
        this.exporter = exporter;
        parameters = new HashMap<>();
    }

    public void writeToStream(OutputStreamWriter out) throws IOException {
        for (String key : parameters.keySet()) {
            out.write("      ");
            writeParameter(out, key, parameters.get(key));
            out.write("\n");
        }
    }

    protected void writeParameter(OutputStreamWriter out, String name, String value) throws IOException {
        out.write(name);
        out.write(" : ");
        out.write(value);
    }

    public void clear() {
        parameters.clear();
    }

    protected void putParameter(String name, String value) {
        parameters.put(name, value);
    }

    @Override
    public void write(boolean value, String name, boolean defVal) throws IOException {
        if (value == defVal) {
            return;
        }

        putParameter(name, ((value) ? "On" : "Off"));
    }

    @Override
    public void writeStringSavableMap(Map<String, ? extends Savable> map, String name, Map<String, ? extends Savable> defVal) throws IOException {
        for (String key : map.keySet()) {
            Savable value = map.get(key);
            if (defVal == null || !defVal.containsKey(key) || !defVal.get(key).equals(value)) {
                putParameter(key, format(value));
            }
        }
    }

    protected String format(Savable value) {
        if (value instanceof MatParamTexture) {
            return formatMatParamTexture((MatParamTexture) value);
        }
        if (value instanceof MatParam) {
            return formatMatParam((MatParam) value);
        }

        throw new UnsupportedOperationException(value.getClass() + ": Not supported yet.");
    }

    private String formatMatParam(MatParam param){
        VarType type = param.getVarType();
        Object val = param.getValue();
        switch (type) {
            case Boolean:
            case Float:
            case Int:
                return val.toString();
            case Vector2:
                Vector2f v2 = (Vector2f) val;
                return v2.getX() + " " + v2.getY();
            case Vector3:
                Vector3f v3 = (Vector3f) val;
                return v3.getX() + " " + v3.getY() + " " + v3.getZ();
            case Vector4:
                // can be either ColorRGBA, Vector4f or Quaternion
                if (val instanceof Vector4f) {
                    Vector4f v4 = (Vector4f) val;
                    return v4.getX() + " " + v4.getY() + " "
                            + v4.getZ() + " " + v4.getW();
                } else if (val instanceof ColorRGBA) {
                    ColorRGBA color = (ColorRGBA) val;
                    return color.getRed() + " " + color.getGreen() + " "
                            + color.getBlue() + " " + color.getAlpha();
                } else if (val instanceof Quaternion) {
                    Quaternion quat = (Quaternion) val;
                    return quat.getX() + " " + quat.getY() + " "
                            + quat.getZ() + " " + quat.getW();
                } else {
                    throw new UnsupportedOperationException("Unexpected Vector4 type: " + val);
                }

            default:
                return null; // parameter type not supported in J3M
        }
    }

    protected static String formatMatParamTexture(MatParamTexture param) {
        StringBuilder ret = new StringBuilder();
        Texture tex = (Texture) param.getValue();
        TextureKey key;
        if (tex != null) {
            key = (TextureKey) tex.getKey();

            if (key != null && key.isFlipY()) {
                ret.append("Flip ");
            }

            ret.append(formatWrapMode(tex, Texture.WrapAxis.S));
            ret.append(formatWrapMode(tex, Texture.WrapAxis.T));
            ret.append(formatWrapMode(tex, Texture.WrapAxis.R));

            //Min and Mag filter
            Texture.MinFilter def = Texture.MinFilter.BilinearNoMipMaps;
            if (tex.getImage().hasMipmaps() || (key != null && key.isGenerateMips())) {
                def = Texture.MinFilter.Trilinear;
            }
            if (tex.getMinFilter() != def) {
                ret.append("Min").append(tex.getMinFilter().name()).append(" ");
            }

            if (tex.getMagFilter() != Texture.MagFilter.Bilinear) {
                ret.append("Mag").append(tex.getMagFilter().name()).append(" ");
            }

            ret.append("\"").append(key.getName()).append("\"");
        }

        return ret.toString();
    }

    protected static String formatWrapMode(Texture texVal, Texture.WrapAxis axis) {
        WrapMode mode;
        try {
            mode = texVal.getWrap(axis);
        } catch (IllegalArgumentException e) {
            //this axis doesn't exist on the texture
            return "";
        }
        if (mode != WrapMode.EdgeClamp) {
            return "Wrap" + mode.name() + "_" + axis.name() + " ";
        }
        return "";
    }

    @Override
    public void write(Enum value, String name, Enum defVal) throws IOException {
        if (value == defVal) {
            return;
        }

        putParameter(name, value.toString());
    }

    @Override
    public void write(float value, String name, float defVal) throws IOException {
        if (value == defVal) {
            return;
        }

        putParameter(name, Float.toString(value));
    }

    @Override
    public void write(float[] value, String name, float[] defVal) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        if (value == defVal) {
            return;
        }

        putParameter(name, Integer.toString(value));
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
