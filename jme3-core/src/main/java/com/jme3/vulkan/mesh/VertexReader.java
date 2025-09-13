package com.jme3.vulkan.mesh;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;

import java.nio.*;

public interface VertexReader {

    int capacity();

    int limit();

    int components();

    byte getByte(int vertex, int component);

    short getShort(int vertex, int component);

    int getInt(int vertex, int component);

    float getFloat(int vertex, int component);

    double getDouble(int vertex, int component);

    long getLong(int vertex, int component);

    default Vector2f getVector2(int vertex, int baseComponent, Vector2f store) {
        if (store == null) {
            store = new Vector2f();
        }
        return store.set(
                getFloat(vertex, baseComponent++),
                getFloat(vertex, baseComponent  ));
    }

    default Vector3f getVector3(int vertex, int baseComponent, Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        return store.set(
                getFloat(vertex, baseComponent++),
                getFloat(vertex, baseComponent++),
                getFloat(vertex, baseComponent  ));
    }

    default Vector4f getVector4(int vertex, int baseComponent, Vector4f store) {
        if (store == null) {
            store = new Vector4f();
        }
        return store.set(
                getFloat(vertex, baseComponent++),
                getFloat(vertex, baseComponent++),
                getFloat(vertex, baseComponent++),
                getFloat(vertex, baseComponent  ));
    }

    default ColorRGBA getColor(int vertex, int baseComponent, ColorRGBA store) {
        if (store == null) {
            store = new ColorRGBA();
        }
        return store.set(
                getFloat(vertex, baseComponent++),
                getFloat(vertex, baseComponent++),
                getFloat(vertex, baseComponent++),
                getFloat(vertex, baseComponent  ));
    }

    default byte[] getBytes(int baseVertex, int baseComponent, byte[] store) {
        for (int i = 0; i < store.length; i++) {
            store[i] = getByte(baseVertex, baseComponent);
            if (++baseComponent >= components()) {
                baseComponent = 0;
                baseVertex++;
            }
        }
        return store;
    }

    default ByteBuffer getBytes(int baseVertex, int baseComponent, ByteBuffer store) {
        for (int i = 0; i < store.limit(); i++) {
            store.put(i, getByte(baseVertex, baseComponent));
            if (++baseComponent >= components()) {
                baseComponent = 0;
                baseVertex++;
            }
        }
        return store;
    }

    default short[] getShorts(int baseVertex, int baseComponent, short[] store) {
        for (int i = 0; i < store.length; i++) {
            store[i] = getShort(baseVertex, baseComponent);
            if (++baseComponent >= components()) {
                baseComponent = 0;
                baseVertex++;
            }
        }
        return store;
    }

    default ShortBuffer getShorts(int baseVertex, int baseComponent, ShortBuffer store) {
        for (int i = 0; i < store.limit(); i++) {
            store.put(i, getShort(baseVertex, baseComponent));
            if (++baseComponent >= components()) {
                baseComponent = 0;
                baseVertex++;
            }
        }
        return store;
    }

    default int[] getInts(int baseVertex, int baseComponent, int[] store) {
        for (int i = 0; i < store.length; i++) {
            store[i] = getInt(baseVertex, baseComponent);
            if (++baseComponent >= components()) {
                baseComponent = 0;
                baseVertex++;
            }
        }
        return store;
    }

    default IntBuffer getInts(int baseVertex, int baseComponent, IntBuffer store) {
        for (int i = 0; i < store.limit(); i++) {
            store.put(i, getInt(baseVertex, baseComponent));
            if (++baseComponent >= components()) {
                baseComponent = 0;
                baseVertex++;
            }
        }
        return store;
    }

    default float[] getFloats(int baseVertex, int baseComponent, float[] store) {
        for (int i = 0; i < store.length; i++) {
            store[i] = getFloat(baseVertex, baseComponent);
            if (++baseComponent >= components()) {
                baseComponent = 0;
                baseVertex++;
            }
        }
        return store;
    }

    default FloatBuffer getFloats(int baseVertex, int baseComponent, FloatBuffer store) {
        for (int i = 0; i < store.limit(); i++) {
            store.put(i, getFloat(baseVertex, baseComponent));
            if (++baseComponent >= components()) {
                baseComponent = 0;
                baseVertex++;
            }
        }
        return store;
    }

    default double[] getDoubles(int baseVertex, int baseComponent, double[] store) {
        for (int i = 0; i < store.length; i++) {
            store[i] = getDouble(baseVertex, baseComponent);
            if (++baseComponent >= components()) {
                baseComponent = 0;
                baseVertex++;
            }
        }
        return store;
    }

    default DoubleBuffer getDoubles(int baseVertex, int baseComponent, DoubleBuffer store) {
        for (int i = 0; i < store.limit(); i++) {
            store.put(i, getDouble(baseVertex, baseComponent));
            if (++baseComponent >= components()) {
                baseComponent = 0;
                baseVertex++;
            }
        }
        return store;
    }

    default long[] getLongs(int baseVertex, int baseComponent, long[] store) {
        for (int i = 0; i < store.length; i++) {
            store[i] = getLong(baseVertex, baseComponent);
            if (++baseComponent >= components()) {
                baseComponent = 0;
                baseVertex++;
            }
        }
        return store;
    }

    default LongBuffer getLongs(int baseVertex, int baseComponent, LongBuffer store) {
        for (int i = 0; i < store.limit(); i++) {
            store.put(i, getLong(baseVertex, baseComponent));
            if (++baseComponent >= components()) {
                baseComponent = 0;
                baseVertex++;
            }
        }
        return store;
    }

}
