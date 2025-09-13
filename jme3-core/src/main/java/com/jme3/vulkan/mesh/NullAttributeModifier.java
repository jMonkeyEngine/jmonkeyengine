package com.jme3.vulkan.mesh;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;

import java.nio.*;

public class NullAttributeModifier extends AttributeModifier {

    public NullAttributeModifier() {
        super(null, null);
    }

    @Override
    protected AttributeModifier map() {
        return this;
    }

    @Override
    public void close() {}

    @Override
    public int capacity() {
        return 0;
    }

    @Override
    public int limit() {
        return 0;
    }

    @Override
    public AttributeModifier limit(int vertex) {
        return this;
    }

    @Override
    public AttributeModifier putByte(int vertex, int component, byte value) {
        return this;
    }

    @Override
    public AttributeModifier putShort(int vertex, int component, short value) {
        return this;
    }

    @Override
    public AttributeModifier putInt(int vertex, int component, int value) {
        return this;
    }

    @Override
    public AttributeModifier putFloat(int vertex, int component, float value) {
        return this;
    }

    @Override
    public AttributeModifier putDouble(int vertex, int component, double value) {
        return this;
    }

    @Override
    public AttributeModifier putLong(int vertex, int component, long value) {
        return this;
    }

    @Override
    public AttributeModifier putVector2(int vertex, int baseComponent, Vector2f value) {
        return this;
    }

    @Override
    public AttributeModifier putVector2(int vertex, int baseComponent, float x, float y) {
        return this;
    }

    @Override
    public AttributeModifier putVector3(int vertex, int baseComponent, Vector3f value) {
        return this;
    }

    @Override
    public AttributeModifier putVector3(int vertex, int baseComponent, float x, float y, float z) {
        return this;
    }

    @Override
    public AttributeModifier putVector4(int vertex, int baseComponent, Vector4f value) {
        return putVector4(vertex, baseComponent, value.x, value.y, value.z, value.w);
    }

    @Override
    public AttributeModifier putVector4(int vertex, int baseComponent, float x, float y, float z, float w) {
        return this;
    }

    @Override
    public AttributeModifier putColor(int vertex, int baseComponent, ColorRGBA value) {
        return this;
    }

    @Override
    public AttributeModifier putBytes(int baseVertex, int baseComponent, byte[] values) {
        return this;
    }

    @Override
    public AttributeModifier putBytes(int baseVertex, int baseComponent, ByteBuffer values) {
        return this;
    }

    @Override
    public AttributeModifier putShorts(int baseVertex, int baseComponent, short[] values) {
        return this;
    }

    @Override
    public AttributeModifier putShorts(int baseVertex, int baseComponent, ShortBuffer values) {
        return this;
    }

    @Override
    public AttributeModifier putInts(int baseVertex, int baseComponent, int[] values) {
        return this;
    }

    @Override
    public AttributeModifier putInts(int baseVertex, int baseComponent, IntBuffer values) {
        return this;
    }

    @Override
    public AttributeModifier putFloats(int baseVertex, int baseComponent, float[] values) {
        return this;
    }

    @Override
    public AttributeModifier putFloats(int baseVertex, int baseComponent, FloatBuffer values) {
        return this;
    }

    @Override
    public AttributeModifier putDoubles(int baseVertex, int baseComponent, double[] values) {
        return this;
    }

    @Override
    public AttributeModifier putDoubles(int baseVertex, int baseComponent, DoubleBuffer values) {
        return this;
    }

    @Override
    public AttributeModifier putLongs(int baseVertex, int baseComponent, long[] values) {
        return this;
    }

    @Override
    public AttributeModifier putLongs(int baseVertex, int baseComponent, LongBuffer values) {
        return this;
    }

    @Override
    public byte getByte(int vertex, int component) {
        return 0;
    }

    @Override
    public short getShort(int vertex, int component) {
        return 0;
    }

    @Override
    public int getInt(int vertex, int component) {
        return 0;
    }

    @Override
    public float getFloat(int vertex, int component) {
        return 0f;
    }

    @Override
    public double getDouble(int vertex, int component) {
        return 0.0;
    }

    @Override
    public long getLong(int vertex, int component) {
        return 0L;
    }

    @Override
    public Vector2f getVector2(int vertex, int baseComponent, Vector2f store) {
        if (store == null) {
            store = new Vector2f();
        }
        return store.set(0f, 0f);
    }

    @Override
    public Vector3f getVector3(int vertex, int baseComponent, Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        return store.set(0f, 0f, 0f);
    }

    @Override
    public Vector4f getVector4(int vertex, int baseComponent, Vector4f store) {
        if (store == null) {
            store = new Vector4f();
        }
        return store.set(0f, 0f, 0f, 0f);
    }

    @Override
    public ColorRGBA getColor(int vertex, int baseComponent, ColorRGBA store) {
        if (store == null) {
            store = new ColorRGBA();
        }
        return store.set(0f, 0f, 0f, 0f);
    }

}
