/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
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
 *
 * @author codex
 */
public class SavableObject implements Savable {

    private Object object;
    private final String name = "object";
    
    public SavableObject() {}
    public SavableObject(Object object) {
        this.object = object;
    }
    
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        if (object instanceof Savable) {
            out.write((Savable)object, name, (Savable)object);
            out.write("Savable", "type", "");
        } else if (object instanceof Integer) {
            out.write((int)object, name, (int)object);
            out.write("Integer", "type", "");
        } else if (object instanceof Float) {
            out.write((float)object, name, (float)object);
            out.write("Float", "type", "");
        } else if (object instanceof Double) {
            out.write((double)object, name, (double)object);
            out.write("Double", "type", "");
        } else if (object instanceof Boolean) {
            out.write((boolean)object, name, (boolean)object);
            out.write("Boolean", "type", "");
        } else if (object instanceof Byte) {
            out.write((byte)object, name, (byte)object);
            out.write("Byte", "type", "");
        } else if (object instanceof String) {
            out.write((String)object, name, (String)object);
            out.write("String", "type", "");
        } else if (object instanceof Long) {
            out.write((Long)object, name, (Long)object);
            out.write("Long", "type", "");
        } else if (object instanceof Short) {
            out.write((short)object, name, (short)object);
            out.write("Short", "type", "");
        } else if (object instanceof BitSet) {
            out.write((BitSet)object, name, (BitSet)object);
            out.write("BitSet", "type", "");
        } else if (object instanceof FloatBuffer) {
            out.write((FloatBuffer)object, name, (FloatBuffer)object);
            out.write("FloatBuffer", "type", "");
        } else if (object instanceof IntBuffer) {
            out.write((IntBuffer)object, name, (IntBuffer)object);
            out.write("IntBuffer", "type", "");
        } else if (object instanceof ByteBuffer) {
            out.write((ByteBuffer)object, name, (ByteBuffer)object);
            out.write("ByteBuffer", "type", "");
        } else if (object instanceof ShortBuffer) {
            out.write((ShortBuffer)object, name, (ShortBuffer)object);
            out.write("ShortBuffer", "type", "");
        } else if (object instanceof ArrayList) {
            out.writeSavableArrayList((ArrayList)object, name, (ArrayList)object);
            out.write("ArrayList", "type", "");
        } else if (object instanceof Map) {
            out.writeStringSavableMap((Map<String, ? extends Savable>)object, name, (Map<String, ? extends Savable>)object);
            out.write("StringMap", "type", "");
        } else if (object instanceof IntMap) {
            out.writeIntSavableMap((IntMap)object, name, (IntMap)object);
            out.write("IntMap", "type", "");
        }
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        String type = in.readString("type", "");
        switch (type) {
            case "Savable":
                object = in.readSavable(name, null);
                break;
            case "Integer":
                object = in.readInt(name, 0);
                break;
            case "Float":
                object = in.readFloat(name, 0);
                break;
            case "Double":
                object = in.readDouble(name, 0);
                break;
            case "Boolean":
                object = in.readBoolean(name, false);
                break;
            case "Byte":
                object = in.readByte(name, (byte)0);
                break;
            case "String":
                object = in.readString(name, null);
                break;
            case "Long":
                object = in.readLong(name, 0);
                break;
            case "Short":
                object = in.readShort(name, (short)0);
                break;
            case "BitSet":
                object = in.readBitSet(name, null);
                break;
            case "FloatBuffer":
                object = in.readFloatBuffer(name, null);
                break;
            case "IntBuffer":
                object = in.readIntBuffer(name, null);
                break;
            case "ByteBuffer":
                object = in.readByteBuffer(name, null);
                break;
            case "ShortBuffer":
                object = in.readShortBuffer(name, null);
                break;
            case "ArrayList":
                object = in.readSavableArrayList(name, null);
                break;
            case "StringMap":
                object = in.readStringSavableMap(name, null);
                break;
            case "IntMap":
                object = in.readIntSavableMap(name, null);
                break;
        }
    }
    
    public void setObject(Object object) {
        this.object = object;
    }
    
    public Object getObject() {
        return object;
    }
    
}
