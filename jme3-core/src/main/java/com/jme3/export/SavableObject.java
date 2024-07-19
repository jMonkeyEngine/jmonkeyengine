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
 * Saves the internal object if it is savable.
 * 
 * @author codex
 */
public class SavableObject implements Savable {

    public static final SavableObject NULL = new SavableObject();
    private static final String OBJECT = "object";
    private static final String TYPE = "type";
    private static final String NULL_TYPE = "Null";
    
    private Object object;
    
    public SavableObject() {}
    public SavableObject(Object object) {
        this.object = object;
    }
    
    @Override
    public void write(JmeExporter ex) throws IOException {
        if (object == null) return;
        OutputCapsule out = ex.getCapsule(this);
        if (object instanceof Savable) {
            out.write((Savable)object, OBJECT, null);
            out.write("Savable", TYPE, NULL_TYPE);
        } else if (object instanceof Integer) {
            out.write((int)object, OBJECT, 0);
            out.write("Integer", TYPE, NULL_TYPE);
        } else if (object instanceof Float) {
            out.write((float)object, OBJECT, 0);
            out.write("Float", TYPE, NULL_TYPE);
        } else if (object instanceof Double) {
            out.write((double)object, OBJECT, 0);
            out.write("Double", TYPE, NULL_TYPE);
        } else if (object instanceof Boolean) {
            out.write((boolean)object, OBJECT, false);
            out.write("Boolean", TYPE, NULL_TYPE);
        } else if (object instanceof Byte) {
            out.write((byte)object, OBJECT, (byte)0);
            out.write("Byte", TYPE, NULL_TYPE);
        } else if (object instanceof String) {
            out.write((String)object, OBJECT, null);
            out.write("String", TYPE, NULL_TYPE);
        } else if (object instanceof Long) {
            out.write((Long)object, OBJECT, 0);
            out.write("Long", TYPE, NULL_TYPE);
        } else if (object instanceof Short) {
            out.write((short)object, OBJECT, (short)0);
            out.write("Short", TYPE, NULL_TYPE);
        } else if (object instanceof BitSet) {
            out.write((BitSet)object, OBJECT, null);
            out.write("BitSet", TYPE, NULL_TYPE);
        } else if (object instanceof FloatBuffer) {
            out.write((FloatBuffer)object, OBJECT, null);
            out.write("FloatBuffer", TYPE, NULL_TYPE);
        } else if (object instanceof IntBuffer) {
            out.write((IntBuffer)object, OBJECT, null);
            out.write("IntBuffer", TYPE, NULL_TYPE);
        } else if (object instanceof ByteBuffer) {
            out.write((ByteBuffer)object, OBJECT, null);
            out.write("ByteBuffer", TYPE, NULL_TYPE);
        } else if (object instanceof ShortBuffer) {
            out.write((ShortBuffer)object, OBJECT, null);
            out.write("ShortBuffer", TYPE, NULL_TYPE);
        } else if (object instanceof ArrayList) {
            out.writeSavableArrayList((ArrayList)object, OBJECT, null);
            out.write("ArrayList", TYPE, NULL_TYPE);
        } else if (object instanceof Map) {
            out.writeStringSavableMap((Map<String, ? extends Savable>)object, OBJECT, null);
            out.write("StringMap", TYPE, NULL_TYPE);
        } else if (object instanceof IntMap) {
            out.writeIntSavableMap((IntMap)object, OBJECT, null);
            out.write("IntMap", TYPE, NULL_TYPE);
        }
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        String type = in.readString(TYPE, NULL_TYPE);
        switch (type) {
            case "Savable":
                object = in.readSavable(OBJECT, null);
                break;
            case "Integer":
                object = in.readInt(OBJECT, 0);
                break;
            case "Float":
                object = in.readFloat(OBJECT, 0);
                break;
            case "Double":
                object = in.readDouble(OBJECT, 0);
                break;
            case "Boolean":
                object = in.readBoolean(OBJECT, false);
                break;
            case "Byte":
                object = in.readByte(OBJECT, (byte)0);
                break;
            case "String":
                object = in.readString(OBJECT, null);
                break;
            case "Long":
                object = in.readLong(OBJECT, 0);
                break;
            case "Short":
                object = in.readShort(OBJECT, (short)0);
                break;
            case "BitSet":
                object = in.readBitSet(OBJECT, null);
                break;
            case "FloatBuffer":
                object = in.readFloatBuffer(OBJECT, null);
                break;
            case "IntBuffer":
                object = in.readIntBuffer(OBJECT, null);
                break;
            case "ByteBuffer":
                object = in.readByteBuffer(OBJECT, null);
                break;
            case "ShortBuffer":
                object = in.readShortBuffer(OBJECT, null);
                break;
            case "ArrayList":
                object = in.readSavableArrayList(OBJECT, null);
                break;
            case "StringMap":
                object = in.readStringSavableMap(OBJECT, null);
                break;
            case "IntMap":
                object = in.readIntSavableMap(OBJECT, null);
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
